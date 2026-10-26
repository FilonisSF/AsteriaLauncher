#!/usr/bin/env bash
# Offline build of the Asteria mods (no Gradle/Loom): javac against a Yarn-named Minecraft jar, then remap to intermediary.
#   mods/toolchain/build.sh <mc version> <libs dir> <yarn mappings dir>
# <libs dir> must contain: mc-named.jar, mc-intermediary.jar (only for 1.16.5 style builds), named-*.jar (Fabric API modules),
#                          and the Minecraft/Fabric libraries (*.jar). Output: mods/dist/asteria-core-<v>.jar, asteria-visuals-<v>.jar
set -euo pipefail
V=$1; LIBS=$(cd "$2" && pwd); YARN=$(cd "$3" && pwd)
ROOT=$(cd "$(dirname "$0")/.." && pwd); OUT=$ROOT/dist; WORK=${WORK:-/tmp/asteria-build-$V}
JAVA_REL=${JAVA_REL:-8}
ASM=$(ls $LIBS/../asm/*.jar 2>/dev/null | tr '\n' ':' || true)
[ -z "$ASM" ] && ASM=/opt/gradle-8.14.3/lib/asm-9.7.1.jar:/opt/gradle-8.14.3/lib/asm-commons-9.7.1.jar:/opt/gradle-8.14.3/lib/asm-tree-9.7.1.jar
rm -rf "$WORK"; mkdir -p "$WORK/tc" "$OUT"
javac -nowarn -d "$WORK/tc" -cp "$ASM" "$ROOT/toolchain/Remap.java"
CP=$(ls $LIBS/*.jar | grep -v mc-intermediary | tr '\n' ':')
NAMED=$(ls $LIBS/mc-named.jar $LIBS/named-*.jar 2>/dev/null | tr '\n' ' ' || true)
VER=$(cat "$ROOT/VERSION" 2>/dev/null || echo 1.2.0)

build() { # $1 = core|visuals
  local M=$1 ID NAME ENTRY DEPS COMMON PKG
  if [ "$M" = core ]; then ID=asteria; NAME=AsteriaLauncher; ENTRY=ru.safimine.asteria.AsteriaClient; COMMON=$ROOT/common-core; PKG=ru.safimine.asteria.mixin
  else ID=asteria_visuals; NAME="Asteria Visuals"; ENTRY=ru.safimine.asteria.visuals.mc.VisualsClient; COMMON=$ROOT/common-visuals; PKG=ru.safimine.asteria.visuals.mc.mixin; fi
  local SRC=$ROOT/$V/$M/src/main W=$WORK/$M
  mkdir -p "$W/named" "$W/inter"
  find "$SRC/java" "$COMMON" -name '*.java' > "$W/sources.txt"
  javac -nowarn -encoding UTF-8 --release $JAVA_REL -proc:none -d "$W/named" -cp "$CP" @"$W/sources.txt"
  java -cp "$WORK/tc:$ASM" Remap mod "$YARN" "$LIBS/mc-intermediary.jar" "$W/named" "$W/inter" $NAMED
  # resources: assets + generated fabric.mod.json / mixin config
  [ -d "$SRC/resources" ] && cp -r "$SRC/resources/." "$W/inter/"
  [ -d "$COMMON/resources" ] && cp -r "$COMMON/resources/." "$W/inter/"
  local MIX=$(cd "$W/inter" && find "${PKG//.//}" -maxdepth 1 -name '*.class' 2>/dev/null | sed 's#.*/##; s#\.class$##' | grep -v '\$' | sort | sed 's/.*/"&"/' | paste -sd, -)
  local LEVEL=JAVA_8; [ "$JAVA_REL" = 17 ] && LEVEL=JAVA_17; [ "$JAVA_REL" = 21 ] && LEVEL=JAVA_21
  printf '{"required":true,"minVersion":"0.8","package":"%s","compatibilityLevel":"%s","client":[%s],"injectors":{"defaultRequire":1}}\n' "$PKG" "$LEVEL" "$MIX" > "$W/inter/$ID.mixins.json"
  local MCDEP; MCDEP=$(cat "$ROOT/$V/mcdep" 2>/dev/null || echo "$V")
  local FAPI=""; [ "$M" = visuals ] && [ -f "$ROOT/$V/needs-fabric-api" ] && FAPI=',"fabric":"*"'
  local DESC; if [ "$M" = core ]; then DESC="SafiMine / AsteriaLauncher: star in the tab list and above heads for launcher players, their skins and capes, settings from the launcher applied live."
  else DESC="PREMIUM visuals of AsteriaLauncher: HUD, Click GUI (Right Shift), trails, jump circles, target marker, china hat, hit particles, zoom, custom crosshair, fog, time, hand position. Works only with an active PREMIUM subscription."; fi
  cat > "$W/inter/fabric.mod.json" <<JSON
{"schemaVersion":1,"id":"$ID","version":"$VER","name":"$NAME","description":"$DESC","authors":["SafiMine"],"contact":{"homepage":"https://safimine.ru"},
 "icon":"assets/$ID/icon.png","environment":"client","entrypoints":{"client":["$ENTRY"]},"mixins":["$ID.mixins.json"],
 "depends":{"fabricloader":">=0.11","minecraft":"$MCDEP"$FAPI}}
JSON
  (cd "$W/inter" && rm -f "$OUT/asteria-$M-$V.jar" && zip -qr "$OUT/asteria-$M-$V.jar" .)
  echo "built $OUT/asteria-$M-$V.jar ($(du -k "$OUT/asteria-$M-$V.jar" | cut -f1) KB, mixins: $MIX)"
}
build core
[ -d "$ROOT/$V/visuals" ] && build visuals || true
