import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties
import org.gradle.kotlin.dsl.annotationProcessor

//plugins {
//    alias(libs.plugins.android.application)
//    id("com.android.application")
//    id("org.jetbrains.kotlin.android")
//    id("kotlin-kapt")
//}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

val localProp = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProp.load(FileInputStream(localPropertiesFile))
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
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

        buildConfigField(
            "String",
            "GOOGLE_VISION_API",
            localProp.getProperty("GOOGLE_VISION_API_KEY", "\"\"")
        )
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
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {        resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1,DEPENDENCIES,INDEX.LIST}"

    }
    }
}

dependencies {
   // implementation("com.github.bumptech.glide:glide:4.16.0")
   // implementation("androidx.core:core-ktx:1.12.0")

    implementation(platform(libs.grpc.bom))
    implementation(libs.google.cloud.vision){
        exclude(group = "com.google.protobuf", module = "protobuf-java")
        exclude(group = "com.google.api.grpc", module = "proto-google-common-protos")
    }
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // Google Services
    implementation(libs.play.services.auth)
    implementation(libs.googleid)

    // UI
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.activity)
    implementation(libs.core.ktx)
    implementation(libs.cardview)
    implementation(libs.circleimageview)

    // Glide for Image Loading
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.okhttp)

    // Other
    implementation(libs.json)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    // implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    //    implementation(libs.core.ktx.v1120)
    //    implementation(libs.appcompat.v161)
    //    implementation(libs.glide.v4160)
    //    implementation("androidx.core:core-ktx:1.12.0")
    //    implementation("androidx.appcompat:appcompat:1.6.1")
    //    implementation("com.google.android.material:material:1.11.0")
    //    // implementation("com.github.bumptech.glide:glide:4.16.0")
    //
    //    // implementation("com.squareup.okhttp3:okhttp:4.12.0")
    //    implementation("com.google.code.gson:gson:2.10.1")
}

