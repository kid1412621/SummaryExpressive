plugins {
    id("com.android.application") version "9.3.1" apply false
    // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.serialization
    kotlin("plugin.serialization") version "2.4.10" apply false
    // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.compose
    kotlin("plugin.compose") version "2.4.10" apply false
    //  https://github.com/google/ksp/releases
    id("com.google.devtools.ksp") version "2.3.11" apply false
    // https://mvnrepository.com/artifact/com.google.dagger/hilt-android-gradle-plugin
    id("com.google.dagger.hilt.android") version "2.60.1" apply false
}