# Cihaz desteği ve mağaza kapsamı

## Android

- **Desteklenen sürüm:** Android 8.0 (API 26) ve üzeri.
- **Mimari:** ARMv7/ARM64/x86/x86_64. Uygulama kodu saf Kotlin; tek native
  bileşen ML Kit barkod okuyucudur (`libbarhopper_v3.so`). Play, AAB'den cihaza
  yalnızca o cihazın mimarisini indirir.
- **Hedef:** Android 16 (API 36), 16 KB sayfa boyutlu cihazlara hazır.
- **Ekran:** Telefon, katlanabilir, 7"/10" tablet ve yatay mod. Tüm ekranlar
  kaydırılabilir; edge-to-edge insetleri tüketilir; minimum genişlikte tablolar
  sütun ağırlıklarıyla daralır, metinler kısaltılır.

### Neden S4 mini (Android 4.4 / API 19) desteklenmiyor?

Uygulama Jetpack Compose, CameraX, WorkManager ve AndroidX Biometric kullanıyor.
Bu bileşenlerin en düşük desteklediği sürüm API 21'dir; güvenlik ve mağaza
gereksinimleri nedeniyle proje **API 26** taban alır. Android 4.4 (S4 mini'nin
özgün sürümü) ne Compose'u ne de CameraX'i çalıştırabilir. API 19 desteği için
uygulamanın görünüm katmanının baştan yazılması gerekir; bu, mevcut kod tabanında
gerçekçi değildir. Android 8+ dünyadaki aktif cihazların büyük bölümünü kapsar.

### Boyut (imzalı sürüm, 2.4)

| Ölçüm | Değer |
|---|---|
| `app-release.aab` (Play'e yüklenen) | 18,4 MB |
| Evrensel `app-release.apk` (dört mimari) | 25,7 MB |
| arm64 telefonda Play indirmesi | ~9,5 MB |
| armeabi-v7a telefonda Play indirmesi | ~7,9 MB |

Play, AAB'yi bölerek yalnızca gereken ABI'yi, dili ve yoğunluğu gönderir. Doğrudan
APK dağıtırsanız (GitHub Release) evrensel APK iner; bu yüzden mağaza sürümü
tercih edilmelidir.

### Test edilen yapılandırmalar

| Yapılandırma | Sonuç |
|---|---|
| API 36, 1080×2400, 16 KB sayfa (S24 sınıfı) | Derleme, kurulum, tüm ekranlar |
| API 36, küçük ekran 540×960 (S4 mini fiziksel boyutu) | Yerleşim kaydırma/taşma kontrolü |
| API 36, tablet 1600×2560 | Yerleşim/ızgara kontrolü |
| Yatay mod | Liste ve kompakt tamamla eylemi |
| Koyu/açık tema + 4 arayüz stili | Kiler, Cebim, Prospekt, Rapor |

> Fiziksel cihaz testi bu ortamda yapılamadı; sonuçlar API 36 emülatöründedir.
> Yayın öncesi en az bir Samsung ve bir Pixel fiziksel cihazda duman testi yapın.

## iOS / App Store

**Bu depo bir Android uygulamasıdır ve iPhone'a kurulamaz.** App Store'a yüklemek
için ayrı bir iOS uygulaması gerekir:

- SwiftUI (veya Kotlin Multiplatform/Flutter) ile yeni bir Xcode projesi,
- Apple Developer Program üyeliği (yıllık ücret), Mac + Xcode,
- App Store Connect kaydı, gizlilik beyanı ve 6,7"/6,5"/5,5" ekran görüntüleri,
- iPhone 6 (iOS 12) artık güncel Xcode ile derlenemez; yeni uygulama en az iOS 16
  hedefler. Bu nedenle iPhone 6 desteği gerçekçi değildir; iPhone 15 ve sonrası
  hedeflenmelidir.

Bu turda iOS projesi **oluşturulmadı**: derlenemeyen/çalışmayan bir iskelet
yayınlamak yanıltıcı olurdu. Aynı özellik setini iOS'ta sunmak ayrı bir proje
kalemidir; veri modeli ve ekran akışı bu depodaki Kotlin kodundan taşınabilir.
