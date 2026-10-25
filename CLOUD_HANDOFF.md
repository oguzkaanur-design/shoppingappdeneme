# Cloud / fresh-thread handoff — Einkauf (shoppingapp)

Bu dosya, sohbet geçmişi taşınmadan (yeni thread veya Freebuff Cloud) devam etmek için kısa özet.
Proje kökü: bu klasör. Uygulama adı **Einkauf**, paket `com.shoppingapp`.

## Durum

- Android uygulaması çalışıyor; telefonda `versionCode 10 / versionName 1.9` kurulu, veriler korunuyor.
- Stack: Kotlin + Jetpack Compose (Material3), Hilt, Room (v6), CameraX + ML Kit barkod, Retrofit (Open Food Facts), Coil, OkHttp.
- Sürümler: AGP 8.13.1, Kotlin 2.2.10, Gradle 8.14.5, JDK 17, compileSdk/targetSdk 36, minSdk 26.
- Release APK: versionCode 14 / versionName 2.3, 16 KB sayfa hizalı, Play hedefi API 36.
- Kalan işler: görsel tema yenilemesi (renk/derinlik/tipografi), gerçek barkodla tarama testi.

## Prospektüsler (v1.6)

- Marketlerin kendi siteleri uygulama içi WebView'de boş kaldığı için (REWE/EDEKA/ALDI 403,
  JS koruması) WebView kaldırıldı. Broşürler artık sayfa görseli olarak gösteriliyor.
- Kaynak: `https://www.marktguru.de/rp/<market>-prospekte` sayfasındaki gömülü JSON
  (`data/offers/FlyerCatalogParser.kt`) ve CDN sayfaları
  `https://cdn.marktguru.de/api/v1/leaflets/<id>/images/pages/<n>/xlarge.webp`.
- `FlyerRepository` günlük `?cb=` parametresi + `Cache-Control: no-cache` kullanır; aynı adres
  bazen küçük bir bölgesel sürüm döndürdüğü için tam broşür gelene kadar en fazla 3 istek yapıp
  sonuçları birleştirir.
- `FlyerSelection` bu haftanın en kapsamlı broşürünü + gelecek haftanın en kapsamlı broşürünü
  gösterir; hiçbiri yoksa en son broşüre düşer.
- Okuyucu: `FlyerReader.kt` — sayfalar arası kaydırma, çift dokunarak/iki parmakla yakınlaştırma,
  komşu sayfaları önceden yükleme. Testler: `FlyerCatalogParserTest`, `FlyerSelectionTest`,
  `FlyerPageUrlsTest`, `FlyerCatalogUrlTest`, `FlyerDateLabelTest`.
- Not: uygulama içi broşür görüntüleme kişisel kullanım içindir; marktguru atfı ekranda duruyor.

## Tıklanabilir prospektüs + Fırsatlar (v1.7)

- Okuyucuda sayfa başına teklifler `api.marktguru.de/api/v1/offers?as=mobiledetailed&leafletId=…&pageIndex=…`
  üzerinden alınır; ürün alanları (`coordinates`) aynı leaflet detayından okunur. API anahtarları
  uygulamaya gömülmez, `marktguru.de` ana sayfasındaki `config` bloğundan okunur (`MarktguruConfigProvider`).
- `FlyerReader` ürün alanlarına yeşil ＋ işareti çizer. Dokunuş, sayfanın o anki yakınlaştırma/kaydırma
  durumuna göre normalize koordinata çevrilip ürüne eşleştirilir (`offerAt`, test: `FlyerReaderTapTest`).
  İşaretler kendi clickable katmanını kullanmaz: üstteki çift dokunma dedektörü dokunuşu tüketiyor.
- Onay sayfası (`FlyerOfferSheet`) ad, açıklama, fiyat, son geçerlilik tarihi ve adet gösterir;
  **Dialog** olarak çizilir çünkü okuyucu da bir Dialog ve ModalBottomSheet onun arkasında kalıyordu.
- Onaylanınca ürün `flyer_list_items` tablosuna (Room v5, migration 4→5) yazılır: market, ad, fiyat,
  adet, geçerlilik aralığı ve sayfadan kırpılan ürün fotoğrafı (`FlyerImageStore`).
- Alt menüdeki **Fırsatlar** sekmesi (`FlyerListScreen`) geçerli ve süresi dolmuş ürünleri ayırır,
  "24 Eki tarihine kadar" etiketiyle fiyat/adet/toplam gösterir; aynı fırsat tekrar eklenirse adet artar.
- Birim testleri: `FlyerOfferParserTest`, `MarktguruConfigParserTest`, `FlyerCropTest`, `FlyerReaderTapTest`.

## Cebim / birikim (v1.8)

- Alt menüye **Cebim** sekmesi eklendi (`feature/money/SavingsScreen.kt`); yer açmak için
  "İstatistik" sekmesi kaldırıldı, o ekran liste üst barındaki grafik düğmesinden açılıyor.
- İki hesap var: **Nakit** ve **Banka**. Her kartta `−` ve `+` düğmeleri bir diyalog açar:
  tutar (virgül/nokta kabul edilir), isteğe bağlı not ve "işlemden sonra bakiye" önizlemesi.
- Bakiye ayrı kolonda tutulmaz: `money_entries` tablosundaki işaretli hareketlerin toplamıdır
  (Room v6, migration 5→6). Böylece her işlem geçmişte görünür ve tek tek silinebilir.
- Toplam = nakit + banka + **iade bekleyen Pfand**. Pfand ekranından eklenen her kayıt
  Cebim toplamına otomatik yansır; Pfand kartı Pfand ekranına götürür.
- Pfand ekranındaki **İadeyi al** artık marketin ödediği tutarı sorar (`ShoppingDao.cashInPfand`).
  Tutara tam denk gelen adetler `PfandReturnPlanner` ile seçilir, yalnızca o adetler düşülür,
  kalan kayıtlar korunur ve ödenen tutar "Pfand iadesi" notuyla nakit bakiyesine yazılır.
  Tüm işlem tek Room transaction'ındadır; tutar tam eşleşmezse hiçbir kayıt değişmez, böylece
  para iki kez sayılmaz veya kaybolmaz.
- Saf mantık: `feature/money/MoneySummary.kt` (+ `MoneySummaryTest`); geçiş testi:
  `MigrationTest.v5tenV6eGecisVeriyiKorurVeCebimTablosunuEkler`.

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
- Git remote: `https://github.com/oguzkaanur-design/shoppingappdeneme`; `main` dalı ve `v*` etiketleri
  `.github/workflows/android-release.yml` ile imzalı APK Release'i üretir (v1.7, v1.8 böyle yayınlandı).
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

## v2.0: Kiler, reyon sırası, yedekleme, grafikler, hatırlatıcılar

- **Kiler sekmesi** (`feature/pantry/`): `pantry_items` artık kullanılıyor. Kritik eşik altına düşen
  ürün "Kritik stok" bölümünde; tek dokunuşla alışveriş listesine ekleniyor, toplu ekleme düğmesi var.
  Son kullanma tarihi 3 gün kala uyarı verir. Alışveriş tamamlanınca kilerdeki stok satın alınan
  adet kadar **kendiliğinden artar** (`ShoppingDao.increasePantryQuantity`). Saf kurallar:
  `PantrySummary.kt` (+ `PantrySummaryTest`).
- **Reyon sıralaması** (`data/aisle/AisleOrder.kt`): `Store.aisleOrderJson` artık 9 market için
  gerçek kategori sırası taşıyor. Market çipi seçilince liste "Reyon n" başlıklarıyla gruplanır;
  numara marketin sırasındaki gerçek konumdur. Ayrıştırma Gson ile (JVM test edilebilir).
- **Yedekleme** (`data/backup/BackupManager.kt`, `feature/backup/`): tüm tablolar tek JSON'da.
  SAF ile dışa/içe aktarma, her açılışta günde bir kez yerel yedek (`files/backups/auto-*.json`,
  son 5 saklanır), yerel yedeği geri yükleme. Geri yükleme tek Room transaction'ı.
  Görseller yedeğe girmez (yalnızca yolları).
- **İstatistik** (`feature/stats/SpendingAnalytics.kt`): haftalık/aylık harcama sütun grafiği
  (Compose Canvas, ek kütüphane yok), kategori dağılımı, market karşılaştırması (ortalama sepet),
  ürün fiyat trendi (son iki alışveriş). Veri `purchase_history_items` + `purchase_history` join'i.
- **Hatırlatıcılar** (`notifications/`): WorkManager günlük işi (18:00): süresi 2 günden az kalan
  fırsatlar, kritik kiler stoğu, cumartesi boş liste hatırlatması. Ayar ve bildirim izni
  Yedekleme ekranında. Kanal `einkauf_reminders`, günde bir kez bildirim. Karar mantığı
  `ReminderPlanner.kt` içinde saf fonksiyon (+ `ReminderPlannerTest`); "Bildirimleri şimdi dene"
  düğmesi aynı planlayıcıyı çalıştırıp bildirimi anında gösterir.
- **Günlük kullanım**: ana ekran widget'ı (alınacak sayısı + barkod tara kısayolu,
  `widget/ShoppingWidgetProvider`), **market modu** (büyük satırlar, satıra dokununca işaretle),
  listeyi metin olarak paylaşma (`feature/share/ListShare.kt`), "Sık alınanlar" şeridi ve
  "Son alışverişi tekrarla". Pfand sekmesi kaldırıldı; Cebim ekranındaki Pfand kartından açılıyor.
- **Dayanıklılık**: broşür listesi cihazda önbelleklenir (`FlyerCache`); ağ hatasında boş ekran
  yerine "Son kayıtlı liste" bandı görünür. Yeni metinlerin bir kısmı `strings.xml`'e taşındı
  (alt menü, widget, yedekleme başlığı); ekran içi metinlerin çoğu hâlâ kodda.
- **Sürüm**: versionCode 11 / versionName 2.0. Şema değişmedi (Room v6 kalıyor), migration gerekmedi.
- **Doğrulama**: `:app:testDebugUnitTest` 95 test; emülatörde (API 37) Kiler ekleme → listeye ekleme,
  Aldi Süd reyon başlığı, market modu, alışveriş tamamlama (€3,09) → kiler stoğu 1→2, sık alınanlar,
  son alışverişi tekrarla, yerel yedek alma/geri yükleme (JSON içeriği doğrulandı), Prospekt
  yükleme + önbellek dosyası, bildirim kanalı ve WorkManager işi kontrol edildi.

## v2.1: fiş OCR, paylaşım içe aktarma, bütçe, barkod fiyat hafızası + güvenlik
- `feature/receipt`: ML Kit OCR (Play Services modeli), `ReceiptTextLayout` iki sütunlu
  fiş satırlarını konuma göre birleştirir, `ReceiptParser` tutar/ürün ayıklar.
  Fiş kaydı `ShoppingRepository.completePurchaseFromReceipt` üzerinden normal
  tamamlama akışını kullanır (geçmiş, fiyat, kiler stoğu).
- `feature/share/ListImport`: paylaşılan metni satırlara ayırır (madde işareti,
  "2 kg un", "3x Milch", virgüllü tek satır). `ACTION_SEND` + önizleme diyaloğu.
- `feature/stats/BudgetStore`: aylık limit DataStore'da; kart + düzenleme diyaloğu.
- Barkod: `ShoppingDao.getLastPriceForProduct` → tarayıcıda "Son: €… · Market · tarih".
- Güvenlik: `security/AppIntegrity` imza özeti kontrolü (sürümde farklı imza → ekran
  kilitli), R8 + kaynak küçültme, `allowBackup=false` + extraction rules, cleartext
  kapalı, Log.d/v/i sürümden çıkarılıyor.
- APK 22 MB; 117 birim testi geçiyor.
- Sonraki iş (v2.3'te tamamlandı): arayüz metinlerini `strings.xml`'e taşıyıp çok dillilik.

## v2.2: targetSdk 36 + bağımlılık yükseltmesi (24 Ekim 2026)
- `compileSdk`/`targetSdk` 36; AGP 8.13.1, Gradle 8.14.5, Kotlin 2.2.10, Compose BOM
  2025.09.01. Kotlin 2.2 + AGP 8.13 minimum Gradle 8.13 istediği için wrapper yükseltildi.
- Room 2.7.2, Hilt 2.57.2, Retrofit 2.11.0, OkHttp 4.12.0, CameraX 1.4.2,
  WorkManager 2.10.2, Coil 2.7.0. Room/Hilt halen kapt ile derleniyor; `jvmTarget 17`.
- Edge-to-edge: `enableEdgeToEdge()`, saydam sistem çubukları; `statusBarColor` ve
  `navigationBarColor` atamaları kaldırıldı, ikon kontrastı `isAppearanceLight*` ile.
  Ana ekran ve tarayıcı inset tüketimi düzeltildi; tarayıcı izin ekranı artık izin
  yokken tarama katmanıyla birlikte çizilmiyor. Açık/koyu temada doğrulandı.
- Doğrulama: 131 birim testi, lint 0 hata; `google_apis_playstore_ps16k` (16 KB sayfa)
  API 36 emülatöründe 5/5 cihaz testi ve imzalı release duman testi (liste, açık/koyu tema,
  tarayıcı). APK `aapt2` ile targetSdk 36, `zipalign -c -P 16 -v 4` başarılı; bu kontrol
  release iş akışında otomatik.

## v2.3: 11 dil + PIN/biyometrik uygulama kilidi (24 Ekim 2026)
- **Yerelleştirme:** arayüz metinleri `strings.xml`'e taşındı; Türkçe, İngilizce (ABD/UK),
  Almanca, Bulgarca, Sırpça, Basitleştirilmiş Çince, Ukraynaca, Rusça, Lehçe, Felemenkçe.
  `bundle.language.enableSplit = false`; API 33+ `LocaleManager.applicationLocales`, eski
  sürümlerde `attachBaseContext` ile locale sarma. Aramalı dil ekranı ve Backup & settings girişi.
- **Uygulama kilidi:** `security/AppLock.kt` 4 haneli PIN'i 16 bayt tuz + PBKDF2-HMAC-SHA256
  (150.000 iterasyon) ile saklar; 5 hatalı denemede 30 sn kilit. `AppLockScreen` geri tuşuyla
  kapanmaz; `AppLockSettingsDialog` etkinleştir/DEĞİŞTİR/kaldır akışını yönetir.
- **Biyometri:** `MainActivity` artık `FragmentActivity`; `BiometricPrompt` (`BIOMETRIC_WEAK`)
  yalnızca kullanıcı kayıtlı parmak izi varsa gösterilir. 3 dakika arka planda kalınca yeniden
  kilitlenir. `AppLockStateViewModel` döndürme/dil değişiminde açık oturumu korur; süreç
  yeniden başlarsa kilit geri gelir. Manifest'e yalnızca `USE_BIOMETRIC` eklendi.
- **Sürüm:** versionCode 14 / versionName 2.3.
- **Doğrulama:** 145 birim testi + 17/17 cihaz testi (API 36 16 KB emülatörü); yanlış PIN,
  doğru PIN, parmak izi kaydı ve biyometrik açılış, yeniden başlatmada kilit ekranı elle doğrulandı.
  Dil değişimi EN → DE ekran görüntüleriyle doğrulandı (`screenshots/api36-language*.png`).
- **Yatay yerleşim:** kısa ekranlarda tarama FAB'ı "Alışverişi tamamla" düğmesini örtüyordu;
  tarama eylemi üst çubuğa taşındı, FAB yalnızca yeterli yükseklikte gösteriliyor. Yatayda filtre
  çipleri, sık alınanlar ve özet kartı listenin başlık öğeleri olarak kaydırılıyor; yukarı kaydırınca
  liste tam yüksekliğe açılıyor ve döndürmede başa dönüyor. Başlık ekrandan çıkınca
  "Alışverişi tamamla" üst çubukta tonal onay simgesi olarak kalıyor. 411 dp yükseklikte iki yönde
  doğrulandı (`screenshots/api36-landscape-home.png`, `screenshots/api36-landscape-scrolled.png`,
  `screenshots/api36-landscape-compact-complete.png`, `screenshots/api36-portrait-home.png`).
  Davranış `ShoppingCompactCompleteTest` ile API 36 16 KB emülatöründe otomatikleştirildi
  (başlık kaydırma, kompakt tamamlama, diyalog ve dikey ⇄ yatay dönüşte başa dönme).

## Yayın hazırlığı (25 Ekim 2026)

- **Play AAB:** `.github/workflows/android-release.yml` artık `bundleRelease` de üretiyor;
  AAB imzası `jarsigner` ile, APK 16 KB hizası `zipalign -P 16` ile doğrulanıyor. R8
  `mapping.txt` artifact'ı her sürümde saklanıyor. Release işi artifact üretmeden önce
  birim testi + lint çalıştırıyor. Yerel `bundleRelease` ve `jarsigner -verify` başarılı.
- **CI:** `.github/workflows/ci.yml` içine API 36 / `google_apis_playstore_ps16k` emülatöründe
  `connectedDebugAndroidTest` çalıştıran `instrumented` işi eklendi.
- **Yedek hata güvenliği:** `BackupValidator` satır alanlarını, tiplerini, pozitif ve tekil
  kimlikleri doğruluyor; `BackupRestoreTest` bozuk/aşırı büyük/gelecek şemalı yedeklerin
  mevcut veriyi silmediğini, geçerli yedeğin fazladan kayıtları temizlediğini kanıtlıyor.
- **Ağ ve kilit hataları:** MockWebServer ile retry/exhaustion/POST testleri; kilit süresi
  dolması ve bozuk PIN özeti testleri.
- **Yayın adımları:** `docs/PLAY_YAYIN_KONTROL_LISTESI.md` (Play Console, Data Safety,
  gizlilik URL'si, AAB yükleme, aşamalı dağıtım, geri çekme).
