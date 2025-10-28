import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
// import java.util.Properties  <-- 필요 없음

// 이전의 Properties 로드 관련 코드 모두 삭제
// val properties = Properties() <-- 삭제
// val localPropertiesFile = rootProject.file("local.properties") <-- 삭제
// println("DEBUG_PROP_CHECK: " + properties.getProperty("PHONE_NUMBER", "---NOT FOUND---")) <-- 삭제
// if (localPropertiesFile.exists()) { ... } <-- 삭제

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // **************************************************
        buildConfigField(
            "String",
            "PHONE_NUMBER",
            gradleLocalProperties(rootDir, providers).getProperty("PHONE_NUMBER") ?: "DEFAULT_KEY"
        )
        // **************************************************

        manifestPlaceholders["K_API_KEY"] = gradleLocalProperties(rootDir, providers)
            .getProperty("KAKAO_API_KEY") ?: "DEFAULT_KAKAO_KEY"
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.health.connect:connect-client:1.1.0-alpha12")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
//    Retrofit (서버 통신용)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
//   비동기 실행
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
//  로그 확인용
    implementation("com.jakewharton.timber:timber:5.0.1")

    implementation("androidx.datastore:datastore-preferences:1.1.7")

    implementation("com.google.android.gms:play-services-location:21.0.1")

    val kakaoSdkVersion = "2.21.0"
    implementation("com.kakao.sdk:v2-share:$kakaoSdkVersion")
    implementation("com.kakao.sdk:v2-talk:$kakaoSdkVersion")
    implementation("com.kakao.sdk:v2-user:$kakaoSdkVersion")
}
