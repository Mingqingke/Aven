plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.aven.app"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.aven.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 10
        versionName = "1.0.9"
        ndk { abiFilters += listOf("arm64-v8a") }
        resourceConfigurations += listOf("zh", "en")
    }

    val keystoreProps = rootProject.file("keystore.properties")
    val ks = mutableMapOf<String, String>()
    if (keystoreProps.exists()) {
        keystoreProps.readLines().forEach { line ->
            val idx = line.indexOf('=')
            if (idx > 0) ks[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
        }
    }
    val hasReleaseKey = ks.containsKey("storeFile")

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = rootProject.file(ks.getValue("storeFile"))
                storePassword = ks.getValue("storePassword")
                keyAlias = ks.getValue("keyAlias")
                keyPassword = ks.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        debug { versionNameSuffix = "" }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = if (hasReleaseKey) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures { compose = true; buildConfig = true }

    lint { abortOnError = false; checkReleaseBuilds = false }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/*.kotlin_module"
        }
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // MIUIX 官方组件（HyperOS 界面体系）
    implementation("top.yukonga.miuix.kmp:miuix-ui:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-preference:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-icons:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-squircle:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-nav:0.9.4")

    // 数据层
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    kapt("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.20")
    kapt("androidx.room:room-compiler:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.3")

    // 网络与图片
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    // 本地 OCR（离线中文识别）
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")

    // 生物识别与 FragmentActivity
    implementation("androidx.fragment:fragment-ktx:1.8.9")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}
