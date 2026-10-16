package com.example.data.local

import android.util.Log
import androidx.room.TypeConverter
import com.example.data.model.AggregationDetails
import com.example.data.model.AssistantReception
import com.example.data.model.CeremonyDetails
import com.example.data.model.ExamApproval
import com.example.data.model.ScoringDetails
import com.example.data.model.StagesMap
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)

    private val intListType = Types.newParameterizedType(List::class.java, Int::class.javaObjectType)
    private val intListAdapter = moshi.adapter<List<Int>>(intListType)

    private val stringBooleanMapType = Types.newParameterizedType(
        Map::class.java,
        String::class.java,
        Boolean::class.javaObjectType
    )
    private val stringBooleanMapAdapter = moshi.adapter<Map<String, Boolean>>(stringBooleanMapType)

    private val stagesMapAdapter = moshi.adapter(StagesMap::class.java)
    private val scoringDetailsAdapter = moshi.adapter(ScoringDetails::class.java)
    private val aggregationDetailsAdapter = moshi.adapter(AggregationDetails::class.java)
    private val ceremonyDetailsAdapter = moshi.adapter(CeremonyDetails::class.java)
    private val assistantReceptionAdapter = moshi.adapter(AssistantReception::class.java)
    private val examApprovalAdapter = moshi.adapter(ExamApproval::class.java)

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.let { stringListAdapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            stringListAdapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            // A stored value that no longer parses is shown as empty; log it so it is noticed.
            Log.w("Converters", "Unreadable stored value: ${value.take(200)}", e)
            emptyList()
        }
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String {
        return value?.let { intListAdapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            intListAdapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            // A stored value that no longer parses is shown as empty; log it so it is noticed.
            Log.w("Converters", "Unreadable stored value: ${value.take(200)}", e)
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringBooleanMap(value: Map<String, Boolean>?): String {
        return value?.let { stringBooleanMapAdapter.toJson(it) } ?: "{}"
    }

    @TypeConverter
    fun toStringBooleanMap(value: String?): Map<String, Boolean> {
        if (value.isNullOrBlank()) return emptyMap()
        return try {
            stringBooleanMapAdapter.fromJson(value) ?: emptyMap()
        } catch (e: Exception) {
            // A stored value that no longer parses is shown as empty; log it so it is noticed.
            Log.w("Converters", "Unreadable stored value: ${value.take(200)}", e)
            emptyMap()
        }
    }

    @TypeConverter
    fun fromStagesMap(value: StagesMap?): String {
        return value?.let { stagesMapAdapter.toJson(it) } ?: "{}"
    }

    @TypeConverter
    fun toStagesMap(value: String?): StagesMap {
        if (value.isNullOrBlank()) return StagesMap()
        return try {
            stagesMapAdapter.fromJson(value) ?: StagesMap()
        } catch (e: Exception) {
            // A stored value that no longer parses is shown as empty; log it so it is noticed.
            Log.w("Converters", "Unreadable stored value: ${value.take(200)}", e)
            StagesMap()
        }
    }

    @TypeConverter
    fun fromScoringDetails(value: ScoringDetails?): String {
        return value?.let { scoringDetailsAdapter.toJson(it) } ?: "{}"
    }

    @TypeConverter
    fun toScoringDetails(value: String?): ScoringDetails {
        if (value.isNullOrBlank()) return ScoringDetails()
        return try {
            scoringDetailsAdapter.fromJson(value) ?: ScoringDetails()
        } catch (e: Exception) {
            // A stored value that no longer parses is shown as empty; log it so it is noticed.
            Log.w("Converters", "Unreadable stored value: ${value.take(200)}", e)
            ScoringDetails()
        }
    }

    @TypeConverter
    fun fromAggregationDetails(value: AggregationDetails?): String {
        return value?.let { aggregationDetailsAdapter.toJson(it) } ?: "{}"
    }

    @TypeConverter
    fun toAggregationDetails(value: String?): AggregationDetails {
        if (value.isNullOrBlank()) return AggregationDetails()
        return try {
            aggregationDetailsAdapter.fromJson(value) ?: AggregationDetails()
        } catch (e: Exception) {
            // A stored value that no longer parses is shown as empty; log it so it is noticed.
            Log.w("Converters", "Unreadable stored value: ${value.take(200)}", e)
            AggregationDetails()
        }
    }

    @TypeConverter
    fun fromCeremonyDetails(value: CeremonyDetails?): String {
        return value?.let { ceremonyDetailsAdapter.toJson(it) } ?: "{}"
    }

    @TypeConverter
    fun toCeremonyDetails(value: String?): CeremonyDetails {
        if (value.isNullOrBlank()) return CeremonyDetails()
        return try {
            ceremonyDetailsAdapter.fromJson(value) ?: CeremonyDetails()
        } catch (e: Exception) {
            // A stored value that no longer parses is shown as empty; log it so it is noticed.
            Log.w("Converters", "Unreadable stored value: ${value.take(200)}", e)
            CeremonyDetails()
        }
    }

    @TypeConverter
    fun fromAssistantReception(value: AssistantReception?): String {
        return value?.let { assistantReceptionAdapter.toJson(it) } ?: ""
    }

    @TypeConverter
    fun toAssistantReception(value: String?): AssistantReception? {
        if (value.isNullOrBlank()) return null
        return try {
            assistantReceptionAdapter.fromJson(value)
        } catch (e: Exception) {
            // A stored value that no longer parses is shown as empty; log it so it is noticed.
            Log.w("Converters", "Unreadable stored value: ${value.take(200)}", e)
            null
        }
    }

    @TypeConverter
    fun fromExamApproval(value: ExamApproval?): String {
        return value?.let { examApprovalAdapter.toJson(it) } ?: ""
    }

    @TypeConverter
    fun toExamApproval(value: String?): ExamApproval? {
        if (value.isNullOrBlank()) return null
        return try {
            examApprovalAdapter.fromJson(value)
        } catch (e: Exception) {
            // A stored value that no longer parses is shown as empty; log it so it is noticed.
            Log.w("Converters", "Unreadable stored value: ${value.take(200)}", e)
            null
        }
    }
}
