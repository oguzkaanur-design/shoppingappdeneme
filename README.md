# Korb

Kişisel market, kiler ve bütçe uygulaması. Çevrimdışı öncelikli; veriler
Room/SQLite'ta cihazda tutulur. Kotlin + Jetpack Compose + Hilt.

- **İlk açılış akışı:** dil → ülke/para birimi → hesap → interaktif tanıtım turu
- Alışveriş listesi, market modu, barkod ve fiş tarama
- Kiler (stok, kritik eşik, son kullanma), Pfand iadesi
- Cebim: nakit/banka hareketleri, bütçe, istatistikler
- Prospektüsler ve fırsat listesi
- **Rapor ekranı:** hareket, Pfand ve kiler tabloları + çevrimdışı **PDF** dışa aktarma
- 5 renk paleti × 4 arayüz stili (Modern, Klasik, Kompakt, Vitrin)
- 11 dil, isteğe bağlı PIN/biyometrik uygulama kilidi

## Para birimi

Onboarding'de seçilen ülke para birimini belirler (Almanya → €, Türkiye → ₺,
İngiltere → £ …); ayarlardan değiştirilebilir. Tüm tutarlar `money/MoneyFormat`
(`money/Currency.kt`) üzerinden biçimlendirilir: `1.234,56 €`, `34,90 ₺`, `$1,234.56`.
Giriş alanları hem `12,50` hem `12.50` yazımını kabul eder.

## Hesaplar ve senkronizasyon

Uygulama hesapsız tam çalışır. İsteğe bağlı iki hesap türü var:

- **Google ile giriş:** yedekler kullanıcının kendi Google Drive'ındaki
  `appDataFolder` (uygulamaya özel gizli klasör) içinde tutulur. Sunucu yoktur;
  senkronizasyon `sync/SyncManager` içinde "daha yeni `createdAt` kazanır"
  kuralıyla yapılır. Gerekli izin: `drive.appdata`.
- **Yerel hesap:** e-posta + şifre yalnızca cihazda; şifre PBKDF2-SHA256
  (120 000 tur) ile özetlenir, kendisi saklanmaz.

Google girişini açmak için Google Cloud'da bir **Web** OAuth istemcisi oluşturup
kimliğini verin (ANAHTAR REPOYA GİRMEZ):

```properties
# local.properties veya gradle.properties
googleWebClientId=1234567890-abc.apps.googleusercontent.com
```

İstemci kimliği boşsa giriş düğmesi "yapılandırılmadı" durumunda kalır; uygulama
hesapsız çalışmaya devam eder. Android istemci kimliği (paket + imza SHA-1) ve
OAuth onay ekranında `drive.appdata` kapsamı da tanımlanmalıdır.

GitHub Actions'ta üretilen APK'lar için aynı değer depo **variable**'ı olarak durur
(Secrets değil; değer APK içinde zaten görünür):
`Settings → Secrets and variables → Actions → Variables → GOOGLE_WEB_CLIENT_ID`.

## Güvenlik

- Release derlemesi R8 ile küçültülür/karıştırılır. İmza özeti tanınmayan
  (klonlanmış) bir APK çalışır ama premium açılmaz; kullanıcı verisi silinmez.
- Play'den kurulan sürümler Google tarafından yeniden imzalanır: ilk AAB
  yüklemesinden sonra Play Console'daki App Signing SHA-256 değerini
  `AppIntegrity.PLAY_APP_SIGNING_CERT_SHA256` sabitine yazın.
- Premium, **ECDSA P-256 ile imzalanmış çevrimdışı lisansla** açılır; yerel bir
  `true` bayrağı yoktur (`security/PremiumLicense.kt`, `security/PremiumGate.kt`).
- Lisans özel anahtarı depoda değildir: `license/` (bkz. `license/README.md`).
- Uygulamada gömülü API anahtarı/parola yok; ağ yalnızca HTTPS ve sabit alan adları.

## Gereksinimler

- JDK 17
- Android SDK 36 (compileSdk/targetSdk 36, **minSdk 26**)

## Geliştirme

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

## Cihaz testleri

API 36 / 16 KB sayfa boyutlu emülatörde:

```bash
./gradlew :app:connectedDebugAndroidTest
```

## Yayın

```bash
# İmzalı APK + Play AAB + R8 mapping
./gradlew :app:assembleRelease :app:bundleRelease
```

Çıktılar:

- `app/build/outputs/bundle/release/app-release.aab` — Play Console
- `app/build/outputs/apk/release/app-release.apk` — doğrudan kurulum
- `app/build/outputs/mapping/release/mapping.txt` — R8/Play deobfuscation

İmzalı derleme için `keystore.properties` ve `keystore/` gerekir; bu dosyalar
repoya girmez. GitHub Actions'ta `KEYSTORE_*` secret'ları kullanılır.

Mağaza görselleri: `store/play-icon-512.png`, `store/feature-graphic-1024x500.png`,
uygulama içi ekran görüntüleri `screenshots/` altında.

Yayın adımları: [docs/PLAY_YAYIN_KONTROL_LISTESI.md](docs/PLAY_YAYIN_KONTROL_LISTESI.md).
Cihaz/iOS kapsamı: [docs/DEVICE_SUPPORT.md](docs/DEVICE_SUPPORT.md).

## Dokümantasyon

- [PROJE_NOTLARI.md](PROJE_NOTLARI.md) — geliştirme günlüğü
- [docs/PRODUCTION_READINESS.md](docs/PRODUCTION_READINESS.md) — üretim denetimi
- [store/PLAY_LISTING.md](store/PLAY_LISTING.md) — mağaza metinleri ve form yanıtları
- [docs/legal/](docs/legal/) — gizlilik ve kullanım şartları
