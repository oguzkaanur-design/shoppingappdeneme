# Einkauf v2.1

Sürüm 2.1 günlük kullanımı hızlandıran dört özellik ve uygulamanın kopyalanmasını
zorlaştıran güvenlik sertleştirmesi getirir.

## Yeni

- **Fişle tamamla** — Menü → *Fişle tamamla*. Fişin fotoğrafını çek veya galeriden
  seç; ML Kit fişi okur, ürünleri ve fiyatları çıkarır. Tutarlar iki sütunlu fişlerde
  bile doğru eşleşsin diye satırlar konuma göre yeniden kurulur. Kaydetmeden önce
  fiyatlar düzeltilebilir, satır silinebilir. Kaydedince alışveriş geçmişi, fiyat
  trendleri ve kiler stoğu normal akıştaki gibi güncellenir.
- **Paylaşımdan liste** — WhatsApp/mesajdan gelen listeyi uygulamaya paylaş;
  “süt, 2 kg un, 3x Milch” gibi satırlar ayrıştırılıp önizlenir, tek dokunuşla
  listeye eklenir.
- **Aylık bütçe** — İstatistikler ekranında limit belirle; bu ayın harcaması,
  ilerleme çubuğu ve kalan tutar görünür, limit aşılınca kırmızıya döner.
- **Barkod fiyat hafızası** — Barkod okutulduğunda o ürüne en son ödenen fiyat,
  market ve tarih gösterilir (“Son: €1,09 · Rewe · 12 Eki”).

## Güvenlik

- **İmza doğrulama**: APK farklı bir anahtarla yeniden imzalanmışsa uygulama açılmaz
  (yayın sertifikasının SHA-256 özeti sabit kodlu).
- **R8**: kod karıştırma ve kullanılmayan kodun atılması açık; hata ayıklama
  günlükleri sürüm APK'sından çıkarılıyor.
- **Yedekleme kapalı**: Android bulut yedeği ve cihaz aktarımı devre dışı; veri
  yalnızca cihazda ve uygulamanın kendi JSON dışa aktarımıyla taşınır.
- **Yalnızca HTTPS**: cleartext trafiği engellendi, sistem CA'ları kullanılıyor.

## Boyut ve doğrulama

- APK 32 MB → **22 MB** (R8 + kaynak küçültme + OCR modelinin Play Services'ten
  gelmesi).
- `testDebugUnitTest`: 117 test, 0 hata (fiş ayrıştırma, OCR satır birleştirme,
  paylaşım ayrıştırma, bütçe/analitik, migration).
- Emülatörde sürüm APK ile: paylaşımdan liste ekleme (2 ürün), bütçe limiti
  (€250,00), fiş tarama (4 ürün · €6,55 → kaydedildi, liste boşaldı, istatistik
  “Bu ay €6,55”).

## Bilinen sınırlar

- Arayüz hâlâ yalnızca Türkçe. Metinlerin büyük bölümü kod içinde; çok dillilik
  ayrı bir çalışma olarak planlandı.
- Fiş OCR'ı Play Services modelini kullanır; Play Services olmayan cihazlarda
  “Fiş okunamadı” mesajı görünür, uygulamanın kalanı çalışır.
- Barkod fiyat hafızası emülatörde uçtan uca denenmedi (manuel barkod + geçmiş
  kaydı gerektiriyor); sorgu ve gösterim derlendi, veri yolu fiş akışıyla doğrulandı.
