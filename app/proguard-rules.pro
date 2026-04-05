-keep class com.ftt.signal.JsBridge { *; }
-keepclassmembers class com.ftt.signal.JsBridge {
    @android.webkit.JavascriptInterface <methods>;
}
