# Keep the serialisable API models intact for kotlinx.serialization.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class br.com.lote.data.** {
    *** Companion;
}
-keepclasseswithmembers class br.com.lote.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
