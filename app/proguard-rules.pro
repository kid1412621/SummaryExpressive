# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Global Attributes
-keepattributes Signature,*Annotation*,ServiceLoader,EnclosingMethod,InnerClasses

# Preserve the kotlin.Metadata annotation, which is required for reflection to work.
-keepattributes kotlin.Metadata
-keep class kotlin.Metadata { *; }

# 1. ai.koog library rules
# The library uses reflection and ServiceLoader extensively for dynamic tool discovery.
-keep class ai.koog.agents.core.tools.Tool { *; }
-keep class ai.koog.prompt.executor.model.PromptExecutor { *; }
-keep class ai.koog.prompt.llm.LLMProvider { *; }
-keepclassmembers class * implements ai.koog.agents.core.tools.Tool { *; }
-keep,allowshrinking,allowoptimization class * implements ai.koog.prompt.executor.model.PromptExecutor
-keep,allowshrinking,allowoptimization class * implements ai.koog.prompt.llm.LLMProvider

# Preserve ServiceLoader implementations for KoogHttpClient
-keep class * implements ai.koog.http.client.KoogHttpClient$Factory

# Suppress warnings for Java 9+ features not available on Android
-dontwarn java.lang.ProcessHandle
-dontwarn ai.koog.agents.ext.tool.shell.JvmShellCommandExecutor

# 2. Kotlin Reflection
# Narrowed keep for kotlin-reflect to reduce impact on binary size.
-keep class kotlin.reflect.KClass { *; }
-keep class kotlin.reflect.KType { *; }
-keep,allowshrinking,allowoptimization class kotlin.reflect.KProperty* { *; }
-keep class kotlin.reflect.KCallable { *; }
-dontwarn kotlin.reflect.**

# 3. Ktor & Networking
-dontwarn io.ktor.**

# Ktor and its dependencies (Netty, Reactor, etc.) have optional references
# to classes not present in Android.
-dontwarn com.google.auto.value.AutoValue$CopyAnnotations
-dontwarn com.google.errorprone.annotations.**
-dontwarn io.micrometer.context.**
-dontwarn io.netty.**
-dontwarn io.opentelemetry.api.incubator.**
-dontwarn javax.enterprise.inject.spi.Extension
-dontwarn jdk.jfr.**
-dontwarn jdk.net.**
-dontwarn org.HdrHistogram.**
-dontwarn org.LatencyUtils.**
-dontwarn org.eclipse.jetty.**
-dontwarn reactor.blockhound.integration.BlockHoundIntegration
-dontwarn java.lang.management.**
-dontwarn com.fasterxml.jackson.core.JsonFactory
-dontwarn com.fasterxml.jackson.core.JsonGenerator

# 4. jsoup
-dontwarn com.google.re2j.**
-dontwarn org.jsoup.helper.Re2jRegex**

# 5. Structured LLM response model (inspected reflectively for schema generation)
-keep class me.nanova.summaryexpressive.model.LlmSummaryResponse { *; }
