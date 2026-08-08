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

# Retrofit and Gson inspect these models and endpoint signatures at runtime.
-keepattributes Signature,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault
-keep interface app.khom.pavlo.crypto.model.network.** { *; }
-keep class app.khom.pavlo.crypto.model.AllCoinsResponse { *; }
-keep class app.khom.pavlo.crypto.model.InfoCoin { *; }
-keep class app.khom.pavlo.crypto.model.RawCoin { *; }
-keep class app.khom.pavlo.crypto.model.DisplayCoin { *; }
-keep class app.khom.pavlo.crypto.model.HistoData { *; }
-keep class app.khom.pavlo.crypto.model.PairData { *; }
-keep class app.khom.pavlo.crypto.model.TopCoinData { *; }
-keep class app.khom.pavlo.crypto.ui.news.NewsItem { *; }
