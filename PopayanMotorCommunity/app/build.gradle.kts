import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// La URL del backend se lee de local.properties (no se sube a git): api.baseUrl=http://192.168.1.10:8000/api/
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val apiBaseUrl = localProps.getProperty("api.baseUrl") ?: "http://10.0.2.2:8000/api/"  // emulador → PC

android {
    namespace = "com.david.popayanmotor"
    compileSdk {
        version = release(37)
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.david.popayanmotor"
        minSdk = 24
        // 36 y no 37: Android 17 exige el permiso ACCESS_LOCAL_NETWORK para llegar al backend local (10.0.2.2 / IP del PC)
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.coroutines.android)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    testImplementation(libs.junit)
}
