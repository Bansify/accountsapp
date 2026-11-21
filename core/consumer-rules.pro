# Keep public manifest entry points and serialization models for R8/release builds
-keep class com.bansify.accounts.core.AccountCallbackActivity { *; }
-keep class com.bansify.accounts.core.broker.AccountDirectoryProvider { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class com.bansify.accounts.core.** {
    @kotlinx.serialization.Serializable *;
}
