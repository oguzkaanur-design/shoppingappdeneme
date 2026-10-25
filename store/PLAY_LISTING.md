# Baskit — Play Store yükleme metinleri

Paket: `com.shoppingapp` · Sürüm: 2.4 (versionCode 15) · Ücretsiz, hesap yok.

> Mağazaya yüklemeden önce "Baskit" adının Play Console'da ve ilgili marka
> tescilinde uygun olduğunu kontrol edin. Paket adı korunuyor; böylece mevcut
> kurulumlar güncelleme olarak alınır ve verileri silinmez.

## Kimlik

- **Uygulama adı (30 karakter):** Baskit — Shopping List & Pantry
- **Kısa açıklama (80 karakter):** List, pantry, deposits and budget — offline, private, no account.
- **Kategori:** Alışveriş (Shopping)
- **Etiketler:** shopping list, pantry, budget, deposit, groceries

## Uzun açıklama

Baskit keeps groceries, pantry stock, bottle deposits and your cash budget in
one calm, offline-first app. No account, no ads, no tracking — your data stays
on your phone.

**Plan the trip**
- Shared shopping lists with stores, aisles, quantities and prices
- Barcode scanner and receipt OCR to add items in seconds
- Weekly flyers and a deals list with expiry dates

**Know what you have**
- Pantry stock with critical thresholds and expiry reminders
- One-tap restock: low items go straight to the shopping list
- Deposit (Pfand) tracking with partial returns and automatic cash-in

**Know what you spend**
- Cash and bank balances built from individual transactions
- Monthly budget, spending charts and store comparisons
- Report screen with transaction, deposit and pantry tables
- Export a clean PDF report and share it with family or your tax folder

**Make it yours**
- Five colour palettes (system, white, OLED black, night, green)
- Four layouts (modern, classic, compact, gallery)
- Eleven languages, optional PIN or biometric app lock

Baskit works without an internet connection. Flyers and barcode names need a
connection; everything you enter is stored locally. Back up to a JSON file and
move to a new phone whenever you like.

## Data safety formu (önerilen yanıtlar)

| Soru | Yanıt |
|---|---|
| Veri topluyor mu? | Hayır (geliştiriciye hiçbir kişisel veri gönderilmez) |
| Veri paylaşıyor mu? | Hayır |
| Veriler aktarımda şifreli mi? | Evet (yalnızca HTTPS) |
| Kullanıcı silmeyi talep edebilir mi? | Evet — uygulama içi "Tüm verileri sil" |
| Konum / kişiler / reklam kimliği | Kullanılmıyor |
| Kamera | Yalnızca barkod ve fiş okuma; görüntü cihazda işlenir |
| Üçüncü taraf | Barkod adı için Open Food Facts; broşür için Marktguru'ya ürün/market sorgusu |

## İçerik derecelendirmesi

- Şiddet, cinsellik, kumar, uyuşturucu, korku: **yok**
- Kullanıcı etkileşimi / reklam / uygulama içi satın alma: **yok** (premium altyapısı hazır, satış yok)
- Beklenen derece: **3+ / Everyone**

## Görsel varlıklar

| Varlık | Yol | Durum |
|---|---|---|
| Uygulama ikonu 512×512 | `store/play-icon-512.png` | hazır |
| Öne çıkan grafik 1024×500 | `store/feature-graphic-1024x500.png` | hazır |
| Telefon ekran görüntüleri (min 2) | `screenshots/` | yeni marka ile yeniden alınmalı |
| 7" / 10" tablet görüntüleri | `screenshots/` | isteğe bağlı, önerilir |
| Gizlilik politikası URL'si | GitHub Pages `docs/legal/privacy.html` | yayınlanmalı |

## Mağaza metni çevirileri

Play Console'da en az İngilizce ve Almanca liste oluşturun. Kısa açıklama için
öneriler:

- **DE:** Liste, Vorrat, Pfand und Budget — offline, privat, ohne Konto.
- **TR:** Liste, kiler, Pfand ve bütçe — çevrimdışı, gizli, hesapsız.
- **NL:** Lijst, voorraad, statiegeld en budget — offline, privé, zonder account.

## Yükleme öncesi son kontrol

1. `versionCode` 15'ten büyük mü?
2. AAB imzalı ve Play App Signing açık mı?
3. Gizlilik URL'si HTTPS ve erişilebilir mi?
4. Ekran görüntüleri yeni adı/simgeyi gösteriyor mu?
5. Data safety yanıtları yukarıdakiyle aynı mı?
6. Internal testing'de temiz kurulum + yükseltme testi yapıldı mı?
