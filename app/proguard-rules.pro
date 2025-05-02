# 🔒 Podstawowa ochrona kodu i minimalizacja
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontoptimize

# 🚀 Zachowanie podstawowych komponentów Androida
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Activity { *; }

# ⚠️ Usunięcie ostrzeżeń, które mogą powodować błędy
-dontwarn android.support.**
-dontwarn androidx.**
-dontwarn com.google.android.gms.**
-dontwarn org.intellij.lang.annotations.*

# 🔥 Zachowanie metod refleksyjnych, jeśli aplikacja używa WebView
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
