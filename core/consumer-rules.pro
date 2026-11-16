# Keep public manifest entry points and serialization models for R8/release builds
-keep class com.gothwad.accounts.core.AccountCallbackActivity { *; }
-keep class com.gothwad.accounts.core.broker.AccountDirectoryProvider { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class com.gothwad.accounts.core.** {
    @kotlinx.serialization.Serializable *;
}
