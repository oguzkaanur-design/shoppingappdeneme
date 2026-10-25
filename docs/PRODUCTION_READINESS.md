# Üretim Hazırlığı Denetimi — Einkauf (com.shoppingapp)

Tarih: 24 Ekim 2026 · Sürüm: 2.3 (versionCode 14) · Kapsam: bu depodaki Android istemcisi

## Önce kapsam: bu depoda sunucu yok

Einkauf **istemci-only, çevrimdışı öncelikli** bir Android uygulamasıdır: Room/SQLite,
Jetpack Compose, Hilt, WorkManager. Depoda backend, REST/GraphQL servisi, kullanıcı
hesabı, oturum/JWT, ödeme, web sitesi veya konteyner altyapısı **bulunmuyor**.
Ağ erişimi yalnızca iki herkese açık kaynağa gidiyor:

- `https://world.openfoodfacts.org` — barkodla ürün adı arama (Retrofit).
- `https://www.marktguru.de` ve CDN'i — prospektüs listesi/sayfa görselleri (OkHttp).

Bu nedenle 69 maddeden sunucu, veritabanı kümesi, kuyruk, konteyner ve izleme
altyapısı isteyenler bu depoda **uygulanabilir değil**. Bunları sahte Docker/K8s/
Terraform dosyalarıyla "tamamlanmış" gibi göstermek yanlış olurdu; aşağıda her biri
nedeniyle birlikte işaretli. İleride senkronizasyon/hesap eklenirse, o maddelerin
sunucu tarafı ayrı bir depoda ve gerçekten uygulanmalıdır.

Lejant: ✅ zaten vardı · 🔧 bu turda eklendi/düzeltildi · ⚠️ açık, karar veya dış hesap
gerekiyor · ➖ bu depo için geçerli değil (gerekçeli)

---

## 1. Uygulama ve kod güvenliği

| # | Madde | Durum | Kanıt / not |
|---|---|---|---|
| 1 | API key & secret isolation | ✅ | Kodda API anahtarı, parola veya token yok. İmza parolaları `keystore.properties` dosyasında, `.gitignore`'da; CI'da GitHub Secrets. Marktguru anahtarı çalışma anında siteden okunur, gömülü sır değildir. |
| 2 | Git cleanup | ✅ | `git log --all -- keystore.properties local.properties .env` boş. İzlenen dosyalarda anahtar/parola taraması yalnızca CI'ın `$KEYSTORE_PASSWORD` değişken adını buldu. |
| 3 | Minimum izinler | ✅ | Yalnızca `CAMERA`, `INTERNET`, `POST_NOTIFICATIONS`; kamera `required=false`. `FileProvider` ve widget alıcısı `exported=false`, `allowBackup=false`, veri çıkarma kuralları tüm alanları dışlıyor. |
| 4 | Server-side enforcement | ➖ | Sunucu yok; tüm veri kullanıcının cihazında ve yalnızca o kullanıcıya ait. Yetki kararı verilecek uzak kaynak yok. |
| 5 | Rate limiting / throttling | ➖ | Giriş/kayıt/API uç noktası yok. Üçüncü taraf GET'lerde sınırlı retry var; bu bir güvenlik hız sınırı değildir. |
| 6 | Input validation | 🔧 | Ürün adı 60, birim 12, ondalık alanlar 7 karakter; miktar 0.01–999, fiyat/Pfand 0–9999; kontrol karakterleri filtreleniyor. Paylaşılan metin 200.000 karakter, 100 satır, satır başına 90 karakter. |
| 7 | File & upload validation | 🔧 | Yedek içe aktarma: en fazla 8 MB, JSON nesnesi, şema 1, tablo başına 20.000 ve toplam 60.000 kayıt, eksik tablolar boş diziye tamamlanıyor. Ayrıca her satırın zorunlu alanları, alan tipleri, pozitif ve yinelenen kimlikleri doğrulanıyor; hata transaction başlamadan döner. Fiş görselleri yalnızca kamera + uygulama önbelleği; keyfî dosya yükleme yok. |
| 8 | CORS | ➖ | Sunucu/web origin yok. |
| 9 | Security headers | ➖ | Sunucu/web yok; mobil karşılığı `network_security_config.xml`. |
| 10 | HTTPS enforcement | ✅ | `usesCleartextTraffic=false` + `cleartextTrafficPermitted=false`; tüm adresler https, güven çapaları sistem sertifikaları. |
| 11 | Password hashing | ✅🔧 | Hesap/parola yok; isteğe bağlı **uygulama kilidi** PIN'i düz metin saklanmaz. 16 baytlık rastgele tuz + PBKDF2-HMAC-SHA256, 150.000 iterasyon, 256 bit özet; `MessageDigest.isEqual` ile sabit zamanlı karşılaştırma. 5 hatalı denemede 30 sn kilit. |
| 12 | Secure cookies & tokens | ➖ | Çerez, oturum veya JWT yok. |
| 13 | Obfuscation | ✅🔧 | R8 `isMinifyEnabled` + `isShrinkResources`, `proguard-android-optimize`, `Log` çağrıları release'te siliniyor. **Düzeltme:** `data.backup.**` Gson keep kuralı eklendi; yoksa yedek JSON anahtarları sürümden sürüme karışır ve eski yedek açılmaz. |
| 14 | Account deletion | ✅🔧 | Hesap yok; Google Play'in hesap silme şartı kapsam dışı. Yerine "Tüm verileri sil" akışı eklendi: veritabanı, yerel yedekler, broşür kırpmaları ve çökme kaydı. |
| 15 | Financial & cost alerts | ➖ | Ücretli bulut/servis yok. Play Console ve olası Firebase kullanımı için faturalama uyarısı hesap açıldığında kurulmalı. |

## 2. Siber güvenlik ve sertleştirme

| # | Madde | Durum | Kanıt / not |
|---|---|---|---|
| 16 | Authentication & RBAC | ➖🔧 | Hesap, oturum ve rol yok. Cihaz içi isteğe bağlı uygulama kilidi eklendi: 4 haneli PIN veya kayıtlı parmak iziyle açılış; 3 dakika arka planda kalınca yeniden kilitlenir. Uzak yetki kararı verilecek kaynak yok. |
| 17 | SQL injection | ✅ | Tüm sorgular Room DAO bind parametreleri; uygulama kodunda ham `query()`/`execSQL()` yok (geçişlerdeki DDL sabittir). |
| 18 | XSS | ➖ | WebView/HTML yok; Compose `Text` HTML yorumlamaz. |
| 19 | CSRF | ➖ | Çerez/oturum yok. |
| 20 | IDOR & ownership | ➖ | Uzak nesne yok; veri uygulama sandbox'ında, tek kullanıcıya ait. |
| 21 | SSRF | ✅ | İstek adresleri sabit kodlanmış alan adlarından üretilir; kullanıcı URL giremez. OkHttp yönlendirmeleri izler ama cleartext ağ kuralıyla http'ye düşemez. |
| 22 | Secrets management | ✅ | İstemci-only uygulama için Vault/Secrets Manager gereksiz; imza anahtarı repoya girmiyor, CI secret'larından geri yazılıyor. |
| 23 | IAM & OAuth | ➖ | Kimlik sağlayıcı/hesap yok. |
| 24 | JWT rotation | ➖ | Token yok. |
| 25 | Encryption at rest & in transit | ⚠️ | Taşımada TLS; cihazda Android dosya tabanlı şifreleme + sandbox. Uygulama kilidi yalnızca arayüz erişimini engeller, Room dosyasını ayrıca şifrelemez (SQLCipher yok). Kullanıcı yedeği **bilinçli olarak düz JSON** ve şifrelenmiyor; gizlilik metninde yazıyor. İstenirse parola tabanlı yedek şifreleme ayrı iş. |
| 26 | WAF & DDoS | ➖ | Sunucu yok. |
| 27 | Webhook signatures | ➖ | Webhook yok. |
| 28 | Dependency scanning | ✅🔧 | `.github/dependabot.yml` (Gradle haftalık, Actions aylık). Bağımlılıklar 24 Ekim 2026'da targetSdk 36 için güncellendi: AGP 8.13.1, Kotlin 2.2.10, Compose BOM 2025.09.01, Room 2.7.2, CameraX 1.4.2, Retrofit 2.11.0, OkHttp 4.12.0. Derleme + 145 birim testi + 17 cihaz testi + lint ile doğrulandı. |
| 29 | Adversarial testing | 🔧⚠️ | Sınır/bozuk girdi testleri eklendi: yedek şema/boyut/satır alanı/tekil kimlik, başarısız geri yüklemenin veriyi silmemesi, retry zaman aşımı/429/POST, içe aktarma, tarih, bildirim planı, kilit süresi ve bozuk PIN özeti. Fuzz/pentest koşusu yapılmadı. |

## 3. Sunucu, mimari ve mikroservis

| # | Madde | Durum | Not |
|---|---|---|---|
| 30 | Reverse proxy / API gateway | ➖ | Sunucu yok. |
| 31 | CI/CD | ✅🔧 | `ci.yml`: birim testleri, lint (hata eşiği), debug derlemesi ve API 36 16 KB emülatöründe `connectedDebugAndroidTest`. Release işi test/lint kapısından sonra imzalı APK + Play AAB üretir, imza ve 16 KB hizasını doğrular, R8 mapping'ini saklar. |
| 32 | Docker & Kubernetes | ➖ | Konteynerleştirilecek servis yok. |
| 33 | Service discovery / circuit breaker | ➖ | Servis topolojisi yok; istemcide sınırlı retry var. |
| 34 | Timeouts, retries, exponential backoff | ✅🔧 | 15 sn bağlanma / 20 sn okuma / 30 sn çağrı zaman aşımı vardı. `RetryInterceptor` eklendi: yalnızca GET, en fazla 3 deneme, 400 ms→4 sn üstel geri çekilme, `Retry-After` desteği, 408/429/5xx. |
| 35 | Idempotency | ➖ | Ödeme/sipariş yok; yerel işlemler Room transaction'larıyla atomik. |
| 36 | Message queues / Pub/Sub | ➖ | Mesajlaşma altyapısı yok. |
| 37 | Event-driven architecture | ➖ | Sunucu olayları yok; UI Room `Flow` ile reaktif. |
| 38 | Saga pattern | ➖ | Dağıtık işlem yok. |
| 39 | Dead letter queues | ➖ | Kuyruk yok. |
| 40 | Cron jobs | ✅ | WorkManager günlük hatırlatıcılar; uygulama açılışında günde bir yerel yedek. |
| 41 | WebSockets / SSE | ➖ | Gerçek zamanlı sunucu bağlantısı yok. |

## 4. Veritabanı, performans, ölçeklenebilirlik

| # | Madde | Durum | Not |
|---|---|---|---|
| 42 | Indexing & query optimization | 🔧 | 10 indeks eklendi: `products.barcode`, `list_items.isPurchased/productId`, `pantry_items.productId`, `purchase_history.purchaseDate`, `purchase_history_items.historyId/productId`, `pfand_entries.isReturned`, `money_entries.createdAt`, `flyer_list_items.validToEpochMs`. Şema v7 + geçiş testi. |
| 43 | N+1 query | ✅ | Sorgular açık JOIN'lerle tek seferde; ORM lazy-loading yok. |
| 44 | Connection pooling | ➖ | Tek süreçli SQLite/Room; havuz kavramı yok. |
| 45 | Read replicas / sharding | ➖ | Tek kullanıcılı yerel veritabanı. |
| 46 | Replication / leader election | ➖ | Çoğaltma yok. |
| 47 | Consistency models | ✅ | Tek yazarlı SQLite; yedek geri yükleme ve alışveriş tamamlama tek transaction. |
| 48 | Locking mechanisms | ✅ | Room transaction'ları; dağıtık kilit gerektiren kaynak yok. |
| 49 | Concurrency hazards | ✅ | Yazma yolları transaction içinde; Room yazmaları serileştirir. Stres testi yapılmadı. |
| 50 | Memory & resource leaks | ⚠️ | Kamera executor'ı kapatılıyor, görsel yükleme Coil'e sınırlı, yedek okuması 8 MB ile sınırlı. Profiler/LeakCanary koşusu yapılmadı. |
| 51 | Backpressure & autoscaling | ➖✅ | Otomatik ölçekleme yok (istemci). Kamera `STRATEGY_KEEP_ONLY_LATEST` ile backpressure uyguluyor. |
| 52 | Caching & invalidation | ✅ | Son başarılı prospektüs listesi `flyer-cache` altında; ağ hatasında `fromCache` rozetiyle gösterilir; görseller Coil önbelleğinde. |
| 53 | Feature flags | ➖ | Uzak yapılandırma/deney altyapısı yok; gereksiz karmaşıklık olurdu. |
| 54 | Migrations & schema versioning | ✅🔧 | Room v7, `app/schemas` dışa aktarımı, `AppMigrations` (v2→v7), `MigrationTest` 5/5 API 36 emülatöründe geçti. |
| 55 | Deployment strategies | ⚠️ | GitHub Release + imzalı APK/AAB var; Play aşamalı dağıtım ve geri çekme adımları `docs/PLAY_YAYIN_KONTROL_LISTESI.md` içinde. Play hesabı/mağaza formları dış adım olarak açık. |
| 56 | Health checks & probes | ➖ | Sunucu yok. |
| 57 | Monitoring & logging | ✅🔧 | Release'te `Log` çağrıları R8 ile siliniyor; loglarda kullanıcı verisi yok. Yerel çökme kaydı eklendi. Merkezî izleme yok. |
| 58 | Tracing & metrics | ➖ | Dağıtık sistem yok. |
| 59 | Alerts, SLO, SLI | ➖ | Servis yok. |
| 60 | Infrastructure as Code | ➖ | Yönetilen altyapı yok. |
| 61 | Build caching | ✅ | CI'da Gradle cache; bağımlılık kilidi/version catalog yok. |
| 62 | Incidents & on-call | ➖ | Tek geliştirici; kurumsal nöbet süreci yok. |
| 63 | Disaster recovery & failover | ✅➖ | Uygulama verisi için günlük yerel yedek + kullanıcı dışa aktarımı var. Bölgesel failover altyapı olmadığı için yok. |
| 64 | Network & protocol | ✅ | HTTP/2 (OkHttp), TLS, zaman aşımı/retry; gRPC yok; sürümleme SemVer `versionName`. |

## 5. DevOps, dağıtım, gözlemlenebilirlik

Yukarıdaki 55–63 satırları bu bölümü kapsıyor. Ek olarak:

- CI lint eşiği: `abortOnError = true`; bu turda lint'in bulduğu **21 hata** kapatıldı
  (19 hatalı `%s`/Int formatı, 1 bildirim izni, 1 kamera opt-in).
- İmzalı release yalnızca `KEYSTORE_*` secret'ları tanımlıysa üretiliyor; imza
  doğrulanmadan artifact yüklenmiyor.

## 6. Mobil, mağaza ve UX

| # | Madde | Durum | Not |
|---|---|---|---|
| 65 | Crash reporting | 🔧⚠️ | `CrashLog` yakalanmamış hatayı cihazda tutar (sürüm, Android, yığın izi; kullanıcı verisi yok, 64 KB sınır). Yedekleme ekranından paylaşılabilir. Crashlytics/Sentry hesabı olmadığı için merkezî raporlama yok. |
| 66 | Global exception handling | 🔧 | Varsayılan handler sarıldı: kayıt yazılır, sonra sistem handler'ı çalışır; uygulama çökme diyaloğu yine görünür (kullanıcıya teknik kod gösterilmez). Özel hata ekranı yok. |
| 67 | Offline state | ✅ | Uygulama çevrimdışı öncelikli; prospektüs önbelleği, `fromCache` bilgisi, boş/hata durumları ve yeniden dene düğmeleri var. |
| 68 | Store legal pages | 🔧⚠️ | Uygulama içi `LegalScreen` (gizlilik, şartlar, iletişim) eklendi; `docs/legal/` altında yayınlanabilir HTML sayfaları var. **Açık:** Play Console'a HTTPS URL girilmeli. |
| 69 | SEO / metadata | ➖ | Web sitesi/landing page yok. Mağaza tarafı ASO ve Data Safety formu Play Console adımı. |

---

## Bu turda yapılan kod değişiklikleri

| Alan | Dosya |
|---|---|
| Yedek doğrulama, boyut/şema sınırları | `data/backup/BackupValidator.kt`, `BackupManager.kt` |
| Yedek anahtarının R8'de korunması | `app/proguard-rules.pro` |
| GET retry + exponential backoff | `data/remote/RetryPolicy.kt`, `RetryInterceptor.kt`, `di/NetworkModule.kt` |
| Girdi sınırları | `ui/components/AddProductSheet.kt`, `feature/share/ListImport.kt`, `SharedListImportDialog.kt`, `MainActivity.kt` |
| Çökme kaydı | `security/CrashLog.kt`, `App.kt` |
| Gizlilik/şartlar ekranı | `feature/legal/LegalScreen.kt`, `ui/AppNavigation.kt` |
| Tüm verileri silme | `BackupManager.deleteEverything()`, `BackupViewModel.wipeAll()`, `BackupScreen.kt`, `SeedState.kt` (silme sonrası demo veri geri gelmez) |
| Veritabanı indeksleri + v7 geçişi | `data/model/Entities.kt`, `data/local/ShoppingDatabase.kt`, `AppMigrations.kt`, `androidTest/.../MigrationTest.kt` |
| CI ve bağımlılık taraması | `.github/workflows/ci.yml`, `.github/dependabot.yml`, `app/build.gradle.kts` |
| Yasal metinler | `docs/legal/index.html`, `privacy.html`, `terms.html` |
| Çok dillilik + aramalı dil seçimi | `feature/language/LanguageStore.kt`, `LanguageCatalog.kt`, `LanguageScreen.kt`, `res/values-*/strings.xml`, `ui/AppNavigation.kt`, `feature/backup/BackupScreen.kt` |
| PIN + biyometrik uygulama kilidi | `security/AppLock.kt`, `AppLockStateViewModel.kt`, `AppLockScreen.kt`, `AppLockSettingsDialog.kt`, `MainActivity.kt`, `AndroidManifest.xml` |
| Kilit ve dil testleri | `androidTest/security/AppLockTest.kt`, `test/feature/language/LanguageCatalogTest.kt` |

Test kanıtı: `:app:testDebugUnitTest` **145 test, 0 hata**; `:app:lintDebug`
**0 hata, 146 bulgu (139 uyarı, 7 bilgi)**; `:app:connectedDebugAndroidTest` **17/17**
(API 36 16 KB emülatörü: Migration 5, AppLock 9, BackupRestore 2, Compose 1).
Emülatörde imzalı release kuruldu: gizlilik ekranı TR/EN/DE açıldı, "Tüm verileri sil"
sonrası yeniden açılışta liste boş kaldı, çökme tamponu boş. Release APK:
`aapt2` targetSdk **36**, `zipalign -c -P 16 -v 4` **Verification successful**.
Play AAB `app-release.aab` üretildi; `jarsigner -verify` **jar verified** ve R8
`mapping.txt` artifact olarak saklanıyor.

---

## Tamamlanan son iş — targetSdk 36 ve bağımlılık yükseltmesi

Google Play'in 31 Ağustos 2026 sonrası istediği Android 16 (API 36) hedefi tamamlandı.

- **Sürüm seti:** AGP 8.13.1, Gradle 8.14.5, Kotlin 2.2.10, Compose BOM 2025.09.01,
  Room 2.7.2, Hilt 2.57.2, Retrofit 2.11.0, OkHttp 4.12.0, CameraX 1.4.2,
  WorkManager 2.10.2, Coil 2.7.0. Kotlin 2.2 + AGP 8.13 minimum Gradle 8.13 ister;
  wrapper 8.14.5'e yükseltildi. Room/Hilt halen kapt ile derleniyor (KSP geçişi ayrı iş).
- **API 36 zorunlulukları:** `compileSdk`/`targetSdk` 36; `enableEdgeToEdge()` ile
  sistem çubukları saydam; `statusBarColor`/`navigationBarColor` atamaları kaldırıldı,
  ikon kontrastı `isAppearanceLight*` ile yönetiliyor. Ana ekran ve tarayıcı inset
  tüketimine göre düzeltildi; izin ekranı yalnızca izin yokken çizilerek tarama
  kontrolleriyle üst üste binmesi giderildi. Açık/koyu tema ve tarayıcı ekran
  görüntüsüyle doğrulandı.
- **16 KB sayfa hizası:** Release APK'daki tüm `.so` dosyaları 16 KB hizalı;
  `zipalign -c -P 16 -v 4` başarılı. Aynı kontrol `.github/workflows/android-release.yml`
  içinde her release derlemesinde otomatik çalışıyor. APK, `google_apis_playstore_ps16k`
  (16 KB sayfalı) API 36 sistem imajında kurulup açıldı; liste, açık/koyu tema, tarayıcı
  ekranı ve kamera izni akışı doğrulandı, 5/5 cihaz testi geçti.
- **Doğrulama:** yukarıdaki 131 birim testi, lint, API 36 emülatöründe 5/5 cihaz testi
  ve imzalı release duman testi.

---

## Tamamlanan son iş — çok dillilik ve uygulama kilidi (v2.3)

### Yerelleştirme

- Arayüz metinleri `strings.xml`'e taşındı; **11 dil**: Türkçe, İngilizce (ABD), İngilizce (Birleşik Krallık),
  Almanca, Bulgarca, Sırpça, Basitleştirilmiş Çince, Ukraynaca, Rusça, Lehçe ve Felemenkçe.
- `values`, `values-en`, `values-en-rGB`, `values-de`, `values-bg`, `values-sr`, `values-zh-rCN`,
  `values-uk`, `values-ru`, `values-pl`, `values-nl`. Play dil bölmesi kapatıldı
  (`bundle.language.enableSplit = false`), böylece uygulama içi seçim tüm dilleri anında kullanır.
- Aramalı dil ekranı (`LanguageScreen`) ve Backup & settings girişi; seçim DataStore/SharedPreferences
  yerine küçük bir SharedPreferences anahtarında tutulur. API 33+ sistem `LocaleManager.applicationLocales`
  kullanır, eski sürümlerde `attachBaseContext` ile yapılandırma sarılır.
- Doğrulama: İngilizce ana ekran → dil ekranı → Almanca seçimi; ekran yeni dilde yeniden çizildi
  (`screenshots/api36-language.png`, `screenshots/api36-language-de.png`).

### Uygulama kilidi

- `security/AppLock.kt`: 4 haneli PIN, 16 bayt rastgele tuz, PBKDF2-HMAC-SHA256 (150.000 iterasyon),
  5 hatalı denemede 30 sn kilit; PIN hiçbir yerde düz metin tutulmaz.
- `AppLockScreen`: geri tuşuyla kapanmayan tam ekran kilit; nokta göstergesi, hatalı PIN ve
  kilit süresi mesajı; kayıtlıysa parmak izi düğmesi.
- `AppLockSettingsDialog`: etkinleştir / PIN değiştir / kaldır; mevcut PIN doğrulanmadan değişiklik yok.
- `MainActivity`: `FragmentActivity` + `BiometricPrompt` (`BIOMETRIC_WEAK`); uygulama 3 dakikadan
  uzun süre arka planda kaldıysa `onStart`'ta yeniden kilitlenir. `AppLockStateViewModel`
  açık oturumu döndürme/dil değişimi boyunca korur; süreç ölürse kilit yeniden devreye girer.
  Doğrulama: 90° döndürmede liste açık kaldı, force-stop sonrası kilit ekranı geri geldi.
- Manifest'e yalnızca `USE_BIOMETRIC` eklendi (fotoğraf/kişi erişimi yok).
- Doğrulama (API 36, 16 KB sayfa emülatörü): yanlış PIN mesajı, doğru PIN ile açılış, parmak izi
  kaydı → biyometrik açılış, uygulama yeniden başlatmada kilit ekranı; `screenshots/api36-app-lock.png`.

---

## Yayın hazırlığı turu — hata testleri ve AAB (25 Ekim 2026)

- **Yedek dayanıklılığı:** `BackupValidator` tablo boyutunun yanında her satırın zorunlu
  alanlarını, tiplerini, pozitif kimliklerini ve yinelenen kimlikleri doğrular. Yeni
  `BackupRestoreTest` boş/bozuk/eksik alanlı/gelecek şemalı/8 MB üstü yedeklerin mevcut
  veriyi silmediğini; geçerli yedeğin fazladan kayıtları temizleyerek geri yüklendiğini
  gerçek Room veritabanında kanıtlar.
- **Ağ hataları:** `RetryInterceptorTest` (MockWebServer) 500→200, 429 `Retry-After`,
  POST'un tekrarlanmaması ve 3 deneme sınırını kapsar.
- **Kilit hataları:** kilit süresi dolma yolu ve bozuk PIN özetiyle asla açılmama testleri.
- **CI:** API 36 / 16 KB Play Store imajlı emülatör işi; release işi test + lint kapısından
  sonra imzalı APK ve AAB üretir, AAB imzasını ve 16 KB hizasını doğrular, `mapping.txt`
  artifact'ını saklar. Ayrıntılı mağaza adımları: [PLAY_YAYIN_KONTROL_LISTESI.md](PLAY_YAYIN_KONTROL_LISTESI.md).
- **Kapsam dışı kalanlar:** merkezî çökme raporlama hesabı, fuzz/pentest ve LeakCanary
  profili; bunlar Play yayını için zorunlu değil, sonraki kalite turunda ele alınmalı.

## Yayına çıkmadan önce kapatılması zorunlu (P0)

1. **Gizlilik politikası URL'si.** `docs/legal/` GitHub Pages'e (Settings → Pages →
   `/docs`) yayınlanıp Play Console'a ve mağaza listelemeye girilmeli.
2. **Play Console kurulumu:** Data Safety formu (veri toplanmıyor; barkod üçüncü
   tarafa işlevsellik için gidiyor), içerik derecelendirmesi, geliştirici e-posta,
   mağaza görselleri, App Signing.
3. **İmza anahtarı yedeği.** `keystore/` ve `keystore.properties` kaybolursa
   güncelleme yayınlanamaz. Şifre yöneticisi + çevrimdışı kopya.

## P1 iyileştirmeler

- Dependabot PR'larını izleme ve isteğe bağlı version catalog.
- Yedek dosyasına isteğe bağlı parola tabanlı şifreleme.
- Crashlytics/Sentry (hesap açılırsa) ve merkezî hata panosu.
- LeakCanary/profiler ile bellek denetimi.
- Fuzz testleri (yedek JSON ve paylaşılan metin ayrıştırıcı) ve bağımsız pentest.
- Play aşamalı dağıtım + rollback runbook'u.
- Baseline profile ile soğuk açılış süresi.

## Sunucu eklenirse uygulanacak maddeler (bu depoda yok)

4, 5, 8, 9, 11, 12, 16, 19, 22–24, 26, 27, 30, 32, 33, 35–39, 41, 44–46, 51,
55, 56, 58–60, 62, 63. Hesap/senkronizasyon eklenirse önce sunucu tarafında
yetkilendirme, sahiplik kontrolü, hız sınırı, girdi doğrulama, JWT rotasyonu,
şifreleme ve merkezî izleme kurulmalı; istemci bunların yerine geçmez.
