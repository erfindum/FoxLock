# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.examples.android.model.** { *; }

-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

-keep public class * implements com.bumptech.glide.module.GlideModule

-keep class com.mopub.mobileads.** {*;}
-dontwarn com.mopub.mobileads.**
-dontwarn com.facebook.ads.internal.**
-keepclassmembers class com.millennialmedia** {
public *;
}
-keep class com.millennialmedia**

-keep class com.google.ads.mediation.admob.AdMobAdapter {
    *;
}

-keep class com.google.ads.mediation.AdUrlAdapter {
    *;
}

#Start-App Ad

-keep class com.startapp.** { *; }

-keepattributes Exceptions, InnerClasses, Signature, Deprecated, SourceFile,LineNumberTable, *Annotation*, EnclosingMethod
-dontwarn android.webkit.JavascriptInterface
-dontwarn com.startapp.**

#Avocarrot Ad
-dontwarn com.avocarrot.**
-keep class com.avocarrot.** { *; }
-keepclassmembers class com.avocarrot.** { *; }
-keep class com.google.android.exoplayer2.SimpleExoPlayer

#Pollfish
-dontwarn com.pollfish.**
-keep class com.pollfish.** { *; }

# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions
-dontwarn java.nio.file.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

-keep class com.smartfoxitsolutions.foxlock.ResetPasswordResponse {*;}
-keep class com.smartfoxitsolutions.foxlock.loyaltybonus.** {*;}








