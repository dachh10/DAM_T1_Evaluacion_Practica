import java.util.Properties

// Cloudinary identifiers are public values kept outside version control.
// providers.fileContents keeps configuration-cache tracks on local.properties.
val localProperties = providers.fileContents(
    rootProject.layout.projectDirectory.file("local.properties")
).asText.map { text ->
    Properties().apply { text.reader().use { load(it) } }
}.orElse(Properties())

val localProperty: (String) -> String = { key ->
    localProperties.get().getProperty(key)?.takeIf { it.isNotBlank() } ?: ""
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.dam_t1_evaluacion_practica"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.dam_t1_evaluacion_practica"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "CLOUDINARY_CLOUD_NAME",
            "\"${localProperty("CLOUDINARY_CLOUD_NAME")}\""
        )
        buildConfigField(
            "String",
            "CLOUDINARY_UPLOAD_PRESET",
            "\"${localProperty("CLOUDINARY_UPLOAD_PRESET")}\""
        )
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
    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.compose.material:material-icons-extended")
    
    // Room
    implementation(libs.room.runtime)
    add("ksp", libs.room.compiler)
    implementation(libs.room.ktx)
    
    // Maps & Location
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.google.maps.android:maps-compose:4.3.3")

    // Firebase (BoM-managed: never use the retired -ktx artifacts)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // WorkManager: sync coordinator and evidence upload workers
    implementation(libs.androidx.work.runtime)

    // OkHttp: remote image loading
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Cloudinary: unsigned evidence uploads
    implementation(libs.cloudinary.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.work.testing)
    testImplementation("org.json:json:20231013")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.firebase.auth)
    androidTestImplementation(libs.firebase.firestore)
    androidTestImplementation("com.google.firebase:firebase-auth:24.0.1")
    androidTestImplementation("com.google.firebase:firebase-firestore:26.2.0")
    androidTestImplementation("org.robolectric:robolectric:4.11")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}