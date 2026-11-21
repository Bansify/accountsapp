# App-specific ProGuard / R8 rules (library rules are shipped via consumer-rules.pro in :core and :ui)
-keep class com.bansify.accounts.AccountsApplication { *; }
-keep class com.bansify.accounts.MainActivity { *; }
-dontwarn okhttp3.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
