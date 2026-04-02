# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontobfuscate
#-renamesourcefileattribute SourceFile
#-keepattributes SourceFile,LineNumberTable

# Temp fix for androidx.window:window:1.0.0-alpha09 imported by termux-shared
# https://issuetracker.google.com/issues/189001730
# https://android-review.googlesource.com/c/platform/frameworks/support/+/1757630
-keep class androidx.window.** { *; }

# Keep application models and data classes (used by Gson, reflection)
-keep class com.termux.app.models.** { *; }
-keep class com.termux.app.configuration.models.** { *; }
-keep class com.termux.filebrowser.domain.model.** { *; }
-keep class com.termux.filebrowser.data.model.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }

# Keep Gson TypeToken and generics
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Keep serializable/parcelable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Kotlin coroutines (removal causes runtime crashes)
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# Keep RxJava
-dontwarn io.reactivex.**
-keep class io.reactivex.** { *; }

# Keep SSHJ library
-keep class com.hierynomus.** { *; }
-dontwarn com.hierynomus.**

# Keep Activities, Fragments, Services referenced from XML/manifest
-keep class * extends android.app.Activity { <init>(...); }
-keep class * extends androidx.fragment.app.Fragment { <init>(...); }
-keep class * extends android.app.Service { <init>(...); }
-keep class * extends android.app.Application { <init>(...); }
