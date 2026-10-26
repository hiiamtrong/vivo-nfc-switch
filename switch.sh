#!/bin/sh
# Đổi ví NFC mặc định qua adb (role WALLET): vivo Wallet <-> Google Pay. Cần: adb đã kết nối máy.
# shortcut: chỉ hỗ trợ đúng 2 ví này; muốn thêm ví khác thì sửa VIVO/GOOGLE.
VIVO=com.vivo.wallet
GOOGLE=com.google.android.gms
cur=$(adb shell cmd role get-role-holders android.app.role.WALLET | tr -d '\r')
[ "$cur" = "$VIVO" ] && new=$GOOGLE || new=$VIVO
adb shell cmd role add-role-holder --user 0 android.app.role.WALLET "$new" || exit 1
sleep 2
echo "NFC wallet: $cur -> $new ($(adb shell dumpsys nfc | grep 'Default route' | tr -s ' '))"
