#!/usr/bin/env bash
# Screenshots of the demo build for the READMEs: tools/capture.sh <out-dir> en|ru
# Build it with `./gradlew assembleDebug -Pdemo -Pshots` (app.stele.shots, never someone's demo).
# Reseeds it in that language, then walks the main screens with guarded taps.
set -e
cd "$(dirname "$0")/.."
source tools/phone.sh
OUT="$1"; LANG_="${2:-en}"
PKG=app.stele.shots
mkdir -p "$OUT"
SHOTS="$OUT"
case "$PKG" in *.demo|*.shots) ;; *) echo "refusing to clear $PKG"; exit 1 ;; esac
"$ADB" shell pm clear $PKG >/dev/null
"$ADB" shell cmd locale set-app-locales $PKG --locales "$LANG_"
"$ADB" shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 10
if [ "$LANG_" = ru ]; then
  T_CLEAN="Начать расчистку"; T_SURE="Скорее да"; T_DIGS="Раскопы"; T_ES="Испанский"; T_KITCHEN="Кухня"; T_BACK="Назад"
  T_MUSEUM="Музей"; T_JOURNAL="Журнал"; T_FORGE="Подделки"; T_DOOR="Запечатанная дверь"; T_KNOCK="Постучать"; T_SHOW="Показать ответ"; T_FIND="la madrugada"
  TEXT="Llegamos a casa de madrugada. Ojalá llueva mañana, porque mañana empieza el viaje con la maleta nueva y el billete en la mano."
else
  T_CLEAN="Start cleaning"; T_SURE="Fairly sure"; T_DIGS="Digs"; T_ES="Spanish"; T_KITCHEN="The kitchen"; T_BACK="Back"
  T_MUSEUM="Museum"; T_JOURNAL="Journal"; T_FORGE="Forgeries"; T_DOOR="The sealed door"; T_KNOCK="Knock"; T_SHOW="Show the answer"; T_FIND="la madrugada"
  TEXT="Llegamos a casa de madrugada. Ojalá llueva mañana, porque mañana empieza el viaje con la maleta nueva y el billete en la mano."
fi
up() { guard $PKG && "$ADB" shell input swipe 540 1900 540 800 400; sleep 1; }
back() { tap_text $PKG "$T_BACK"; sleep 2; }
shot 1-museum
sleep 3; tap_text $PKG "$T_CLEAN"; sleep 3
# The first tap can land while the demo is still being laid out: try once more.
"$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
"$ADB" exec-out cat /sdcard/ui.xml | grep -q "$T_SURE" || { tap_text $PKG "$T_CLEAN"; sleep 3; }
shot 2-clean
tap_text $PKG "$T_SURE"; sleep 3; shot 3-revealed
back
tap_text $PKG "$T_DIGS"; sleep 2
tap_text $PKG "$T_ES"; sleep 3; shot 4-dig
tap_text $PKG "$T_KITCHEN"; sleep 3; shot 5-vessel
back
up; up
tap_text $PKG "$T_FIND"; sleep 3; shot 6-find
back; back
tap_text $PKG "$T_MUSEUM"; sleep 2
up; up
tap_text $PKG "$T_FORGE"; sleep 3
tap_text $PKG "avergonzado" || tap_text $PKG "la salida" || true
sleep 2; shot 7-forgery
back
guard $PKG && "$ADB" shell am start -a android.intent.action.SEND -t text/plain -n $PKG/app.stele.MainActivity --es android.intent.extra.TEXT "'$TEXT'" >/dev/null 2>&1
sleep 4; shot 8-text
back
tap_text $PKG "$T_JOURNAL"; sleep 3; shot 9-journal
tap_text $PKG "$T_MUSEUM"; sleep 2
"$ADB" shell input swipe 540 700 540 2000 300; sleep 1
tap_text $PKG "$T_DOOR"; sleep 3
tap_text $PKG "$T_KNOCK"; sleep 2
tap_text $PKG "$T_SHOW"; sleep 2; shot 10-door
echo done
