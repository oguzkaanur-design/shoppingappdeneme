# Yasal sayfaları yayınlama

Bu klasördeki üç HTML dosyası kendi kendine yeterlidir (harici font/script yok):

- `index.html` — genel bakış
- `privacy.html` — gizlilik politikası
- `terms.html` — kullanım şartları

## GitHub Pages ile yayınlama

1. Depoyu GitHub'a gönder.
2. **Settings → Pages → Build and deployment → Deploy from a branch** seç.
3. Branch: `main`, klasör: `/docs` seç ve kaydet.
4. Birkaç dakika sonra sayfalar şu adreste açılır:
   `https://<kullanıcı>.github.io/<depo>/legal/privacy.html`

Play Console'a girilecek gizlilik politikası URL'si `.../legal/privacy.html` olmalı.
Mağaza listesindeki "Privacy policy" alanına ve uygulama içi **Gizlilik ve koşullar**
ekranıyla aynı metne işaret etmelidir.

## Yayından önce değiştirilmesi gerekenler

- **İletişim:** Sayfalarda geliştirici e-postası uydurulmadı; "Play listesindeki
  geliştirici e-postası" ve GitHub Issues'a yönlendiriliyor. Play Console'da
  izlenen bir e-posta tanımla; istenirse bu sayfalara da ekle.
- **Veri sahibi bilgisi:** Play hesabındaki geliştirici adı/şirket bilgisiyle
  eşleştiğinden emin ol.
- **Tarih:** Metin değişirse `Last updated` alanını güncelle.
- **Almanca:** Mağaza dili Almanca ise `privacy.html` ve `terms.html` için Almanca
  sürüm eklemen önerilir; uygulama içi metinler zaten TR/EN/DE.

## Play Console Data Safety notları

- Veri toplanmıyor, satılmıyor, reklam için kullanılmıyor.
- Barkod, işlevsellik için `world.openfoodfacts.org` adresine gönderiliyor;
  "App functionality" amacıyla üçüncü taraf paylaşımı olarak beyan edilmeli.
- Kamera ve bildirim izinleri isteğe bağlı.
- Kullanıcı verisini uygulama içinden silebiliyor ("Tüm verileri sil").
