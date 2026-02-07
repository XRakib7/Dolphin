plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.softcraft.dolphin"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.softcraft.dolphin"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Source: https://mvnrepository.com/artifact/androidx.datastore/datastore-preferences
    runtimeOnly("androidx.datastore:datastore-preferences:1.2.0")
    // Source: https://mvnrepository.com/artifact/androidx.datastore/datastore-preferences-core
    runtimeOnly("androidx.datastore:datastore-preferences-core:1.2.0")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("javax.inject:javax.inject:1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}