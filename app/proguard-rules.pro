# Keep the serialisable API models intact for kotlinx.serialization.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class br.com.loyfus.data.** {
    *** Companion;
}
-keepclasseswithmembers class br.com.loyfus.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
