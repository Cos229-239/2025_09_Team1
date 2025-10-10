import org.gradle.kotlin.dsl.annotationProcessor

//plugins {
//    alias(libs.plugins.android.application)
//    id("com.android.application")
//    id("org.jetbrains.kotlin.android")
//    id("kotlin-kapt")
//}

plugins {
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}




android {
    namespace = "com.example.wildercards"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.wildercards"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "DEEPAI_API_KEY", "\"4e006901-a82f-4f28-8491-a037ee4d8aa2\"")


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

    buildFeatures{
        viewBinding = true
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
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("androidx.core:core-ktx:1.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")


    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.okhttp)
    implementation(libs.json)
    implementation(libs.circleimageview)
    implementation(libs.core.ktx)
    implementation(libs.google.firebase.storage)
//    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.cardview)
    implementation(libs.constraintlayout.v214)

    implementation(libs.okhttp)
    implementation(libs.json)

    implementation(libs.circleimageview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.material.v1120)
    implementation(libs.cardview)
    implementation(libs.constraintlayout.v214)
    implementation(platform(libs.firebase.bom.v3310))
    implementation(libs.com.google.firebase.firebase.storage)
    implementation(libs.glide)
    annotationProcessor(libs.compiler)
    implementation(libs.play.services.auth.v2120)
    implementation(libs.firebase.firestore)
//    implementation("androidx.core:core-ktx:1.x.x")
//    implementation("androidx.appcompat:appcompat:1.x.x")

    implementation(libs.core.ktx.v1120)
    implementation(libs.appcompat.v161)
    implementation(libs.glide.v4160)
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    // implementation("com.github.bumptech.glide:glide:4.16.0")

    // implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

}