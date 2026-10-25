# Einkauf v2.0 (versionCode 11)

Bu sürüm, uygulamanın yarım kalan vizyonunu tamamlıyor ve günlük kullanımdaki sürtünmeyi azaltıyor.
Veritabanı şeması değişmedi; **mevcut veriler korunur** (üzerine kurulum yapılabilir).

## Yeni

- **Kiler sekmesi** — evdeki stok, kritik eşik ve son kullanma tarihi takibi. Eşik altına düşen ürün
  tek dokunuşla alışveriş listesine eklenir; alışveriş tamamlanınca stok kendiliğinden artar.
- **Reyon sıralaması** — 9 market için reyon düzeni. Market çipi seçilince liste "Reyon 1/2/3…"
  başlıklarıyla gruplanır.
- **Yedekleme ve ayarlar** — tüm veriler tek JSON dosyasında dışa/içe aktarılabilir; her gün
  kendiliğinden alınan yerel yedekler (son 5) listeden geri yüklenebilir.
- **Derin istatistikler** — haftalık/aylık harcama grafiği, kategori dağılımı, market karşılaştırması
  (ortalama sepet) ve ürün fiyat trendi.
- **Hatırlatıcılar** — günde bir kez: süresi dolmak üzere olan fırsatlar, kritik kiler stoğu ve
  cumartesi haftalık liste hatırlatması. Ayarlardan kapatılabilir; "Bildirimleri şimdi dene" ile test edilir.
- **Ana ekran widget'ı** — alınacak ürün sayısı, ilk ürünler ve "Barkod tara" kısayolu.
- **Market modu** — rafta tek elle kullanım için büyük satırlar; satıra dokununca işaretlenir.
- **Paylaşma ve hız kısayolları** — listeyi düz metin olarak paylaş, "Sık alınanlar" şeridi,
  "Son alışverişi tekrarla".
- **Prospektüs önbelleği** — broşür servisi kapalıyken boş ekran yerine son kayıtlı liste gösterilir.

## Notlar

- Pfand sekmesi alt menüden kaldırıldı; **Cebim → Pfand** kartından açılıyor.
- `versionCode 11` — telefon paketi güncelleme olarak kabul eder, veriler kalır.
- APK aynı imza anahtarıyla imzalandı (SHA-256 `a596e8f9…a08d2`).
- Kurulum: telefonda bilinmeyen kaynaklara izin ver → APK'ya dokun → Güncelle.
