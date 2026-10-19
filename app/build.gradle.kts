plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "org.bukhari.competition"
    minSdk = 26
    targetSdk = 36
    // Raise on every release: the server can require a minimum versionCode (MIN_ANDROID_VERSION_CODE).
    versionCode = 4
    versionName = "1.2.1"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

  }

  // Release signing is configured only when all keystore env vars are present; otherwise the
  // release build is left unsigned. Debug builds use Android's default debug keystore.
  val keystorePath = System.getenv("KEYSTORE_PATH")
  val storePasswordEnv = System.getenv("STORE_PASSWORD")
  val keyPasswordEnv = System.getenv("KEY_PASSWORD")
  val hasReleaseKeystore =
    !keystorePath.isNullOrBlank() && !storePasswordEnv.isNullOrBlank() && !keyPasswordEnv.isNullOrBlank()

  signingConfigs {
    if (hasReleaseKeystore) {
      create("release") {
        storeFile = file(keystorePath!!)
        storePassword = storePasswordEnv
        keyAlias = "upload"
        keyPassword = keyPasswordEnv
      }
    }
  }

  // URL of the shared server (Web/server.ts): -PAPI_BASE_URL=... or the API_BASE_URL env var.
  // Debug defaults to a server on the developer machine as seen from the emulator.
  // Release has no default: it must be an https URL, otherwise the build fails.
  val apiBaseUrl = (project.findProperty("API_BASE_URL") as String?) ?: System.getenv("API_BASE_URL")

  buildTypes {
    debug {
      buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl ?: "http://10.0.2.2:3000/"}\"")
    }
    release {
      buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl ?: ""}\"")
      isCrunchPngs = false
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      if (hasReleaseKeystore) signingConfig = signingConfigs.getByName("release")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}



// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  // Unused by the code; kept because it fixes the Compose versions the build resolves. Remove with the next
  // dependency refresh (needs network to resolve the BOM versions).
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.converter.moshi)

  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  implementation(libs.androidx.security.crypto)
  implementation(libs.postgresql)
  implementation(libs.jbcrypt)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

// Release builds must point at an https server.
gradle.taskGraph.whenReady {
  val url = (project.findProperty("API_BASE_URL") as String?) ?: System.getenv("API_BASE_URL")
  if (allTasks.any { it.name.contains("Release") } && (url == null || !url.startsWith("https://"))) {
    throw GradleException("Set API_BASE_URL (https://...) for release builds, e.g. -PAPI_BASE_URL=https://example.org/")
  }
  // A build meant for users must be signed (MOB-13). CI's compile check passes no flag and stays unsigned.
  val requireSigning = (project.findProperty("requireReleaseSigning") as String?) == "true"
  if (requireSigning && allTasks.any { it.name.contains("Release") } &&
    (System.getenv("KEYSTORE_PATH").isNullOrBlank() || System.getenv("STORE_PASSWORD").isNullOrBlank() || System.getenv("KEY_PASSWORD").isNullOrBlank())
  ) {
    throw GradleException("requireReleaseSigning: set KEYSTORE_PATH, STORE_PASSWORD and KEY_PASSWORD to build a release for users.")
  }
}
