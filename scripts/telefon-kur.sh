#!/usr/bin/env bash
# Telefona KABLOSUZ kurulum / güncelleme (Android 11+ "Kablosuz hata ayıklama").
#
# Kullanım:
#   bash scripts/telefon-kur.sh servisler                       # ağdaki telefonu bul (mDNS)
#   bash scripts/telefon-kur.sh esle 192.168.1.34:37123 123456  # ilk kez eşleştir
#   bash scripts/telefon-kur.sh kur 192.168.1.34:55555          # kur veya güncelle (-r: veri korunur)
#
# Not: Bilgisayar ve telefon aynı Wi-Fi ağında olmalı. Eşleştirme bir kez yapılır,
# sonraki güncellemelerde sadece "kur" yeterlidir (port her açılışta değişebilir).

set -euo pipefail

APK="app/build/outputs/apk/release/app-release.apk"
PKG="com.shoppingapp"

kullanim() {
  sed -n '2,7p' "$0"
  exit 1
}

[ $# -ge 1 ] || kullanim

case "$1" in
  servisler)
    echo "Ağdaki adb servisleri (telefonda Kablosuz hata ayıklama açık olmalı):"
    adb mdns services
    ;;

  esle)
    [ $# -ge 3 ] || kullanim
    adb pair "$2" "$3"
    echo
    echo "Eşleşti. Şimdi telefonun Kablosuz hata ayıklama ekranındaki"
    echo "'IP adresi ve bağlantı noktası' değerini kullan:"
    echo "  bash scripts/telefon-kur.sh kur <IP:PORT>"
    ;;

  kur)
    [ $# -ge 2 ] || kullanim
    [ -f "$APK" ] || { echo "APK yok: $APK  ->  ./gradlew :app:assembleRelease"; exit 1; }
    adb connect "$2"
    echo
    echo "Kuruluyor: $APK"
    adb -s "$2" install -r "$APK"
    echo
    echo "Telefondaki sürüm:"
    MSYS_NO_PATHCONV=1 adb -s "$2" shell dumpsys package "$PKG" | grep -E "versionCode|versionName" | head -2
    ;;

  *) kullanim ;;
esac
