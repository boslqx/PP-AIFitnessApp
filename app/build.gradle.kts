plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.aifitnessapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.aifitnessapp"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Room Database
    implementation("androidx.room:room-runtime:2.8.4")
    annotationProcessor("androidx.room:room-compiler:2.8.4")

    // ViewModel + LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.7")
    annotationProcessor("androidx.lifecycle:lifecycle-compiler:2.8.7")

    // Activity KTX — gives you easy ViewModel access in Activities
    implementation("androidx.activity:activity:1.11.0")

    // Fragment — needed for AlertDialog used in LogActivity
    implementation("androidx.fragment:fragment:1.8.5")
}