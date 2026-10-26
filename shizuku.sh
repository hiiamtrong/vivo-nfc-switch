#!/bin/sh
# Khởi động Shizuku từ máy tính sau mỗi lần reboot điện thoại. Cần adb đã kết nối (xem README).
set -e
APK=$(adb shell pm path moe.shizuku.privileged.api | sed 's/package://' | tr -d '\r')
adb shell "$(dirname "$APK")/lib/arm64/libshizuku.so"
sleep 2
if adb shell ps -A | grep -q shizuku_server; then echo "Shizuku đang chạy"; else echo "Shizuku chưa chạy"; exit 1; fi
