# Gelişmiş Kişisel Market & Alışveriş Uygulaması (Android - Almanya / Münih Özel) - Tasarım Dokümanı

## 1. Genel Bakış ve Amaç
Bu uygulama, Bring! benzeri görsel ve pratik alışveriş listesi konseptini temel alan, Almanya / Münih şartlarına ve kişisel kullanıma özel olarak uyarlanmış yerel (offline-first) bir Android uygulamasıdır.

### Temel Hedefler:
- **Görsel & Hızlı Kullanım:** Bring! tarzı büyük simgeli, grid yapısında, tek dokunuşla sepete atılan/çıkarılan modern Jetpack Compose arayüzü.
- **Almanya / Münih Market & Reyon Düzeni:** Münih'te yaygın marketler (Edeka, Rewe, Lidl, Aldi Süd, Kaufland, dm, Rossmann, Penny, Netto, Bio Company vb.) için özel reyon sıralaması ve markete göre özelleştirilebilir liste.
- **Pfand (Depozito) Takibi:** Almanya'ya özel plastik/cam şişe ve kutu depozito (Pfand - 0,25€ / 0,15€ / 0,08€) tutarlarını ve iade edilen Pfand hesaplamasını otomatik takip etme.
- **Barkod Tarama & Web Entegrasyonu:** ML Kit ve Open Food Facts (Almanya/Avrupa veritabanı) ile taranan EAN barkodundan otomatik ürün bilgisi (isim, resim, kategori, tahmini Euro fiyatı) çekme.
- **Fiyat & Harcama Takibi (€ - Euro):** Ürün bazlı fiyat geçmişi, market karşılaştırması (örn: Rewe vs. Lidl fiyat farkı) ve Euro cinsinden aylık/haftalık harcama grafikleri.
- **Kiler / Stok Yönetimi (Vorratskammer / Pantry):** Evdeki stok seviyesini takip etme, son kullanma tarihi uyarısı ve stoğu azalan ürünlerin otomatik alışveriş listesine aktarılması.
- **Kişisel & Yerel Depolama (Offline-First):** Sunucu/üye girişi zorunluluğu olmayan, tüm verilerin cihaz içinde Room DB ile saklandığı yüksek performanslı yapı.
- **Dil Desteği:** Türkçe ve Almanca dil / kategori etiketi uyumluluğu.

---

## 2. Sistem Mimarisi ve Teknoloji Yığını

### 2.1 Teknoloji Yığını
- **Dil & Platform:** Kotlin (Android Native)
- **Kullanıcı Arayüzü:** Jetpack Compose + Material3 + Accompanist (Animasyonlar ve grid tasarımları)
- **Mimari:** MVVM (Model-View-ViewModel) + Clean Architecture
- **Yerel Veritabanı:** Room Database (SQLite) + Flow (Reaktif veri akışı)
- **Bağımlılık Enjeksiyonu:** Hilt / Koin
- **Barkod Tarama:** Google ML Kit Barcode Scanning + CameraX (EAN-13 / EAN-8 desteği)
- **Ağ Entegrasyonu:** Retrofit + Gson (Open Food Facts DE & Web servisleri için)
- **Görsel Yükleme:** Coil (Görsel önbellekleme ve barkod ürün resimleri için)
- **Grafik & Analiz:** MPAndroidChart veya Compose Canvas bazlı harcama grafikleri

---

## 3. Temel Modüller ve Özellik Detayları

### 3.1 Alışveriş Listesi Modülü (Bring! Stil - Münih Özel)
- **Grid / Kart Arayüzü:** Renkli kategoriler, büyük ikonlar/görseller, ürün miktarı belirleme.
- **Münih Market Filtresi:** Seçilen markete göre (örn: *Edeka Münih-Hbf* veya *Lidl*) listeyi sadece o markette satılan veya o marketin reyon dizilimine göre sıralama.
- **Hızlı Durum Değişimi:**
  - *Einkaufsliste (Alınacaklar)* -> *Im Einkaufswagen (Sepettekiler)* -> *Vorratskammer (Kiler / Geçmiş)*.
- **Akıllı Tahmin:** Sık alınan Almanca/Türkçe ürünler (Obst & Gemüse, Molkereiprodukte, Fleisch vb.) en üstte yer alır.

### 3.2 Barkod Okuyucu & Akıllı Ürün Ekleme (EAN-13 Almanya)
- **Kamera ile Tarama:** Kamerayı açıp barkod okutulduğunda:
  1. Yerel veritabanında barkod aranır.
  2. Bulunamazsa Open Food Facts DE (Almanya) API üzerinden ürün adı, marka, kategori ve görsel çekilir.
  3. Kullanıcıya Euro fiyat bilgisi ve varsa Pfand miktarı girilerek ürün tek tıkla sepete eklenir.

### 3.3 Germany/Münih Market & Reyon Yönetimi
- **Varsayılan Marketler:** Edeka, Rewe, Lidl, Aldi Süd, Kaufland, dm-drogerie, Rossmann, Penny.
- **Reyon Sıralama (Aisle Mapping):** Her market için reyon sırası belirlenebilir (Örn: Rewe için -> Obst & Gemüse -> Kühlung -> Konserven -> Getränke -> Drogerie). Alışveriş sırasında liste bu sıraya göre sıralanır.
- **Fiyat Karşılaştırması (€):** Bir ürünün farklı marketlerdeki son kaydedilen fiyatları listelenir ("Milka Schokolade: Lidl'da 1,29 €, Rewe'de 1,49 €").

### 3.4 Pfand (Depozito) Yönetim Sistemi
- **Pfand Türleri:**
  - Plastik Şişe / Kutular: 0,25 €
  - Cam Bira Şişeleri: 0,08 € veya 0,15 €
  - Kasa (Kiste): 1,50 € - 3,10 €
- **Hesaplama:** Alışveriş tamamlandığında toplam tutardan iade edilen Pfand tutarı düşülür veya toplam harcamaya eklenen Pfand ayrıca raporlanır.

### 3.5 Kiler / Stok Yönetimi (Vorratskammer)
- **Evdeki Stoklar:** Un, Şeker, Süt, Kahve gibi sürekli bulundurulan ürünlerin stok miktarları.
- **Kritik Eşik:** Stok belirlenen eşiğin altına düştüğünde otomatik olarak Alışveriş Listesine öneri gönderilir.
- **Münih Yerel Ürün Kategorileri:** Bio ürünler, Bäckerei, Getränke, Süßwaren vb.

### 3.6 Fiyat ve Harcama Analitiği (€ Euro)
- **Alışveriş Tamamlama:** "Einkauf abschließen" (Alışverişi Bitir) butonuna basıldığında toplam tutar ve Pfand hesaplanır, tarihçe kaydedilir.
- **Raporlama:**
  - Aylık/Haftalık Euro harcaması.
  - Market bazlı harcama dağılımı (Rewe, Aldi Süd, Edeka vb.).
  - En çok harcama yapılan kategoriler.

---

## 4. Veritabanı Şeması (Room Entities)

1. **`Product` (Ürünler):** `id`, `name`, `barcode`, `categoryId`, `imageUrl`, `unit` (kg, Stück, L, Packung), `defaultStoreId`, `pfandAmount`
2. **`Category` (Kategoriler):** `id`, `nameDe`, `nameTr`, `iconName`, `colorHex`
3. **`Store` (Marketler):** `id`, `name` (Edeka, Rewe, Lidl vb.), `colorHex`, `aisleOrderJson`
4. **`ListItem` (Aktif Alışveriş Listesi):** `id`, `productId`, `storeId`, `quantity`, `isPurchased`, `addedAt`
5. **`PantryItem` (Stok/Kiler):** `id`, `productId`, `currentQuantity`, `minThreshold`, `expiryDate`
6. **`PurchaseHistory` (Geçmiş Alışverişler):** `id`, `storeId`, `totalAmount`, `pfandTotal`, `purchaseDate`
7. **`PurchaseHistoryItem` (Geçmiş Detay):** `id`, `historyId`, `productId`, `priceEuro`, `quantity`

---

## 5. Proje Ekran Haritası (UI Flow)

1. **Ana Ekran (Einkaufsliste / Alışveriş Listesi):**
   - Üstte Market Seçici / Filtresi (Edeka, Rewe, Lidl, Aldi Süd...).
   - Kategorilere göre gruplanmış Bring! tarzı görsel grid.
   - Alt Kısım: Sepet Özeti (€) & "Alışverişi Tamamla" Butonu + Barkod Tarayıcı FAB.
2. **Barkod Tarayıcı Ekranı:**
   - Canlı kamera görüntüsü, EAN barkod okuma, taranan Almanya ürünü bilgisi popup'ı, Euro fiyatı girip ekleme.
3. **Pfand & Harcama Ekranı:**
   - İade edilecek Pfand şişe sayaçları ve harcama istatistikleri.
4. **Kiler / Stok Yönetimi (Vorratskammer):**
   - Evdeki ürünler, stok durum barları, "Alışveriş Listesine Ekle" aksiyonları.
5. **Marketler & Reyon Düzenleyici:**
   - Market ekleme/düzenleme ve reyon sıralamasını sürükle-bırak ile ayarlama.
