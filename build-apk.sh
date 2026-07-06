#!/usr/bin/env bash
# Genera el APK de la app. Por defecto el release firmado.
# Uso:
#   ./build-apk.sh            # release firmado (app-release.apk)
#   ./build-apk.sh debug      # debug (app-debug.apk)
set -e

PROJECT_DIR="/home/marcopg/AndroidStudioProjects/GPSTracker"
VARIANT="${1:-release}"

case "$VARIANT" in
  release)
    "$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" assembleRelease
    APK="$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk"
    ;;
  debug)
    "$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" assembleDebug
    APK="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
    ;;
  *)
    echo "Variante desconocida: $VARIANT (usa 'release' o 'debug')"
    exit 1
    ;;
esac

echo ""
echo "APK generado:"
echo "  $APK"
