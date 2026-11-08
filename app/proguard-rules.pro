# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

-keep public class android.** { *; }
# Dhizuku API/AIDL library classes keep their original package
-keep class com.rosan.dhizuku.** { *; }
-keep public class com.rosan.dhizuku.api.Dhizuku { *; }
-keep public class com.rosan.dhizuku.shared.DhizukuVariables { *; }
-keep public class com.EdS.DhizukuF.App { *; }
-keep public class com.EdS.DhizukuF.ui.activity.** extends android.app.Activity

-keep class rikka.shizuku.ShizukuProvider

-dontwarn **
