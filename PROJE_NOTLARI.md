# Gelişmiş Kişisel Market & Alışveriş Uygulaması - Proje Notları ve Geliştirme Günlüğü

## 📋 Proje Amacı
**Gelişmiş Kişisel Market & Alışveriş Uygulaması**, Bring! benzeri görsel ve pratik alışveriş listesi konseptini temel alan, Almanya / Münih şartlarına ve kişisel kullanıma özel olarak uyarlanmış yüksek performanslı bir Android uygulamasıdır.

**Temel Vizyon:** Sunucu/üye girişi olmadan, tamamen çevrimdışı (offline-first) çalışan, kullanıcıya özel bir market alışveriş asistanı.

**Ana Farklar (Bring! + Gelişmiş):**
1. Almanya / Münih marketlerine özel reyon sıralaması (Edeka, Rewe, Lidl, Aldi Süd, Kaufland, dm vb.).
2. Almanya'ya özgü Pfand (depozito) takip sistemi.
3. Kamera ile barkod tarama ve otomatik ürün bilgileri.
4. Kiler / Vorratskammer (stok) yönetimi.
5. Aylık/haftalık harcama analizi ve fiyat karşılaştırma.
6. Tamamen yerel veritabanı (Room DB) ile kişisel ve gizli kullanım.

---

## 📜 Yapılanlar (Completed)

### 1. Proje Kurulumu ve Altyapı (Task 1)
**Süreç:** `superpowers:subagent-driven-development` ile başlatıldı.
**Durum:** ✅ Tamamlandı
**Detaylar:**
- **Git Kurulumu:** Proje için `.git` klasörü oluşturuldu.
- **Build Yapılandırması:** `build.gradle.kts` (proje ve uygulama) dosyaları oluşturuldu.
- **Bağımlılıklar:** Hilt, Jetpack Compose, CameraX, ML Kit Barcode, Room, Retrofit, Coil gibi tüm kritik kütüphaneler eklendi.
- **Uygulama Sınıfı:** Hilt ile entegre `App.kt` oluşturuldu.
- **İzinler:** `AndroidManifest.xml` dosyası CAMERA ve INTERNET izinleriyle güncellendi.

### 2. Çekirdek Veri Katmanı (Task 2)
**Süreç:** `superpowers:subagent-driven-development` ile subagent'a devredildi.
**Durum:** ✅ Tamamlandı
**Detaylar:**
- **Room Entities:** Spesifikasyona göre tüm veritabanı varlıkları oluşturuldu:
  - `Product` (Ürünler: barcode, name, pfandAmount)
  - `Store` (Marketler: reyon sıralaması için aisleOrderJson)
  - `Category` (Kategoriler: DE/TR çift dil desteği)
  - `ListItem` (Aktif Alışveriş Listesi)
  - `PantryItem` (Kiler / Stok)
  - `PurchaseHistory` & `PurchaseHistoryItem` (Alışveriş Geçmişi)
- **DAO & Veritabanı:** `ShoppingDao` ve tüm veritabanı erişim sınıfları oluşturuldu.

### 3. Arayüz ve Navigasyon (Task 4)
**Süreç:** Subagent ile geliştirildi.
**Durum:** ✅ Tamamlandı
**Detaylar:**
- **Ana Ekran:** Jetpack Compose ile çalışır durumda bir ana ekran.
- **Grid Yapısı:** Bring! benzeri kategori bazlı renkli grid layout.
- **Navigasyon:** Uygulamanın ilk ekran akışı başlatıldı.

### 4. Barkod Tarama (Task 3)
**Süreç:** Subagent ile geliştiriliyor.
**Durum:** 🔄 Devam Ediyor...
**Detaylar:** CameraX ile canlı kamera önizlemesi ve Google ML Kit ile EAN-13 barkod okuma entegrasyonu.

---

## 📈 Yapılacaklar (Pending / To-Do)

### 5. Pfand ve İstatistik Yönetimi (Task 5)
**Durum:** 📋 Planlandı
**Sorumluluk:** Subagent devri.
**Özellikler:**
- Pfand (depozito) otomatik hesaplama.
- Aylık/Haftalık Euro harcama grafikleri.
- Market karşılaştırması (hangi markette ne kadar ucuz).
- Stok takibi ve son kullanma tarihi uyarıları.

### 6. Kiler / Stok Yönetimi (Pantry)
**Durum:** 📋 Pan try modülü, Task 5 ile entegre edilecek.
**Sorumluluk:** Subagent devri.
**Özellikler:**
- Evdeki ürün stok seviyesi takibi.
- Kritik eşik altındaki ürünlerin otomatik alışveriş listesine eklenmesi.

### 7. Test ve Son Dokunuşlar
**Durum:** 📋 Planlandı
**Özellikler:**
- Farklı Android cihazlarda test.
- APK sürümü üretimi.
- Küçük UI iyileştirmeleri ve hata düzeltmeleri.

---

## 🛠 Kullanılan Teknoloji Yığını (Tech Stack)

### Programlama Dili
- **Kotlin** - Android Native

### Mimari
- **MVVM** (Model-View-ViewModel) + Clean Architecture
- **Hilt** - Bağımlılık Enjeksiyonu
- **Offline-First** - Tüm veriler yerel

### UI
- **Jetpack Compose** - Modern Deklaratif UI
- **Material3** - Tasarım Sistemi
- **Accompanist** - Animasyonlar ve grid tasarımları

### Veritabanı
- **Room Database** (SQLite) - Yerel veritabanı
- **Flow** - Reaktif veri akışı

### Kamera ve Tarama
- **CameraX** - Kamera önizlemesi
- **Google ML Kit** - Barkod Tarama (EAN-13/EAN-8)

### Ağ ve Görseller
- **Retrofit + Gson** - Open Food Facts API entegrasyonu
- **Coil** - Görsel yükleme ve önbellekleme

### Diğer
- **MPAndroidChart / Canvas** - Harcama grafikleri

---

## 💡 Geliştirme Notları
- **Proje Klasörü:** `C:\Users\gezgi\shoppingapp`
- **Geliştirme Metodu:** `superpowers:subagent-driven-development`
- **Tasarım Referansı:** `docs/superpowers/specs/2026-10-16-shopping-app-design.md`
- **Plan Referansı:** `docs/superpowers/plans/2026-10-16-shopping-app-plan.md`
- **Durum:** Altyapı kuruldu, arayüz hazır, kamera ve istatistik özellikleri geliştiriliyor.

---

## 📝 Bir Sonraki Adım
Mevcut durum: Uygulama APK olarak derlenebilir durumda. Arayüz çalışıyor.
İlerisi: Barkod tarama ve Pfand/İstatistik modüllerinin tamamlanması ile uygulamanın %100 çalışır hale gelmesi.

---

## 🔧 2026-10-19 — Derleme Onarımı ve İlk Çalışan APK

Proje ilk kez uçtan uca derlendi, emülatöre kuruldu ve üç ekran da doğrulandı.

### Düzeltilen engeller
- **Gradle wrapper:** `gradlew` sadece `exec gradle` çağıran 3 satırlık bir script'ti, `gradle-wrapper.jar` ve `gradlew.bat` yoktu. Gradle 8.14.5 wrapper'ı yeniden üretildi.
- **Hilt plugin kimliği:** Kök projede `com.google.dagger.hilt.android` tanımlıyken modülde `dagger.hilt.android.plugin` uygulanıyordu; build hiç başlamıyordu.
- **AndroidManifest:** AGP 8 ile kaldırılan `package=` özniteliği silindi (namespace zaten `app/build.gradle.kts` içinde).
- **`PfandDao` yoktu:** `ShoppingDatabase` var olmayan bir DAO'ya referans veriyordu; referans ve karşılıksız `pfandDao()` kaldırıldı.
- **Compose Material 2 / Material 3 karışımı:** `AppNavigation` ve `PfandScreen` Material 2 API'leri (`BottomNavigation`, `MaterialTheme.colors`, `backgroundColor`) kullanıyordu ama Material 2 bağımlılığı yoktu. İkisi de Material 3'e taşındı.
- **`PfandViewModel`:** `@HiltViewModel` ve `@Inject` importu eksikti; `hiltViewModel()` çalışma anında çökerdi. Düzeltildi, hesaplama `Locale.GERMANY` ile biçimlendiriliyor.
- **Room kimlik çakışması:** Tüm entity'lerde `@PrimaryKey val id = 0` vardı; yeni eklenen her `ListItem` aynı satırı (id=0) eziyordu. `autoGenerate = true` yapıldı, şema değiştiği için DB sürümü 2'ye çıkarıldı.
- **Eksik dosyalar:** `app/proguard-rules.pro` ve `.gitignore` eklendi.
- `StatsScreen.kt` dosyası `feature/stats/` altına taşındı (paketi zaten `com.shoppingapp.feature.stats` idi).

### Doğrulama
- `./gradlew :app:assembleDebug` → **BUILD SUCCESSFUL**, `app/build/outputs/apk/debug/app-debug.apk` (~37 MB).
- Emülatör (Medium_Phone_API_37.0) kurulumu ve açılış: çökme yok.
- Liste, Pfand ve İstatistik ekranları açılıyor; ekran görüntüleri `screenshots/` altında.

### Sıradaki işler (öncelik sırasıyla)
1. **Alışverişi tamamla akışı:** şu an `PurchaseHistory` yalnızca örnek veriyle dolu; "Alışverişi bitir" butonu, tutar/Pfand hesabı ve geçmişe yazma yok.
2. **Kiler (Pantry) ekranı:** `PantryItem` tablosu ve `getPantryItems()` var, UI yok; kritik eşik → listeye ekleme akışı yok.
3. **Fiyat/mağaza karşılaştırması:** `PurchaseHistoryItem.priceEuro` doldurulmuyor.
4. **Kalıcılık sağlamlaştırma:** `fallbackToDestructiveMigration()` yerine gerçek migration'lar; `barcode` için unique index.
5. **Testler:** ViewModel/Room için birim testi yok.
6. **UX/temizlik:** string kaynakları (şu an metinler kod içinde), erişilebilirlik, `TUM_PROJE_KODLARI.txt` ve 681 MB'lık `java_pid29496.hprof` dosyalarının kaldırılması.

---

## 🛒 2026-10-19 — Barkod, Pfand ve görsel yenileme

Tasarım yönü `ui-ux-pro-max` skill'inin ürettiği "fresh green + food amber" paletine göre kuruldu ve Jetpack Compose'a uyarlandı.

### Barkod tarama (yeni)
- `feature/scanner/BarcodeScannerScreen.kt` + `ScannerViewModel`: CameraX önizleme, ML Kit (EAN-13/8, UPC-A/E, Code 128), fener düğmesi, kamera izni akışı.
- Okunan barkod önce Room'da aranır, yoksa **Open Food Facts**'ten ürün adı/marka/kategori/görsel/Pfand tahmini çekilir; ağ yoksa bile ürün "Ürün <barkod>" olarak oluşturulur.
- Ürün anında **Alınanlar** listesine düşer; ekranda son eklenen ürün ve sayaç görünür.
- Kamera okuyamazsa diye "Barkodu elle gir" diyalogu eklendi.

### Pfand (yeniden yazıldı)
- Yeni `PfandEntry` tablosu (oda `pfand_entries`); toplamlar artık **kalıcı**, uygulama kapanıp açılınca sıfırlanmıyor.
- Hazır türler: Teneke 0,25 €, Plastik şişe 0,25 €, Cam şişe 0,08 €, Cam şişe 0,15 €, Kasa 1,50 €. Birim fiyat ve adet serbestçe düzenlenebilir.
- **Toplam Pfand değeri** kartı, adet sayısı, "İadeyi tamamla" (geçmişe taşır) ve iade geçmişi.
- DB sürümü 3 + gerçek `MIGRATION_2_3` eklendi; mevcut liste verisi silinmiyor.

### Görseller
- Taranan ürünlerde Open Food Facts'in gerçek ürün fotoğrafı kullanılıyor.
- Tarama dışı ürünler için 6 adet ücretsiz (Unsplash lisanslı) kategori fotoğrafı `res/drawable-nodpi/` içine gömüldü: meyve-sebze, süt, içecek, atıştırmalık, temizlik, varsayılan. Böylece her satırda anında ve çevrimdışı bir fotoğraf var; internet servislerine bağımlılık yok.

### Arayüz
- Yeni tema (`ui/theme/Theme.kt`): yeşil/amber palet, açık + koyu şema, 16-24 dp köşe tutarlılığı.
- Alt menü 3 sekme: **Liste / Pfand / İstatistik**; tarayıcı tam ekran ve alt menüsüz açılıyor.
- Liste: özet kartı (alınacak/alındı + sepet Pfandı), ürün fotoğrafı, mağaza ve Pfand etiketleri, adet artır/azalt, satın alındı işaretleme.
- Yeni ürün diyalogu: ad, miktar, birim, fiyat (€), Pfand, kategori ve market seçimi + doğrulama.
- Fiyat/Pfand alanları artık boş geliyor (yer tutucu `0,00`); kullanıcı önce "0,00"ı silmek zorunda değil.
- İstatistik ekranı gerçek Pfand tablosunu okuyor; para biçimi Almanya standardında (€18,50).

### Alışverişi tamamla (sepet → Euro hesabı → geçmiş)
- Liste üzerindeki özet kartı sepetin **Euro toplamını** ve Pfand tutarını canlı gösteriyor (`€4,05` / `Pfand €0,75`).
- "Alışverişi tamamla" diyalogu her kalemi tek tek listeler; birim fiyatlar diyalog içinde düzeltilebilir ve **Toplam** satırı yazarken anında yeniden hesaplanır.
- Onaylanınca: `purchase_history` + `purchase_history_items` kaydı yazılır, girilen fiyatlar ürüne kalıcı olarak işlenir (sonraki sefer diyalog bu fiyatlarla açılır), liste temizlenir ve makbuz diyalogu toplam/Pfand/kalemi gösterir.
- Kayıt sonrası liste boş duruma döner; istatistikteki "Toplam Harcama (Geçmiş)" tutarı yeni alışverişle toplanarak güncelleniyor.

### Telefona kurulum ve güncelleme (imza / versionCode)
- Kalıcı imza anahtarı: `keystore/shoppingapp-release.jks` + parolası `keystore.properties` içinde; ikisi de `.gitignore`'da (git'e girmez). **Bu iki dosyayı yedekle**: kaybolursa uygulama bir daha güncellenemez, silip baştan kurmak gerekir.
- `app/build.gradle.kts` artık release build'i bu anahtarla imzalıyor. Aynı anahtar + aynı `applicationId` + **artan `versionCode`** = telefonda "güncelleme" (uygulama verisi, liste ve geçmiş korunur).
- Her sürümde `versionCode`'u bir artır (şu an 2 / versionName 1.1). APK: `./gradlew :app:assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.
- Telefona güncelleme: `adb install -r app/build/outputs/apk/release/app-release.apk` ya da APK'yı telefona kopyalayıp dokunmak yeterli; kaldırmak gerekmez.
- **Dikkat:** debug APK farklı anahtarla imzalandığı için telefona kurulan release sürümün üstüne yazılamaz; telefonda yalnızca release APK kullan.

### Kablosuz kurulum (USB gerekmez) — `scripts/telefon-kur.sh`
- Telefonda: Geliştirici seçenekleri → **Kablosuz hata ayıklama** aç (Android 11+). Telefon ve bilgisayar aynı Wi-Fi ağında olmalı.
- İlk kez: telefonda "Eşleştirme koduyla eşleştir" ekranındaki `IP:PORT` + 6 haneli kodu kullan → `bash scripts/telefon-kur.sh esle <IP:PORT> <KOD>`.
- Sonraki her güncelleme: `bash scripts/telefon-kur.sh kur <IP:PORT>` (telefonun Kablosuz hata ayıklama ekranındaki bağlantı noktası; port telefonu yeniden başlatınca değişebilir). `adb connect` + `install -r` yapar, yani veri korunur.
- `bash scripts/telefon-kur.sh servisler` = ağdaki adb/uçbirimlerini listeler (IP:PORT bulmak için).
- Usb hiç yok / adb istemiyorsan alternatif: APK'yı Drive, Telegram veya e-posta ile telefona gönder, dosyaya dokun, kur.
- **Telefon eşleşti ve uygulama kuruldu (2026-10-19):** cihaz `R5CX159XL2J`, IP `192.168.178.47`. Eşleştirme telefon tarafında kalıcı; sonraki güncellemelerde tekrar kod gerekmez, sadece `adb connect <IP>:<PORT>` (port her bağlantıda değişebilir, `adb mdns services` ile bulunur) + `bash scripts/telefon-kur.sh kur` → kurulum `versionCode=2 / versionName=1.1` olarak doğrulandı, uygulama telefonda açıldı.

### Doğrulama
- `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- Emülatör: liste, Pfand ekleme (2 × Cam şişe = €0,16), uygulamayı zorla kapatıp açtıktan sonra verinin korunması, manuel ürün ekleme, istatistik ve tarayıcı ekranı doğrulandı.
- Alışverişi tamamla uçtan uca denendi: Milch 2 × €1,29 + Wasser 3 × €0,49 → diyalogda **Toplam €4,05 / Pfand €0,75**, makbuz "5 ürün · Rewe", liste temizlendi ve istatistik toplamı €28,50 → **€32,55** oldu (fark tam olarak €4,05).
- Koyu tema ayrıca emülatörde `cmd uimode night yes` ile görsel olarak kontrol edildi.
- Release imzası `apksigner verify --print-certs` ile doğrulandı (CN=Shopping App, SHA-256 a596e8f9…).
- **Güncelleme testi (gerçek senaryo):** imzalı release APK kuruldu (versionCode 1) → listeye "Marker" adlı €7,77'lik ürün eklendi → `versionCode = 2` yapılıp yeniden derlendi → **uninstall yapmadan** üstüne kuruldu → `versionCode=2 / versionName=1.1` oldu ve "Marker" ürünü sepet toplamıyla (€7,77) birlikte duruyordu. Yani veri kaybı olmadan yerinde güncelleme çalışıyor.
- **Gerçek telefon (Samsung A54, 1440×3120, Android 16):** APK kablosuz kuruldu (`versionCode=2 / 1.1`), uygulama açıldı, liste/özet kartı doğru çizildi. Tarayıcı ekranı gerçek kamerayla test edildi: **canlı kamera önizlemesi çalışıyor**, `CAMERA` izni `granted=true` (kullanıcı onayı ile). Kalan tek doğrulanmamış adım: gerçek bir barkodun çözülüp Open Food Facts'ten ürün çekmesi — kameraya bir market ürünü tutulunca görülecek.
- **Sınırlama:** Emülatörün sanal kamerası gerçek barkod üretmediği için emülatörde uçtan uca barkod okuma denenemedi.
---

## 🏷️ 2026-10-19 — Marka önerileri, yeni ikon/ad, market listesi ve sıfırlama (sürüm 1.2 / versionCode 3)

### İkon ve uygulama adı
- Uygulama adı **Einkauf** (`res/values/strings.xml`), manifest artık `@string/app_name` + `@mipmap/ic_launcher` kullanıyor.
- Yeni adaptive ikon: `drawable/ic_launcher_background.xml` (yeşil degrade) + `drawable/ic_launcher_foreground.xml` (beyaz market çantası + amber liste çizgileri); `mipmap-anydpi-v26` ve API 33+ için `mipmap-anydpi-v33` (themed icon) sürümleri var. Eski `drawable/ic_launcher.xml` kaldırıldı.
- Uygulama içi logo işareti `drawable/ic_logo_mark.xml`; boş liste ekranında kullanılıyor.

### Marketler
- Varsayılan marketler sabit id ile **upsert** ediliyor (`seedConfig()`), yani eski kurulumlara da ekleniyor: Rewe, Edeka, **Aldi Süd** (eski adı "Aldi"), Lidl, Kaufland, **Penny, Netto, dm, Rossmann**.
- Market/kategori kayıtları silinmediği için mevcut liste satırları bozulmuyor.

### Marka önerileri (asıl yeni özellik) — `data/catalog/BrandCatalog.kt`
- Akış: **önce market seç → ürün adına yaz → o marketin kendi markalarıyla öneriler** gelir. Market seçilmezse öneri çıkmaz, ürün yazıldığı gibi (markasız) eklenir.
- Türkçe ve Almanca kelimeler birlikte anlaşılıyor: `yoğurt/joghurt`, `süt/milch`, `su/wasser`, `ekmek/brot`, `yumurta/eier`, `peynir/käse`, `kahve/kaffee`, `makarna/nudeln`, `çikolata/schokolade`, `deterjan/spülmittel`, `tuvalet kağıdı/toilettenpapier`, `şampuan/shampoo` vb.
- Doğrulanmış Eigenmarken eşlemesi (marketlerin kendi ürün sayfalarından): Aldi Süd **MILSANI** (süt ürünleri), Lidl **Milbona** (süt) ve **Saskia** (su), Rewe **ja!**, Edeka **GUT&GÜNSTIG**, Penny **MILPRIMA**, Netto **Gutes Land**, Kaufland **K-Classic**, Aldi Süd ayrıca GOLDÄHREN (fırın), BARISSIMO (kahve), CHOCŒUR (çikolata), RIO D'ORO (meyve suyu), KARLSKRONE (bira), TANDIL (temizlik), dm **Balea/Denkmit**, Rossmann **ISANA/domol**.
- Marka doğrulanamayan kategoride öneri markasız üretilir — yanlış marka yazılmaz.
- Aldi Süd + "joghurt" için doğrulanmış özel öneri: "MILSANI Joghurt nach türkischer Art 3,5% Fett".
- Düzeltme: kullanıcı "Aldi → Milbona" demişti; **Milbona Lidl'in** markası, Aldi Süd'ün süt markası **MILSANI**.

### Ürün ekleme ekranı yenilendi
- `AddItemDialog` yerine **ModalBottomSheet** (`ui/components/AddProductSheet.kt`): market çipleri en üstte, marka öneri kartları, Miktar/Birim, Fiyat/Pfand (boş + yer tutucu), **Kategori: Otomatik**.
- Otomatik kategori ürün adından tahmin edilir (`BrandCatalog.detectCategory`), böylece liste satırındaki fotoğraf doğru oluyor; eşleşme yoksa nötr genel görsel. Öneriden seçilince kategori de öneriden gelir.
- `ProductThumb` de kayıtlı kategori yoksa adı yeniden tahmin eder; eski kayıtlar da doğru fotoğrafı alır.
- "Market yok" seçilirse `storeId = 0` yazılır: ürün sadece **Tümü** filtresinde görünür, etiketinde market yazmaz.

### Barkod tarayıcı: "Hangi marketten aldın?"
- Barkod okunup ürün bulununca ürün **otomatik eklenmiyor**; alt kartta ürün + market çipleri (Market yok / Rewe / Edeka / Aldi Süd …) çıkıyor, seçim yapıp "Listeye ekle" deniyor. Ürün bu marketin etiketiyle **alınacaklar** listesine düşüyor.
- Vazgeçilebilir; aynı barkod 2,5 sn içinde tekrar okunursa yok sayılır (mevcut davranış korundu).
- Open Food Facts ürünlerinde marka artık ada **önde** yazılıyor: "Milbona Joghurt mild" (eskiden "ad · marka").

### Sıfırlama butonları
- Liste ekranı üst bar → **⋮ → "Alışveriş verilerini sıfırla"**: liste, alışveriş geçmişi/istatistikler ve kayıtlı ürünler + fiyatlar silinir. Market ve kategori ayarları ile Pfand kayıtları korunur. (Sıfırlama sonrası örnek veri geri gelmez — temiz liste.)
- Pfand ekranı üst bar → çöp kutusu simgesi → **"Pfand verilerini sıfırla"**: bekleyen + iade edilmiş tüm Pfand kayıtları silinir (kayıt yoksa simge görünmez).

### Örnek veriler
- İlk kurulum verisi artık marka adlarıyla: MILSANI Joghurt 3,5% Fett (Aldi Süd), MILPRIMA Joghurt mild 3,5% (Penny), Saskia Mineralwasser 6×1,5 l Kasten (Lidl, Pfand 1,50 €), ja! H-Milch 1,5% (Rewe), GOLDÄHREN Toast 500 g (Aldi Süd).

### Doğrulama
- `assembleDebug` ve `assembleRelease` → BUILD SUCCESSFUL; release aynı anahtarla imzalı (SHA-256 `a596e8f9…`).
- Emülatörde (API 37) uçtan uca: Penny/Netto çipleri görüldü; **Aldi Süd + "joghurt" → 4 marka önerisi** ("MILSANI Joghurt nach türkischer Art 3,5% Fett" dahil), öneri seçilince kategori "Süt Ürünleri" oldu ve satır süt fotoğrafıyla listeye eklendi; ⋮ → sıfırlama onayı → liste boşaldı, market çipleri kaldı; tarayıcıda elle barkod girilerek **"Hangi marketten aldın?"** ekranı görüldü, "Aldi Süd" seçilip eklendi ("Listeye eklendi · Aldi Süd").
- Uygulama bilgisi ekranında yeni ikon ve **Einkauf** adı doğrulandı.
- Gerçek telefon: **versionCode 2 → 3 yerinde güncellendi (uninstall yok)**, veriler korundu, uygulama açıldı ve yeni ekleme sayfası telefonda doğru çizildi.
- **Not:** Telefonda hâlâ 1.1 ile gelen eski demo ürünler var (ör. "Süt 1.5% (Milch)") çünkü örnek veri yalnızca boş veritabanına yazılır; temizlemek için ⋮ → "Alışveriş verilerini sıfırla".

---

## 🚀 v2.0 — Kiler, reyon, yedekleme, grafikler, hatırlatıcılar

- **Kiler**: gerçek ekran, kritik eşik, SKT uyarısı, tek dokunuşla listeye ekleme, alışverişte otomatik stok artışı.
- **Reyon sıralaması**: 9 market için `aisleOrderJson` dolduruldu; market seçilince liste reyonlara göre gruplanır.
- **Yedekleme**: JSON dışa/içe aktarma + günlük otomatik yerel yedek + yerel yedekten geri yükleme.
- **İstatistik**: haftalık/aylık Canvas grafiği, kategori dağılımı, market karşılaştırması, fiyat trendi.
- **Bildirimler**: WorkManager günlük hatırlatıcı (fırsat SKT, kritik stok, haftalık liste) + ayar/izin ekranı.
- **Hız**: ana ekran widget'ı, market modu, listeyi paylaşma, sık alınanlar, son alışverişi tekrarla.
- **Dayanıklılık**: prospektüs önbelleği/çevrimdışı bandı, kısmi `strings.xml`, yeni saf mantık testleri.
- **Kalan borç**: tüm arayüz metinlerinin `strings.xml`'e taşınması (Almanca için gerekli), gerçek barkodla
  cihaz testi, Compose UI testleri, AGP/Kotlin sürüm yükseltmesi.

---

## 🌍 v2.3 — 11 dil + PIN/biyometrik uygulama kilidi

- **Diller:** arayüz metinleri `strings.xml`'e taşındı; Türkçe, İngilizce (ABD/UK), Almanca,
  Bulgarca, Sırpça, Basitleştirilmiş Çince, Ukraynaca, Rusça, Lehçe ve Felemenkçe (11 dil).
  Play dil bölmesi kapalı (`bundle.language.enableSplit = false`).
- **Dil ekranı:** aramalı liste, sistem dili seçeneği ve anında uygulama; API 33+ için
  `LocaleManager.applicationLocales`, eski sürümlerde `attachBaseContext`.
- **Uygulama kilidi:** isteğe bağlı 4 haneli PIN (PBKDF2-HMAC-SHA256, 150.000 iterasyon,
  rastgele tuz) ve kayıtlıysa parmak izi. 5 hatalı denemede 30 sn kilit; 3 dakika arka planda
  kalınca yeniden kilitlenir. Geri tuşu kilidi kapatmaz.
- **Sürüm:** versionCode 14 / versionName 2.3.
- **Yatay yerleşim:** 411 dp yüksekliğindeki yatay telefonda geniş "Barkod tara" FAB'ı özet
  kartındaki "Alışverişi tamamla" düğmesini örtüyordu. Kısa ekranlarda tarama eylemi üst çubuğa
  taşındı ve FAB gizlendi; dikeyde FAB korunuyor. Ayrıca yatayda mağaza filtreleri, sık alınanlar
  ve özet kartı listenin başlık öğesi olarak kaydırılıyor; yukarı kaydırınca liste tam yüksekliğe
  açılıyor. Başlık ekrandan çıkınca "Alışverişi tamamla" üst çubukta tonal onay simgesi olarak
  kalıyor; böylece liste sonuna kadar kaydırılsa da eylem erişilebilir. Döndürmede liste başa
  dönüyor. İki yönde doğrulandı (`screenshots/api36-landscape-home.png`,
  `screenshots/api36-landscape-scrolled.png`, `screenshots/api36-landscape-compact-complete.png`,
  `screenshots/api36-portrait-home.png`). Bu akış `ShoppingCompactCompleteTest` ile otomatikleştirildi:
  başlık kaydırma, kompakt ✓, tamamlama diyaloğu ve dikey ⇄ yatay döndürmede listenin başa dönmesi.
- **Doğrulama:** 145 birim testi, 17/17 cihaz testi (API 36 16 KB: Migration 5, AppLock 9,
  BackupRestore 2, Compose yerleşim 1), lint 0 hata; release APK'da dil değişimi EN → DE,
  PIN/biyometrik kilit, 90° döndürmede oturum koruma ve yatayda başlık kaydırma elle doğrulandı.

---

## 🚀 Yayın hazırlığı — Play AAB, hata testleri ve CI kapıları (25 Ekim 2026)

- **Play AAB:** `bundleRelease` artık release iş akışında APK ile birlikte üretiliyor;
  `jarsigner` ile imza ve `zipalign -P 16` ile 16 KB hizalama doğrulanıyor. R8 `mapping.txt`
  her sürümde artifact olarak saklanıyor. Yerel doğrulama: `app-release.aab` imzalı.
- **Yedek güvenliği:** `BackupValidator` artık yalnızca tablo boyutunu değil, her satırın
  zorunlu alanlarını, alan tiplerini, pozitif kimlikleri ve yinelenen kimlikleri de denetliyor.
  `BackupRestoreTest` bozuk/eksik/gelecek şemalı/aşırı büyük yedeklerin mevcut veriyi silmediğini,
  geçerli yedeğin ise fazladan kayıtları temizleyerek geri yüklendiğini doğruluyor.
- **Ağ hataları:** `RetryInterceptorTest` (MockWebServer) 500→200 tekrarını, 429 `Retry-After`
  davranışını, POST'un asla tekrarlanmadığını ve 3 deneme sınırını doğruluyor.
- **Uygulama kilidi:** kilit süresi dolma yolu ve bozuk PIN özetiyle asla açılmama testleri eklendi.
- **CI:** `ci.yml` içine API 36 / 16 KB Play Store imajlı emülatör işi eklendi; release işi
  artifact üretmeden önce birim testi + lint kapısından geçiyor. Ayrıntılı dış adımlar
  `docs/PLAY_YAYIN_KONTROL_LISTESI.md` içinde.

---

## 🎨 Görünüm temaları — 5 palet + 4 arayüz (25 Ekim 2026)

- **Renk paletleri:** Sistem (zümrüt, açık/koyu), Beyaz (mavi vurgu), Siyah (saf OLED +
  lime), Gece (mor/lavanta) ve Yeşil (orman yeşili). Her palet kendi yüzey, kap, hata ve
  kontur renklerini tanımlar; sistem çubuklarının ikon kontrastı da palete göre ayarlanır.
- **Arayüz stilleri:** Modern (mevcut yuvarlak kartlar), Klasik (serif tipografi, ince
  çizgiler, fiş görünümlü özet), Kompakt (tek satırlık yoğun liste, etiketsiz alt bar) ve
  Vitrin (iki sütunlu büyük görsel katalog). Tipografi ve köşe ölçeği stil başına değişir;
  ana liste, Kiler, Cebim ve Prospekt/Fırsatlar ekranları kendi stil bileşenlerini kullanır,
  market modu işlevsel olarak korunur.
- **Seçim ekranı:** `feature/theme/ThemeScreen.kt` canlı önizleme, palet swatch'ları ve
  stil tel kafesleri gösterir. Seçim `einkauf_theme` SharedPreferences'ında saklanır;
  `ThemeStore` durumu Compose'da anında yayılır, süreç yeniden başlayınca korunur.
- **Erişilebilirlik:** Tamamla eylemi tüm stillerde `SUMMARY_COMPLETE` etiketiyle, Vitrin'de
  ayrıca üst çubukta erişilebilir kalır; 11 dilde tema adı/açıklaması eklendi.
- **Doğrulama:** 145 birim testi, 18/18 cihaz testi (yeni `ThemeSelectionTest` dahil),
  lint 0 hata. API 36 16 KB emülatörde beş palet ve dört stil açılıp ekran görüntüleriyle
  kontrol edildi (`screenshots/theme-*.png`).

---

## 🔐 Klon/kurcalama koruması, premium lisans ve PDF rapor (25 Ekim 2026)

- **İmza doğrulama:** `AppIntegrity`, çalışan APK'nın sertifika özetini yükleme anahtarı
  ve Play App Signing anahtarıyla karşılaştırır. Başka anahtarla yeniden imzalanmış
  (klonlanmış) APK çalışır ama premium açılmaz; kullanıcı verisini kaybetmez.
- **Premium lisansı:** `PremiumLicense`, `BASKIT1.<payload>.<imza>` biçimindeki ECDSA P-256
  imzalı token'ı doğrular; uygulamada yalnızca açık anahtar var. `PremiumGate` premium'u
  yalnızca (1) imza güvenilir, (2) hata ayıklama/emülatör/Frida-Xposed izi yok ve
  (3) lisans geçerli olduğunda açar. Yerelde `true` dönen bir bayrak yok; SharedPreferences
  kurcalansa bile imza üretilemediği için premium açılmaz.
- **Ayar ekranı:** "Premium" kartı durumu (etkin/kilitli/kurcalanmış/süresi dolmuş) gösterir,
  lisans anahtarını yapıştırıp etkinleştirmeye izin verir. Geçersiz anahtar kaydedilmez.
- **Gizli anahtar:** `license/baskit-license-private.pem` yalnızca çevrimdışı; `.gitignore`
  ve CI'daki `Guard signing and licence secrets` adımı depoya girmesini engeller.
- **Rapor:** hareket, Pfand ve kiler tabloları; `ReportPdfWriter` ile çevrimdışı PDF üretilip
  FileProvider üzerinden paylaşılır (`baskit-rapor-<tarih>.pdf`).
- **Doğrulama:** 173 birim testi (10 yeni lisans testi), 30/30 cihaz testi (yeni
  `PremiumGateTest`, `PremiumCardTest`), lint 0 hata, imzalı APK+AAB. Emülatörde rapor →
  PDF paylaşım sayfası ve premium kartı elle doğrulandı.
- **Boyut:** AAB 18,4 MB; arm64 telefonda Play indirmesi ~9,5 MB (ML Kit ABI bölünmesi).

---

## 🧾 Pfand iade kısayolu — "Tümünü iade et" (25 Ekim 2026)

- **Kısayol:** İade diyaloğunda tutar alanının altına "Tümünü iade et" eklendi; tek
  dokunuşla bekleyen toplam (`PfandAmount.format`) yazılır ve plan özeti anında
  "N adet iade · kalan 0" gösterir. Bekleyen kayıt yoksa düğme pasiftir.
- **Tek kaynak:** Kısayolun yazdığı tutar `PfandReturnPlanner.unitCents` ile
  hesaplanan bekleyen toplamdan gelir; planlayıcının "tamamı istendi" hızlı yolu
  devreye girdiği için tamamı iade edilir, kayan nokta farkı kalmaz.
- **Ondalık ayırıcı:** Alan artık cihazın/uygulama dilinin ayırıcısını gösteriyor
  (`PfandAmount.separatorFor`): tr/de/nl'de "25,00", en/zh'de "25.00". Virgül de
  nokta da yazılabiliyor, değer seçili ayırıcıya normalize edilip aynı sente çözülüyor;
  kısayol da aynı ayırıcıyı kullanıyor.
- **Çeviri:** 11 dilde eklendi (`pfand_return_all`).
- **Doğrulama:** Yeni birim testi (`PfandAmount.format` gidiş-dönüş) ve iki Compose
  testi (kısayol 25,00 € → 100 adet → 2.500 sent onayı; kayıt yokken pasiflik).
  32 cihaz testi geçti; emülatörde 100 × 0,25 € için kısayol → "Take return" →
  bekleyen 0 € ve iade geçmişi 25,00 € olarak elle doğrulandı.

---

## 🍅 Ürün avatarları ve otomatik ürün tanıma (25 Ekim 2026)

- **Sorun:** Liste her ürün için aynı 🛒 simgesini gösteriyordu. Eski eşleştirme tek
  dizede alt dize arıyordu; bu yüzden `ei` (yumurta) "Reiniger" içinde eşleşebiliyor,
  tanınmayan her ürün de arabaya düşüyordu.
- **Katalog:** `ProductAvatars` 60'tan fazla ürün grubunu (meyve, sebze, fırın, et,
  süt ürünleri, içecek, atıştırmalık, temizlik, kozmetik, evcil hayvan, araç gereç…)
  Türkçe/Almanca/İngilizce anahtar sözcüklerle tanımlar. Grup hem emojiyi hem de
  mevcut 5 kategori kimliğini taşır; ürün eklerken kategori de buradan seçilir.
- **Eşleştirme:** Tam kelime > kelime başı > Almanca bileşik sonu ("Vollkorn-brot") >
  Türkçe ek ("Ekmeği" ≈ "ekmek") > kısaltma. İki kelimeli kalıplar tek kelimeyi yener
  ("Elma suyu" → meyve suyu, "Maden suyu" → su). `ei` gibi kısa sözcükler yalnız tam
  kelime olarak eşleşir.
- **Seçim:** Listede simgeye dokunmak `ProductAvatarPickerDialog`'u açar; simgeler
  kategori başlıkları altında ızgarada listelenir, "Otomatik" seçimi temizler. Seçim
  `products.avatarKey` kolonunda saklanır (Room v8 göçü, yedekte isteğe bağlı alan);
  boşsa simge yine addan, o da yoksa kategoriden üretilir. Bu yüzden eski kayıtlar da
  düzeltmeden sonra doğru simgeyi alır.
- **Görsel:** Simge artık kutunun boyuna göre ölçeklenir (kompakt 30 dp satırda taşmıyor,
  vitrin ızgarasında büyüyor); barkodla bulunmuş gerçek ürün fotoğrafı varsa fotoğraf kalır.
- **Çeviri:** Seçim başlığı 11 dilde eklendi (`s_avatar_sec`).
- **Doğrulama:** 215 birim testi (yeni `ProductAvatarsTest`: tanıma, kısa sözcük tuzağı,
  kalıp/ek eşleşmesi, kullanıcı seçiminin önceliği), Room 7→8 göç testi ve yeni cihaz
  testi (`ProductAvatarPickerTest`: 🍅 → dokun → 🧀 seç → veritabanı ve liste güncel →
  "Otomatik" → tekrar 🍅). 35/35 cihaz testi, lint 0 hata.
