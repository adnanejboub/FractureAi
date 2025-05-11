plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.fractureai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fractureai"
        minSdk = 29
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
    buildFeatures {
        viewBinding = true
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

    // Firebase dependencies
    implementation("com.google.firebase:firebase-auth:23.2.0")
    implementation("com.google.firebase:firebase-core:21.1.1")
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-analytics:22.4.0")
    implementation("com.google.firebase:firebase-storage:21.0.1")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-ml-modeldownloader")
    //Prefixe des numeros de telephone
    implementation("com.hbb20:ccp:2.7.3")

    //authentification par google et fb
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.facebook.android:facebook-login:16.3.0")
    implementation("com.facebook.android:facebook-login:latest.release")
    implementation("com.facebook.android:facebook-android-sdk:[4,5)")
    implementation("com.facebook.android:facebook-share:16.2.0")

    //image
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.5.0")

    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    // OkHttp pour les appels API Gemini
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Gson pour parsing JSON
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("org.tensorflow:tensorflow-lite:2.10.0")
    implementation("com.google.firebase:firebase-firestore:24.10.0")

    implementation("org.tensorflow:tensorflow-lite:2.10.0")
    implementation ("com.google.firebase:firebase-firestore:24.10.0")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.core:core:1.12.0")
    implementation ("androidx.appcompat:appcompat:1.6.1")




}