package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

object ApiClient {
    /** Where the session tokens live; set by [SessionManager.get]. */
    interface TokenStore {
        val accessToken: String?
        val refreshToken: String?
        /** The signed-in user; queued writes are tagged with it (MOB-01). */
        val sessionUserId: String?
        fun saveTokens(access: String, refresh: String)
        fun clear()
    }

    /** Records the server sent that could not be read and were skipped during sync (MOB-14). */
    @Volatile
    var skippedRecords: Int = 0

    @Volatile
    var tokenStore: TokenStore? = null

    /** Called when the session can no longer be refreshed (expired, revoked, user disabled). */
    @Volatile
    var onUnauthorized: (() -> Unit)? = null

    val moshi: Moshi = Moshi.Builder()
        .add(LenientListAdapterFactory)
        .add(NullStrippingAdapterFactory)
        .add(KotlinJsonAdapterFactory())
        .build()

    val baseUrl: String = BuildConfig.API_BASE_URL.let { if (it.isEmpty() || it.endsWith("/")) it else "$it/" }
    val isConfigured: Boolean get() = baseUrl.isNotEmpty()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val authInterceptor = Interceptor { chain ->
        val token = tokenStore?.accessToken
        val request = if (token.isNullOrBlank()) chain.request()
        else chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        chain.proceed(request)
    }

    /** Lets the server tell an outdated app to update before it sends writes it would refuse. */
    private val versionInterceptor = Interceptor { chain ->
        chain.proceed(chain.request().newBuilder().header("X-App-Version-Code", BuildConfig.VERSION_CODE.toString()).build())
    }

    /** Calls that must never trigger a token refresh: they are themselves the session calls. */
    private val NO_REFRESH_PATHS = setOf("/api/auth/login", "/api/auth/refresh", "/api/auth/logout", "/api/auth/register")

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
    }

    private val plainClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(versionInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    private data class RefreshResponse(val token: String = "", val refreshToken: String = "")

    /**
     * On 401 the access token is refreshed once with the refresh token (rotated by the server)
     * and the request retried. Only the session calls themselves are excluded; /api/auth/me is
     * refreshed like any other request, so role and scope changes are picked up (MOB-05).
     */
    private val refreshAuthenticator = Authenticator { _, response ->
        val request = response.request
        val store = tokenStore ?: return@Authenticator null
        val path = request.url.encodedPath
        if (NO_REFRESH_PATHS.any { path.endsWith(it) } || path.contains("/api/auth/join-status/") || request.header(RETRIED) != null) {
            return@Authenticator null
        }
        synchronized(this) {
            val sent = request.header("Authorization")?.removePrefix("Bearer ")
            val current = store.accessToken
            // Another request refreshed while this one was in flight.
            if (current != null && current != sent) {
                return@Authenticator request.newBuilder().header("Authorization", "Bearer $current").header(RETRIED, "1").build()
            }
            val refresh = store.refreshToken ?: return@Authenticator null
            val tokens = try {
                val body = """{"refreshToken":${moshi.adapter(String::class.java).toJson(refresh)}}""".toRequestBody(jsonMediaType)
                plainClient.newCall(Request.Builder().url("${baseUrl}api/auth/refresh").post(body).build()).execute().use {
                    if (!it.isSuccessful) null else moshi.adapter(RefreshResponse::class.java).fromJson(it.body?.string().orEmpty())
                }
            } catch (e: Exception) {
                null
            }
            if (tokens == null || tokens.token.isBlank()) return@Authenticator null
            store.saveTokens(tokens.token, tokens.refreshToken)
            request.newBuilder().header("Authorization", "Bearer ${tokens.token}").header(RETRIED, "1").build()
        }
    }
    private const val RETRIED = "X-Auth-Retried"

    private val okHttpClient = plainClient.newBuilder()
        .addInterceptor(authInterceptor)
        .authenticator(refreshAuthenticator)
        .build()

    /** Outcome of a raw request: HTTP status (null = server unreachable), the server's message and error code. */
    data class SendResult(val code: Int?, val message: String?, val body: String? = null, val errorCode: String? = null)

    private data class ErrorBody(val message: String? = null, val code: String? = null)

    private fun serverCode(errorBody: String?): String? = try {
        errorBody?.let { moshi.adapter(ErrorBody::class.java).fromJson(it)?.code }
    } catch (e: Exception) {
        null
    }

    /**
     * Sends a JSON request to [path] (e.g. "api/committees"). With [idempotencyKey], a repeat of the
     * same write is answered by the server without applying it again. Blocking; call off the main thread.
     */
    fun send(method: String, path: String, bodyJson: String?, idempotencyKey: String? = null): SendResult = try {
        val body = (bodyJson ?: if (method == "DELETE" || method == "GET") null else "{}")?.toRequestBody(jsonMediaType)
        val request = Request.Builder().url(baseUrl + path).method(method, body)
            .apply { if (idempotencyKey != null) header("Idempotency-Key", idempotencyKey) }
            .build()
        okHttpClient.newCall(request).execute().use { res ->
            val text = res.body?.string()
            if (res.isSuccessful) SendResult(res.code, null, text)
            else SendResult(res.code, serverMessage(text), null, serverCode(text))
        }
    } catch (e: Exception) {
        SendResult(null, null)
    }

    /** Uploads raw bytes (an exam PDF) with the session. Blocking; call off the main thread. */
    fun sendBytes(method: String, path: String, bytes: ByteArray, contentType: String, headers: Map<String, String> = emptyMap()): SendResult = try {
        val request = Request.Builder().url(baseUrl + path).method(method, bytes.toRequestBody(contentType.toMediaType()))
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .build()
        okHttpClient.newCall(request).execute().use { res ->
            val text = res.body?.string()
            SendResult(res.code, if (res.isSuccessful) null else serverMessage(text), if (res.isSuccessful) text else null)
        }
    } catch (e: Exception) {
        SendResult(null, null)
    }

    /** Downloads a file; a failure carries the server's message (or the HTTP status) to show the user. */
    fun download(path: String): Result<ByteArray> = try {
        okHttpClient.newCall(Request.Builder().url(baseUrl + path).build()).execute().use { res ->
            val body = res.body?.bytes()
            when {
                res.isSuccessful && body != null -> Result.success(body)
                else -> Result.failure(IllegalStateException(serverMessage(body?.toString(Charsets.UTF_8)) ?: "HTTP ${res.code}"))
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun serverMessage(errorBody: String?): String? = try {
        errorBody?.let { moshi.adapter(ErrorBody::class.java).fromJson(it)?.message }
    } catch (e: Exception) {
        null
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl.ifEmpty { "https://server-not-configured.invalid/" })
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}

/**
 * The server returns SQL NULLs as JSON nulls. Kotlin's Moshi adapter rejects a null
 * for a non-null property even when it has a default, so for our own classes we drop
 * null keys first and let the constructor defaults apply.
 */
private object NullStrippingAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        val raw = Types.getRawType(type)
        if (!raw.name.startsWith("com.example.") || raw.isEnum) return null
        val delegate = moshi.nextAdapter<Any>(this, type, annotations)
        return object : JsonAdapter<Any>() {
            override fun fromJson(reader: JsonReader): Any? {
                val value = reader.readJsonValue() ?: return null
                return delegate.fromJsonValue(stripNulls(value))
            }

            override fun toJson(writer: JsonWriter, value: Any?) = delegate.toJson(writer, value)
        }
    }

    private fun stripNulls(value: Any?): Any? = when (value) {
        is Map<*, *> -> value.filterValues { it != null }.mapValues { stripNulls(it.value) }
        is List<*> -> value.map { stripNulls(it) }
        else -> value
    }
}

/**
 * Lists of our own types skip (and log) elements that cannot be read, so one malformed
 * record from the server does not make a whole sync fail.
 */
private object LenientListAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (type !is ParameterizedType || Types.getRawType(type) != List::class.java) return null
        val element = type.actualTypeArguments[0]
        if (!Types.getRawType(element).name.startsWith("com.example.")) return null
        val elementAdapter = moshi.adapter<Any>(element)
        return object : JsonAdapter<List<Any>>() {
            override fun fromJson(reader: JsonReader): List<Any>? {
                val raw = reader.readJsonValue() as? List<*> ?: return null
                return raw.mapNotNull {
                    try {
                        elementAdapter.fromJsonValue(it)
                    } catch (e: JsonDataException) {
                        ApiClient.skippedRecords++
                        Log.w("ApiClient", "Skipped unreadable ${Types.getRawType(element).simpleName}: ${e.message}")
                        null
                    }
                }
            }

            override fun toJson(writer: JsonWriter, value: List<Any>?) {
                if (value == null) writer.nullValue() else {
                    writer.beginArray()
                    value.forEach { elementAdapter.toJson(writer, it) }
                    writer.endArray()
                }
            }
        }
    }
}
