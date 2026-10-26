#!/bin/sh
# Start Shizuku from a computer after every phone reboot. Needs adb connected to the phone (see README).
set -e
APK=$(adb shell pm path moe.shizuku.privileged.api | sed 's/package://' | tr -d '\r')
adb shell "$(dirname "$APK")/lib/arm64/libshizuku.so"
sleep 2
if adb shell ps -A | grep -q shizuku_server; then echo "Shizuku is running"; else echo "Shizuku is not running"; exit 1; fi
