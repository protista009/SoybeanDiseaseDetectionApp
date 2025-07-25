plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")

// Add if using Firebase

}


android {
    namespace = "com.example.soyabean_disease"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.soyabean_disease"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a") // use parentheses + double quotes
            isUniversalApk = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    aaptOptions {
        noCompress("tflite")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }



    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets", "src/main/assets/2", "src/main/assets/3")
        }
    }
}

dependencies {

    implementation(libs.room.common.jvm)
    val room_version = "2.5.2"  // or latest stable


    implementation ("androidx.room:room-runtime:$room_version")
    annotationProcessor ("androidx.room:room-compiler:$room_version")

// For Kotlin (optional)
    implementation ("androidx.transition:transition:1.4.1")


    // Image Cropper

    implementation("com.github.CanHub:Android-Image-Cropper:4.3.3")

    // Choose ONE of these TensorFlow Lite implementations (not both):

    // OPTION 1: Official TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.14.0")

    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")
     implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.14.0") // Only if needed

    // OR OPTION 2: Google's LiteRT (comment out the above if using this)
//     implementation("com.google.ai.edge.litert:litert:1.0.1")
//     implementation("com.google.ai.edge.litert:litert-gpu:1.0.1")
//     implementation("com.google.ai.edge.litert:litert-support:1.0.1")
//     implementation("com.google.ai.edge.litert:litert-metadata:1.0.1")

    // AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // CameraX
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    // Firebase (only if needed)


    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}