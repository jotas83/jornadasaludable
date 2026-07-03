plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.navigation.safeargs)
}

android {
    namespace = "com.jornadasaludable.app"
    compileSdk = 36
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "com.jornadasaludable.app"
        minSdk        = 26
        targetSdk     = 34
        versionCode   = 1
        versionName   = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Flavors por entorno de red. Cada uno fija su API_BASE_URL para no tener
    // que editarla a mano al cambiar entre emulador y móvil físico.
    //   - emulador:    10.0.2.2 mapea a localhost del host (solo emulador).
    //   - dispositivo: IP del host en la LAN (móvil físico en la misma red).
    flavorDimensions += "entorno"
    productFlavors {
        create("emulador") {
            dimension = "entorno"
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2/jornadasaludable/api/v1/\"")
        }
        create("dispositivo") {
            dimension = "entorno"
            buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.140/jornadasaludable/api/v1/\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // API_BASE_URL la fija cada product flavor (emulador / dispositivo).
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // ---- AndroidX core + Material 3 ----
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.recyclerview)

    // ---- Markwon (renderizado Markdown del contenido legal) ----
    implementation(libs.markwon.core)

    // ---- Lifecycle: ViewModel + LiveData (MVVM) ----
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // ---- Retrofit + Gson + OkHttp + logging ----
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // ---- Room ----
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // ---- WorkManager + Hilt-Work ----
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    kapt(libs.androidx.hilt.compiler)

    // ---- Hilt ----
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // ---- Navigation Component ----
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // ---- DataStore (JWT storage) ----
    implementation(libs.androidx.datastore.preferences)
}

// Hilt + KAPT necesita allowed annotation processor sources
kapt {
    correctErrorTypes = true
}
