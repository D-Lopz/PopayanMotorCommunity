import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// URLs y firma se leen de local.properties (no se sube a git):
//   api.baseUrl=http://192.168.1.10:8000/api/            (debug: backend local)
//   api.releaseUrl=https://<app>.azurewebsites.net/api/  (release: Azure)
//   release.storeFile / release.storePassword / release.keyAlias / release.keyPassword
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val apiBaseUrl = localProps.getProperty("api.baseUrl") ?: "http://10.0.2.2:8000/api/"  // emulador → PC
val apiReleaseUrl = localProps.getProperty("api.releaseUrl").orEmpty()
val releaseStoreFile = localProps.getProperty("release.storeFile")

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
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = localProps.getProperty("release.storePassword")
                keyAlias = localProps.getProperty("release.keyAlias")
                keyPassword = localProps.getProperty("release.keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
            // http solo en debug: el backend local no tiene https
            manifestPlaceholders["usesCleartext"] = "true"
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$apiReleaseUrl\"")
            manifestPlaceholders["usesCleartext"] = "false"
            signingConfig = signingConfigs.findByName("release")
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

// Un APK release sin URL de Azure no podría conectarse: se detiene antes de empaquetarlo.
tasks.matching { it.name == "packageRelease" || it.name == "bundleRelease" }.configureEach {
    val url = apiReleaseUrl  // copia local: la caché de configuración no admite referencias al script
    doFirst {
        require(url.startsWith("https://")) {
            "Falta api.releaseUrl=https://... en local.properties"
        }
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
    // Solo para GENERAR el QR de la inscripción (Java puro, Apache-2.0, sin permisos ni Play Services)
    implementation(libs.zxing.core)
    testImplementation(libs.junit)
}
