plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
    // Comment out google-services until Firebase is properly configured
    // id("com.google.gms.google-services")
}

import java.util.Properties

// Optional local keystore properties support. If a `keystore.properties` file
// is placed in the project root, its values will be used; otherwise the
// existing environment-variable-based behavior is preserved. This lets devs
// run `:app:assembleRelease` locally without CI secrets by using a local
// keystore or falling back to the debug signing config.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties()
if (keystorePropsFile.exists()) {
    keystorePropsFile.inputStream().use { keystoreProps.load(it) }
}
// Accept multiple common property name variants so CI workflows and local
// templates using different keys (e.g. `storeFile` vs `KEYSTORE_FILE`) work.
val propStorePassword: String? = keystoreProps.getProperty("STORE_PASSWORD") ?: keystoreProps.getProperty("storePassword")
val propKeyAlias: String? = keystoreProps.getProperty("KEY_ALIAS") ?: keystoreProps.getProperty("keyAlias")
val propKeyPassword: String? = keystoreProps.getProperty("KEY_PASSWORD") ?: keystoreProps.getProperty("keyPassword")
val propKeystoreFile: String? = keystoreProps.getProperty("KEYSTORE_FILE")
    ?: keystoreProps.getProperty("KEYSTORE")
    ?: keystoreProps.getProperty("storeFile")
    ?: keystoreProps.getProperty("keystoreFile")
    ?: keystoreProps.getProperty("keystore")

android {
    namespace = "com.warrantyvault"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.warrantyvault"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = propKeystoreFile ?: "${rootProject.projectDir}/warrantyvault-keystore.jks"
            storeFile = file(storeFilePath)
            storePassword = propStorePassword ?: System.getenv("STORE_PASSWORD")
            keyAlias = propKeyAlias ?: System.getenv("KEY_ALIAS")
            keyPassword = propKeyPassword ?: System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val releaseSigningConfig = if (keystorePropsFile.exists() || System.getenv("STORE_PASSWORD") != null) {
                signingConfigs.getByName("release")
            } else {
                // Local developer fallback: use debug signing when no release keystore is available
                signingConfigs.getByName("debug")
            }
            signingConfig = releaseSigningConfig
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/license.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/notice.txt"
            excludes += "/META-INF/*.kotlin_module"
        }
    }
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    // Be resilient to missing annotation types during kapt for test variants
    correctErrorTypes = true
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Testing dependencies for Room migration tests
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:core:1.5.0")

    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")

    // Encrypted SharedPreferences for secure local storage
    implementation("androidx.security:security-crypto:1.1.0")
    
    // Biometric authentication
    implementation("androidx.biometric:biometric:1.1.0")
    
    // Hilt WorkManager integration
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")
    
    // Google Drive API for cloud backup
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.api-client:google-api-client-android:2.2.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")
    implementation("com.google.http-client:google-http-client-gson:1.43.3")
    
    // Firebase - Commented out until properly configured
    // Uncomment these and enable google-services plugin to use Firebase features
    /*
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    */

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    // Unit test dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.0")
    // Unit test libs are already available via `testImplementation`; kapt.correctErrorTypes
    // handles missing types during annotation processing for test variants.
}
