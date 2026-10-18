#!/usr/bin/env bash
# One step of a manual walk on the phone: tools/look.sh <name> [tap text] — taps (guarded), waits, saves a small copy to look at.
cd "$(dirname "$0")/.."
source tools/phone.sh
PKG="${PKG:-app.stele.shots}"
OUT="${OUT:-/tmp/stele-look}"; mkdir -p "$OUT"; SHOTS="$OUT"
if [ -n "$2" ]; then tap_text "$PKG" "$2" || exit 1; sleep "${WAIT:-2}"; fi
shot "$1"
python -c "from PIL import Image; im=Image.open(r'$OUT/$1.png'); im.thumbnail((450,1000)); im.save(r'$OUT/$1-s.png')"
