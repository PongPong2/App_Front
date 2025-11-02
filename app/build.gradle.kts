import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import java.util.Properties

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
        minSdk = 26 // API 26 이상 (Health Connect 최소 요구사항)
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // BuildConfig 및 Manifest Placeholders 설정
        buildConfigField(
            "String",
            "PHONE_NUMBER",
            gradleLocalProperties(rootDir, providers).getProperty("PHONE_NUMBER") ?: "DEFAULT_KEY"
        )

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
        buildConfig = true // BuildConfig 기능 활성화
        viewBinding = true // ViewBinding도 활성화
    }

    packaging {
        // Kakao SDK와 Retrofit 사용 시 필요한 리소스 충돌 방지
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/*.properties"
        }
    }
}

dependencies {
    // AndroidX & Compose 기본 의존성
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.appcompat)

    // Health Connect & WorkManager (10분 주기 데이터 수집)
    implementation("androidx.health.connect:connect-client:1.1.0-alpha12")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Retrofit (서버 통신용)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp 로깅 인터셉터 (디버깅용)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Kotlin Coroutines (비동기 실행 및 Worker)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")

    // 기타 필수/편의 라이브러리
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("com.jakewharton.timber:timber:5.0.1") // 로그 확인용

    // Kakao SDK
    val kakaoSdkVersion = "2.21.0"
    implementation("com.kakao.sdk:v2-share:$kakaoSdkVersion")
    implementation("com.kakao.sdk:v2-talk:$kakaoSdkVersion")
    implementation("com.kakao.sdk:v2-user:$kakaoSdkVersion")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
