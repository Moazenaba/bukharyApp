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
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

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
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.work.runtime.ktx)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)

  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  implementation(libs.androidx.security.crypto)
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
}
