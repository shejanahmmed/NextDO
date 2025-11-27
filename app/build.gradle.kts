plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.shejan.nextdo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.shejan.nextdo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    
    // Missing critical dependencies
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")

    val lifecycle_version = "2.7.0"
    implementation("androidx.lifecycle:lifecycle-viewmodel:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-livedata:$lifecycle_version")

    val nav_version = "2.7.7"
    implementation("androidx.navigation:navigation-fragment:$nav_version")
    implementation("androidx.navigation:navigation-ui:$nav_version")

    val work_version = "2.9.0"
    implementation("androidx.work:work-runtime:$work_version")

    val preference_version = "1.2.1"
    implementation("androidx.preference:preference:$preference_version")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}