# Baskit premium lisansı

Premium, yerel bir `true` bayrağıyla değil, **satıcının çevrimdışı EC P-256 özel
anahtarıyla imzalanmış bir lisansla** açılır. Uygulamada yalnızca açık anahtar
(`app/src/main/java/com/shoppingapp/security/PremiumLicense.kt`) bulunur.

- `baskit-license-private.pem` — **asla git'e eklemeyin**. Bu dosya kaybolursa daha
  önce verilmiş lisansların yenisi üretilemez; şifre yöneticisi + çevrimdışı yedek tutun.
- `baskit-license-public.b64` — uygulamaya gömülen açık anahtar (X.509/DER, base64).

## Lisans üretme

```bash
tools/sign-license.sh ada@example.com 365 pro
```

Çıktı tek satırlık bir token'dır (`BASKIT1.<payload>.<imza>`). Token'ı kullanıcıya
iletin; uygulama premium etkinleştirme ekranı eklendiğinde `PremiumGate.activate`
ile doğrulanıp saklanacak.

## Anahtarı yeniden üretme (yalnızca ilk kurulum)

```bash
openssl ecparam -name prime256v1 -genkey -noout -out license/baskit-license-private.pem
openssl ec -in license/baskit-license-private.pem -pubout -outform DER \
  | base64 | tr -d '\n' > license/baskit-license-public.b64
```

Yeni açık anahtarı `PremiumLicense.PUBLIC_KEY_B64` sabitine yazın ve uygulamayı
güncelleyin. Eski anahtarla imzalanmış lisanslar geçersiz olur.

## Sınırlar

Bu, sunucusuz bir uygulamada kopya APK'ların premium çalmasını engeller; kararlı bir
tersine mühendis imza kontrolünü yamalayabilir. Ücretli abonelik geldiğinde Play
Billing + sunucu tarafı doğrulama (ve istenirse Play Integrity) kullanın.
