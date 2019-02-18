# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in d:\Android\sdk/tools/proguard/proguard-android.txt
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

#指定代码的压缩级别
#
#-optimizationpasses 5
#-dontusemixedcaseclassnames
#-dontskipnonpubliclibraryclasses
#-dontoptimize
#-dontpreverify
#-verbose
#-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
#-keepattributes *Annotation*
#-keepattributes Signature
#-keepattributes EnclosingMethod
#-keepattributes SourceFile,LineNumberTable

#-keepclasseswithmembers class * implements java.io.Serializable{
#    <fields>;
#    <methods>;
#}
#-keep class **.bean.**{*;}
#-keep class * implements android.os.Parcelable {
#    public static final android.os.Parcelable$Creator *;
#}
#-keepclassmembers enum * {
#    public static **[] values();
#    public static ** valueOf(java.lang.String);
#}
#-keepclasseswithmembers class * implements android.os.Parcelable{
#    <fields>;
#    <methods>;
#}
#-keep class * extends cassette.ui.adapter.holder.BaseViewHolder{*;}

#-keep class **.R$* {*;}
#-keep public class tanzent.cassette.R$*{
#public static final int *;
#}
#-keepclasseswithmembers class * extends android.app.Activity{
#    <methods>;
#}
#-keep public class * extends android.app.Activity
#-keep public class * extends android.app.Application
#-keep public class * extends android.app.Service
#-keep public class * extends android.content.BroadcastReceiver
#-keep public class * extends android.content.ContentProvider
#-keep public class * extends android.app.backup.BackupAgentHelper
#-keep public class * extends android.preference.Preference
#-keep public class com.android.vending.licensing.ILicensingService
#-keep public class * extends java.lang.annotation.Annotation
#-keep public class * extends android.os.Handler

#-keep public class **.R$*{
#   public static final int *;
#}

#-keep,allowobfuscation @interface com.facebook.common.internal.DoNotStrip
#-keep,allowobfuscation @interface com.facebook.soloader.DoNotOptimize
#-keep public class tanzent.cassette.ui.adapter.** { *; }
#-keep @com.facebook.common.internal.DoNotStrip class *
#-keepclassmembers class * {
#    @com.facebook.common.internal.DoNotStrip *;
#}

#-keep @com.facebook.soloader.DoNotOptimize class *
#-keepclassmembers class * {
#    @com.facebook.soloader.DoNotOptimize *;
#}

#-keepclassmembers class * {
#    native <methods>;
#}

#-dontwarn okio.**
#-dontwarn com.squareup.okhttp.**
#-dontwarn okhttp3.**
#-dontwarn javax.annotation.**
#-dontwarn com.android.volley.toolbox.**
#-dontwarn com.facebook.infer.**


#-dontwarn retrofit2.**
#-keep class retrofit2.** { *; }

#-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
# long producerIndex;
# long consumerIndex;
#}
#-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
# rx.internal.util.atomic.LinkedQueueNode producerNode;
#}
#-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
# rx.internal.util.atomic.LinkedQueueNode consumerNode;
#}

#-keep class com.simplecityapps.recyclerview_fastscroll.** { *; }

#-keep class com.tbruyelle.rxpermissions.**{*;}

#-dontwarn java.lang.invoke.*
#-dontwarn **$$Lambda$*

#-dontwarn android.net.compatibility.**
#-dontwarn android.net.http.**
#-dontwarn com.android.internal.http.multipart.**
#-dontwarn org.apache.commons.**
#-dontwarn org.apache.http.**
#-keep class android.net.compatibility.**{*;}
#-keep class android.net.http.**{*;}
#-keep class com.android.internal.http.multipart.**{*;}
#-keep class org.apache.commons.**{*;}
#-keep class org.apache.http.**{*;}

#-dontwarn kotlin.**

#-keep class org.jaudiotagger.** { *; }
#-dontwarn org.jaudiotagger.**

#-keep class tv.danmaku.ijk.media.player.** { *; }

#-dontwarn com.tencent.bugly.**
#-keep public class com.tencent.bugly.**{*;}