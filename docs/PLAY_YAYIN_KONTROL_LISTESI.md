# Play Yayın Kontrol Listesi — Korb

Bu liste, imzalı AAB'yi Play Console'a yüklemek için gereken adımları ve
otomatik kapıları tek yerde toplar. Kod tarafı hazır; aşağıdaki dış hesaplar
ve mağaza formları tamamlanmadan yayın başlatılmamalıdır.

## 1. Otomatik kapılar (her sürümde)

```bash
# Birim testleri + lint + derleme
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug

# API 36 / 16 KB emülatöründe cihaz testleri
./gradlew :app:connectedDebugAndroidTest

# İmzalı APK + Play AAB + R8 mapping
./gradlew :app:assembleRelease :app:bundleRelease
```

CI'daki `verify` işi birim testi, lint ve debug derlemesini; `instrumented`
işi API 36 16 KB emülatöründe migration, uygulama kilidi, yedek geri yükleme
ve Compose yerleşim testlerini çalıştırır. `Android Release APK` işi ise önce
aynı test/lint kapısını geçer, sonra imzalı APK + AAB üretir; APK imzasını,
16 KB hizasını ve AAB imzasını doğrular.

Boyut (2.4): AAB 18,4 MB; arm64 telefonda Play indirmesi ~9,5 MB. Ayrıntı:
`docs/DEVICE_SUPPORT.md`.

Artefaktlar:

- `app/build/outputs/bundle/release/app-release.aab` — Play Console'a yüklenecek dosya.
- `app/build/outputs/mapping/release/mapping.txt` — R8/Play deobfuscation için sakla.
- `app/build/outputs/apk/release/app-release.apk` — yalnızca doğrudan APK dağıtımı için.

## 2. Yayın öncesi zorunlu dış adımlar

- [ ] **Gizlilik politikası URL'si** HTTPS olarak yayında. `.github/workflows/pages.yml`
      `docs/legal` klasörünü GitHub Pages'e yayınlar. Pages kaynağı **GitHub Actions**
      seçildikten sonra beklenen adres:
      `https://oguzkaanur-design.github.io/shoppingappdeneme/privacy.html`.
      Bu URL Play Console'a ve mağaza listelemeye gir.
- [ ] **Play Console uygulaması** oluşturuldu; paket adı `com.shoppingapp`.
- [ ] **App Signing**: Play App Signing açık; yükleme anahtarı `keystore/` +
      `keystore.properties` güvenli yedekte. Bu dosyalar kaybolursa güncelleme
      yayınlanamaz.
- [ ] **Data Safety formu**: veri toplanmıyor; barkod sorgusu için ürün kodu
      Open Food Facts'e, broşür sorgusu için market adı Marktguru'ya gidiyor.
      Yerel veri cihazda kalıyor; uygulama içi yedek kullanıcıya ait.
- [ ] **İçerik derecelendirmesi** ve **hedef kitle** formları dolduruldu.
- [ ] **Mağaza girişi**: uygulama adı, kısa/uzun açıklama, 11 dilde çeviri,
      ekran görüntüleri (`screenshots/`), 512×512 ikon (`store/play-icon-512.png`)
      ve öne çıkan grafik (`store/feature-graphic-1024x500.png`). Metinler:
      `store/PLAY_LISTING.md`.
- [ ] **Hesap gereksinimi**: uygulama hesapsız çalışır; Google ile giriş **isteğe bağlı**.
  Google girişi açılacaksa Play Console → App content → "Account deletion" ve
  Data Safety formunda hesap verisi (e-posta, görünen ad) ve Drive senkronizasyonu
  beyan edilmelidir. Giriş yapılandırılmadıysa düğme "yapılandırılmadı" durumunda kalır.
- [ ] **Google ile giriş (opsiyonel)**: Google Cloud'da Web OAuth istemcisi +
  Android istemcisi (paket `com.shoppingapp`, yükleme ve Play App Signing SHA-1),
  onay ekranında `drive.appdata` kapsamı. Kimlik yalnızca `googleWebClientId`
  özelliğiyle verilir; APK/AAB içine gömülü gizli anahtar yoktur.
- [ ] **Para birimi/ülke**: onboarding ülke seçimi para birimini belirler; mağaza
  metinlerinde 11 dil ve çoklu para birimi desteği belirtildi.
- [ ] **Reklamlar**: uygulamada reklam yok.
- [ ] **İzin beyanları**: kamera (barkod/fiş), bildirim (hatırlatıcı).
- [ ] **Premium lisans anahtarı** (`license/baskit-license-private.pem`) yalnızca
      çevrimdışı yedekte; depoda ve CI'da yok. `.pem`/`.jks`/`keystore.properties`
      izleniyorsa CI `Guard signing and licence secrets` adımı kırmızıya döner.
- [ ] **Ülke/bölge** seçimi ve fiyatlandırma (ücretsiz) yapıldı.

## 3. Sürüm numarası ve yükleme

1. `versionCode`'u Play'deki son yüklemeden **büyük** yap:
   `./gradlew :app:bundleRelease -PversionCode=15 -PversionName=2.4`.
2. AAB'yi Play Console → **Internal testing** kanalına yükle.
3. Play'in otomatik ön kontrolünü geç; uyarıları gidermeden production'a çıkma.
4. Internal testing'de en az bir temiz kurulum ve bir yükseltme kurulumu dene:
   liste ekleme, tamamlama, yedek al/yükle, dil değiştir, uygulama kilidi.
5. Aşamalı dağıtım önerilir: %5 → %20 → %50 → %100; her adımda Play Console
   Android vitals ve çökme oranını kontrol et.
6. Rapor ekranından bir PDF üretip paylaş; PDF'in açıldığını ve tabloların
   güncel bakiyeyi/Pfand'ı gösterdiğini doğrula.
7. Premium lisans özel anahtarının (`license/baskit-license-private.pem`)
   çevrimdışı yedeğini al; kaybolursa yeni lisans üretilemez.
6. `mapping.txt`'yi bu sürüm için sakla; R8 yığın izleri ancak bununla çözülür.

## 4. Yayın sonrası

- [ ] Play Console → Android vitals: ANR ve çökme oranı eşiğin altında.
- [ ] Yeni sürümde migration testi ve yedek geri yükleme testi yeşil.
- [ ] Acil geri çekme: dağıtımı durdur; düzeltmeyi **daha yüksek versionCode**
      ile yükle. Play yayınlanmış bir sürümü geri alamaz.
- [ ] Kullanıcı geri bildirimi ve gizlilik talepleri için iletişim adresi takip ediliyor.

## 5. Bilinen açık işler (yayını engellemeyen)

- Merkezî çökme raporlama (Crashlytics/Sentry) hesabı yok; cihaz içi
  `CrashLog` ve Play vitals kullanılıyor.
- Yedek JSON'u isteğe bağlı parola ile şifrelenmiyor; gizlilik metninde açıkça yazıyor.
- Fuzz/pentest ve LeakCanary profili ayrı bir güvenlik/performans turu.
- iOS/App Store bu deponun kapsamı dışında; ayrı bir SwiftUI projesi gerekir
  (bkz. `docs/DEVICE_SUPPORT.md`). Android minSdk 26 olduğu için S4 mini
  (Android 4.4) desteklenmez.

## 6. İlk yüklemeden sonra yapılacak tek kod değişikliği

Play App Signing, yüklediğiniz AAB'yi Google'ın anahtarıyla yeniden imzalar. Premium
kapısının Play'den kurulan sürümlerde de çalışması için:

1. Play Console → **Test and release → Setup → App integrity → App signing**.
2. "App signing key certificate" altındaki **SHA-256** değerini kopyalayın.
3. `app/src/main/java/com/shoppingapp/security/AppIntegrity.kt` içindeki
   `PLAY_APP_SIGNING_CERT_SHA256` sabitine yazın (iki nokta işaretleri olmadan da olur).
4. Yeni bir `versionCode` ile tekrar yükleyin.

Bu adım atlanırsa uygulama sorunsuz çalışır; yalnızca premium lisansı etkinleştirme
"bu kurulumda kullanılamaz" durumunda kalır.
