#!/usr/bin/env bash
# GitHub Actions için imza anahtarını base64'e çevirir -> keystore-base64.txt
#
# Bu dosyanın İÇERİĞİNİ sohbete yapıştırma ve repoya commit etme; sadece
# GitHub -> Settings -> Secrets and variables -> Actions bölümünde secret olarak kullan.
#
# Kullanım: bash scripts/keystore-secret-olustur.sh

set -euo pipefail

KS="keystore/shoppingapp-release.jks"
OUT="keystore-base64.txt"

[ -f "$KS" ] || { echo "Bulunamadı: $KS"; exit 1; }

# Windows'ta base64 satır sonu eklenmesin diye tek satıra indiriyoruz.
base64 < "$KS" | tr -d '\r\n' > "$OUT"

echo "Oluştu: $OUT ($(wc -c < "$OUT") bayt)"
echo
echo "GitHub'da Settings -> Secrets and variables -> Actions -> New repository secret:"
echo "  1) KEYSTORE_BASE64    <- $OUT dosyasının tüm içeriği"
echo "  2) KEYSTORE_PASSWORD  <- keystore.properties: storePassword"
echo "  3) KEY_ALIAS          <- keystore.properties: keyAlias"
echo "  4) KEY_PASSWORD       <- keystore.properties: keyPassword"
