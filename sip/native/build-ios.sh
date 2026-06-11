#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <konan_target> <output_dir>" >&2
  exit 1
fi

KONAN_TARGET="$1"
OUTPUT_DIR="$2"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$SCRIPT_DIR/build/ios-${KONAN_TARGET}"

case "$KONAN_TARGET" in
  ios_arm64)
    SDK=iphoneos
    ARCH=arm64
    ;;
  ios_simulator_arm64)
    SDK=iphonesimulator
    ARCH=arm64
    ;;
  ios_x64)
    SDK=iphonesimulator
    ARCH=x86_64
    ;;
  *)
    echo "Unsupported Konan target: $KONAN_TARGET" >&2
    exit 1
    ;;
esac

SYSROOT="$(xcrun --sdk "$SDK" --show-sdk-path)"

cmake -S "$SCRIPT_DIR/ios" -B "$BUILD_DIR" \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_SYSTEM_NAME=iOS \
  -DCMAKE_OSX_SYSROOT="$SYSROOT" \
  -DCMAKE_OSX_ARCHITECTURES="$ARCH" \
  -DCMAKE_OSX_DEPLOYMENT_TARGET=13.0 \
  -DCMAKE_POLICY_VERSION_MINIMUM=3.5

cmake --build "$BUILD_DIR" --config Release --parallel "$(sysctl -n hw.ncpu 2>/dev/null || echo 4)"

mkdir -p "$OUTPUT_DIR"
cp "$BUILD_DIR/libtelephony.a" "$OUTPUT_DIR/"
