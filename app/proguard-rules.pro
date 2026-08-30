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

-keep class com.movielist.tmdb.network.** { *;}
-keep class com.movielist.tmdb.util.** { *; }
# FavoriteMovie is written to and read back from SharedPreferences as JSON;
# renaming its fields would orphan everything the user has already saved.
-keep class com.movielist.tmdb.data.** { *; }

# Jackson binds the model classes reflectively, and Retrofit reads the generic
# type of each suspend function's return value, so both need to survive R8.
-keepattributes Signature
-keepattributes AnnotationDefault,RuntimeVisibleAnnotations