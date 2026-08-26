import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "me.nanova.summaryexpressive"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "me.nanova.summaryexpressive"
        minSdk = 33
        targetSdk = 37
        versionCode = 50
        versionName = "1.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val keyPropertiesFile = rootProject.file("keystore.properties")
        val keyProperties = Properties()
        if (keyPropertiesFile.exists()) {
            keyProperties.load(FileInputStream(keyPropertiesFile))
        }

        create("release") {
            keyAlias = keyProperties.getProperty("keyAlias")
            keyPassword = keyProperties.getProperty("keyPassword")
            storeFile = if (keyProperties.getProperty("storeFile") != null) rootProject.file(keyProperties.getProperty("storeFile")) else null
            storePassword = keyProperties.getProperty("storePassword")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("gms") {
            dimension = "distribution"
        }
        create("standalone") {
            dimension = "distribution"
            applicationIdSuffix = ".standalone"
            versionNameSuffix = "-standalone"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    packaging {
        resources {
            excludes += "META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/INDEX.LIST"
            merges += "META-INF/DEPENDENCIES"
        }
    }

    androidResources {
        generateLocaleConfig = true
    }

    lint {
        disable.add("MissingTranslation")
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_2_4
        jvmTarget = JvmTarget.JVM_25
    }
}

dependencies {
    // Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.webkit)

    // DI (Hilt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.hilt.navigation.compose)

    // Jetpack Compose
    // https://developer.android.com/develop/ui/compose/bom/bom-mapping
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)

    // Paging
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.paging.runtime.ktx)

    // Data Persistence
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ML & AI
    // Custom configurations for build flavors to manage ML model packaging
    // Bundles model in APK
    "standaloneImplementation"(libs.mlkit.text.recognition)
    // Uses Google Play Services
    "gmsImplementation"(libs.play.services.mlkit.text.recognition)
    
    // Koog Stable (Version 1.1.1)
    implementation(libs.koog.agents)
    implementation(libs.http.client.ktor)
    implementation(libs.prompt.executor.openai.client.android)
    implementation(libs.prompt.executor.anthropic.client.android)
    implementation(libs.prompt.executor.ollama.client.android)
    implementation(libs.prompt.executor.openrouter.client.android)
    implementation(libs.prompt.executor.bedrock.client.android)
    implementation(libs.koog.agents.additions)
    implementation(libs.prompt.executor.google.client.android)
    implementation(libs.prompt.executor.deepseek.client.android)
    implementation(libs.prompt.executor.mistralai.client.android)
    implementation(libs.prompt.executor.dashscope.client.android)

    implementation(libs.kotlinx.coroutines.play.services)

    // Networking
    implementation(libs.ktor.client.android)

    // Serialization & Utilities
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.jsoup)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    // Debug & Tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    // debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
