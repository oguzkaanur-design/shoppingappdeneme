# R8 yapılandırması: kod karıştırılır, yalnızca yansıma (reflection) ile erişilen
# sınıflar korunur. Room/Hilt/Compose/Retrofit kendi kurallarını getirir.

# Room varlıkları SQLite'tan yansımayla okunur.
-keep class com.shoppingapp.data.model.** { *; }
-keepclassmembers class com.shoppingapp.data.model.** { *; }

# Gson ile ayrıştırılan prospektüs/Open Food Facts modelleri.
-keep class com.shoppingapp.data.offers.** { *; }
-keep class com.shoppingapp.data.remote.** { *; }

# Yedek JSON'u Gson ile alan adları üzerinden yazılır/okunur. Bu sınıf
# karıştırılırsa yedek anahtarları sürümden sürüme değişir ve eski yedekler açılmaz.
-keep class com.shoppingapp.data.backup.** { *; }
-keepclassmembers class com.shoppingapp.data.backup.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Enum sabitleri: veritabanında ve JSON'da adlarıyla saklanır.
-keepclassmembers enum * { *; }

# Sürüm bilgisi ve yedek şeması yansımayla okunabilir.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# R8 full mode ile Room/Hilt uyarılarını azalt.
-dontwarn javax.annotation.**
-dontwarn kotlinx.**

# Serbest bırakma derlemesinde hata ayıklama günlükleri APK'dan tamamen çıkarılır.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
