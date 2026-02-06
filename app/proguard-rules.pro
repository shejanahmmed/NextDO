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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep all model classes
-keep class com.shejan.nextdo.Task { *; }
-keep class com.shejan.nextdo.TaskDao { *; }
-keep class com.shejan.nextdo.AppDatabase { *; }

# Keep BroadcastReceivers
-keep class com.shejan.nextdo.ReminderBroadcastReceiver { *; }
-keep class com.shejan.nextdo.SnoozeReceiver { *; }
-keep class com.shejan.nextdo.BootCompletedReceiver { *; }

# Keep Widget Providers
-keep class com.shejan.nextdo.UpcomingTasksWidgetProvider { *; }
-keep class com.shejan.nextdo.UpcomingTasksLightWidgetProvider { *; }

# Lifecycle
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}
