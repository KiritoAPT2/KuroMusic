# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

## Kotlin Serialization
# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclasseswithmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclasseswithmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclasseswithmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-dontwarn kotlinx.serialization.**

-dontwarn javax.servlet.ServletContainerInitializer
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn org.slf4j.impl.StaticLoggerBinder

## Coil image loading
-keep class coil.** { *; }
-keep interface coil.** { *; }
-keep class coil.memory.** { *; }
-keep class coil.disk.** { *; }
-keep class coil.network.** { *; }
-keep class coil.decode.** { *; }
-keep class coil.fetch.** { *; }
-keep class coil.key.** { *; }
-keep class coil.size.** { *; }
-keep class coil.target.** { *; }
-keep class coil.transition.** { *; }
-keep class coil.util.** { *; }
-dontwarn coil.**

## Rules for NewPipeExtractor
-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.javascript.engine.** { *; }
-dontwarn org.mozilla.javascript.JavaToJSONConverters
-dontwarn org.mozilla.javascript.tools.**
-keep class javax.script.** { *; }
-dontwarn javax.script.**
-keep class jdk.dynalink.** { *; }
-dontwarn jdk.dynalink.**

## Logging (does not affect Timber)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    ## Leave in release builds
    #public static int i(...);
    #public static int w(...);
    #public static int e(...);
}

# Generated automatically by the Android Gradle plugin.
-dontwarn java.beans.BeanDescriptor
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
-dontwarn okhttp3.internal.Util


## Rules for PipePipeExtractor
-keep class project.pipepipe.extractor.** { *; }
-keep class project.pipepipe.shared.** { *; }

## Netty rules (used by PipePipeExtractor dependencies)
-dontwarn io.netty.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**
-dontwarn reactor.blockhound.**
-dontwarn io.micrometer.context.**
-dontwarn javax.enterprise.inject.**

## Lettuce (Redis client used by PipePipeExtractor)
-dontwarn io.lettuce.core.**

## Reactor
-dontwarn reactor.util.context.**

## Keep Wire protobuf classes
-keep class com.squareup.wire.** { *; }

## Media3 rules (Prevent ClassCastException and reflection issues in Release)
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keep class androidx.media3.ui.** { *; }
-keep interface androidx.media3.common.** { *; }
-keep interface androidx.media3.exoplayer.** { *; }
-keep interface androidx.media3.session.** { *; }
-dontwarn androidx.media3.exoplayer.**
-dontwarn androidx.media3.common.**
-dontwarn androidx.media3.session.**

# KuroMusic Models
-keep class com.kuromusic.models.** { *; }
-keep class com.kuromusic.innertube.models.** { *; }

# Serialization
-keep class com.kuromusic.innertube.utils.UtilsKt { *; }

## Ktor and OkHttp engine
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Auth classes
-keep class com.kuromusic.innertube.YouTube { *; }
-keep class com.kuromusic.innertube.InnerTube { *; }
-keep class com.kuromusic.innertube.** { *; }
-keep class com.kuromusic.playback.YouTubeSessionInterceptor { *; }
-keepclassmembers class com.kuromusic.models.** {
    <fields>;
    <methods>;
}

# Mantener nombres de variables para los detalles técnicos
-keepclassmembers class ** {
    @com.google.gson.annotations.SerializedName <fields>;
}