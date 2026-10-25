#!/usr/bin/env bash
# Baskit premium lisansı üretir. Özel anahtar depoda tutulmaz
# (license/baskit-license-private.pem, .gitignore'da); çevrimdışı yedekleyin.
#
# Kullanım: tools/sign-license.sh <alıcı> <geçerlilik-gün> [plan]
# Örnek:    tools/sign-license.sh ada@example.com 365 pro
set -euo pipefail

SUBJECT="${1:-}"
DAYS="${2:-365}"
PLAN="${3:-pro}"
KEY="license/baskit-license-private.pem"

if [[ -z "$SUBJECT" ]]; then
  echo "Kullanım: $0 <alıcı> <geçerlilik-gün> [plan]" >&2
  exit 2
fi
if [[ ! -f "$KEY" ]]; then
  echo "Anahtar bulunamadı: $KEY" >&2
  echo "Üretmek için: openssl ecparam -name prime256v1 -genkey -noout -out $KEY" >&2
  exit 2
fi
if ! [[ "$DAYS" =~ ^[0-9]+$ ]] || [[ "$DAYS" -lt 1 ]]; then
  echo "Geçerlilik günü pozitif tam sayı olmalı." >&2
  exit 2
fi

NOW_MS=$(( $(date +%s) * 1000 ))
EXP_MS=$(( NOW_MS + DAYS * 86400000 ))
# JSON'u Python ile üret: alıcı adı kaçış karakterleri doğru yazılsın.
PAYLOAD=$(python - "$SUBJECT" "$PLAN" "$NOW_MS" "$EXP_MS" <<'PY'
import json, sys
print(json.dumps({
    "v": 1,
    "sub": sys.argv[1],
    "plan": sys.argv[2],
    "iat": int(sys.argv[3]),
    "exp": int(sys.argv[4]),
}, separators=(",", ":"), ensure_ascii=False), end="")
PY
)

B64_PAYLOAD=$(printf '%s' "$PAYLOAD" | openssl base64 -A | tr '+/' '-_' | tr -d '=')
B64_SIG=$(printf '%s' "$PAYLOAD" | openssl dgst -sha256 -sign "$KEY" \
  | openssl base64 -A | tr '+/' '-_' | tr -d '=')

printf 'BASKIT1.%s.%s\n' "$B64_PAYLOAD" "$B64_SIG"
