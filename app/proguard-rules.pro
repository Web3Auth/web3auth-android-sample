# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
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
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-dontobfuscate
-dontoptimize
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# Gson
-dontwarn sun.misc.**
-keep class com.google.gson.examples.android.model.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Retrofit
-dontnote retrofit2.Platform
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**

-keepnames class kotlinx.** { *; }

