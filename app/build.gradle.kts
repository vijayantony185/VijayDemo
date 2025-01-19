plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.app.wifiserverdemojava"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.app.wifiserverdemojava"
        minSdk = 26
        targetSdk = 34
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

  /*  implementation (libs.vosk.android)
    implementation("net.java.dev.jna:jna:5.11.0")*/
    implementation ("net.java.dev.jna:jna:5.13.0@aar")
    implementation ("com.alphacephei:vosk-android:0.3.47@aar")
}