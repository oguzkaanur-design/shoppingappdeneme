# Cloud / fresh-thread handoff — Einkauf (shoppingapp)

Bu dosya, sohbet geçmişi taşınmadan (yeni thread veya Freebuff Cloud) devam etmek için kısa özet.
Proje kökü: bu klasör. Uygulama adı **Einkauf**, paket `com.shoppingapp`.

## Durum

- Android uygulaması çalışıyor; telefonda `versionCode 3 / versionName 1.2` kurulu, veriler korunuyor.
- Stack: Kotlin + Jetpack Compose (Material3), Hilt, Room (v4), CameraX + ML Kit barkod, Retrofit (Open Food Facts), Coil.
- Sürümler: AGP 8.3.0, Kotlin 1.9.22, Gradle 8.14.5, JDK 17, compileSdk/targetSdk 34, minSdk 26.
- Kalan işler: görsel tema yenilemesi (renk/derinlik/tipografi), gerçek barkodla tarama testi.

## Derleme

```bash
# SDK yolu (bulutta local.properties veya ANDROID_HOME ile verilmeli)
export ANDROID_HOME=/opt/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

./gradlew :app:assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease      # app/build/outputs/apk/release/app-release.apk
```

Bulutta SDK yoksa (yaklaşık 1.5–2 GB indirme):

```bash
mkdir -p $ANDROID_HOME/cmdline-tools && cd $ANDROID_HOME/cmdline-tools
curl -LO https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q commandlinetools-linux-*.zip && mv cmdline-tools latest && cd ../..
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses > /dev/null
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

## İmza (çok önemli)

- `keystore/shoppingapp-release.jks` + `keystore.properties` proje içinde ve **git'e girmez** (`.gitignore`).
- `keystore.properties` anahtarları: `storeFile`, `storePassword`, `keyAlias`, `keyPassword`. Değerleri sohbete yazma.
- Aynı anahtar + aynı `applicationId` + **artan** `versionCode` = telefonda güncelleme, veriler korunur.
- Her sürümde `app/build.gradle.kts` içindeki `versionCode` artırılmalı.
- Dosyalar taşınmazsa release imzasız kalır (build.gradle bunu tolere eder) ve telefona güncelleme kurulamaz.

## Telefona kurulum

Bulut makinesi kullanıcının Wi-Fi'sindeki telefona `adb` ile erişemez.
Akış: APK'yı telefona indir → üzerine kur. (Yerel makinede `adb -s <ip>:<port> install -r <apk>` çalışıyordu.)

## Bilinen notlar

- Room: `fallbackToDestructiveMigration()` **kaldırıldı**. Şema değiştirirken `AppMigrations`
  içine migration ekle ve `ShoppingDatabase.version`'u artır; eksik geçiş veriyi silmez,
  uygulama açılışta `IllegalStateException` ile durur (bilinçli: sessiz veri kaybından iyidir).
- Şemalar `app/schemas/com.shoppingapp.data.local.ShoppingDatabase/` altında (git'e girer).
  Yeni sürüm şemasını üretmek için sürümü artırıp `./gradlew :app:assembleDebug` çalıştır.
- Geçiş testleri: `app/src/androidTest/java/com/shoppingapp/data/local/MigrationTest.kt`
  (veri korunması + geçişsiz sürümün hata vermesi). Çalıştırmak için cihaz/emülatör gerekir:
  `./gradlew :app:connectedDebugAndroidTest`.
- Örnek/demo ürünler sadece boş veritabanına yazılıyor; temizlemek için liste ekranı ⋮ → "Alışveriş verilerini sıfırla".
- Git: tek commit var, remote yok, çoğu dosya untracked (`git config user.name` = "Claude Sonnet 4.5"). Push için önce kimlik/hesap bağlanmalı.
- 1.7 GB toplam boyutun ~460 MB'ı `app/build`, 16 MB'ı `screenshots` — klona/taşımaya gerekmez.

## Yedek plan: GitHub Actions ile APK (telefondan, bulut IDE'si olmadan)

Bulut taşıma başarısız olursa veya PC kapalıyken APK gerekiyorsa kullanılan akış.
Hazır dosya: `.github/workflows/android-release.yml`

Tek seferlik kurulum (PC açıkken):

1. Repoyu çalışır hale getir: tüm kaynak dosyalar commit edilmeli (`git status` temiz olmalı).
   Untracked dosyalar GitHub'a gitmezse CI eksik kodla derler.
2. Boş bir GitHub deposuna gönder: `git remote add origin <url> && git push -u origin master`.
3. `bash scripts/keystore-secret-olustur.sh` çalıştır → `keystore-base64.txt` üretilir.
4. GitHub → Settings → Secrets and variables → Actions → 4 secret ekle:
   `KEYSTORE_BASE64` (üretilen dosyanın içeriği), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
   (son üçü `keystore.properties` içindeki değerler).

Her sürümde (telefondan yapılabilir):

5. Actions → "Android Release APK" → Run workflow.
   `versionCode` ARTIRILMALI (örn. 4), `versionName` örn. 1.3, `releaseTag` örn. v1.3.
6. Release sayfasındaki `.apk` dosyasını telefondan indir → üzerine kur. Veriler korunur.

Notlar:

- İmza anahtarı repoya girmez; sadece GitHub secret olarak durur. Secret yoksa CI hata verir
  (imzasız release APK telefona "güncelleme" olarak kurulamaz).
- `versionCode` artmazsa telefon paketi güncelleme olarak kabul etmez.
- Sürüm numarası kod düzenlemeden verilebilir: `./gradlew :app:assembleRelease -PversionCode=4 -PversionName=1.3`
- Artifact indirmesi zip verir; Release asset doğrudan `.apk` dosyasıdır.
