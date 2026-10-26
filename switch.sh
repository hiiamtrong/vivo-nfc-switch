#!/bin/sh
# Toggle the default NFC wallet over adb (WALLET role): vivo Wallet <-> Google Pay. Needs adb connected to the phone.
# shortcut: supports exactly these two wallets, edit VIVO/GOOGLE to use others.
VIVO=com.vivo.wallet
GOOGLE=com.google.android.gms
cur=$(adb shell cmd role get-role-holders android.app.role.WALLET | tr -d '\r')
[ "$cur" = "$VIVO" ] && new=$GOOGLE || new=$VIVO
adb shell cmd role add-role-holder --user 0 android.app.role.WALLET "$new" || exit 1
sleep 2
echo "NFC wallet: $cur -> $new ($(adb shell dumpsys nfc | grep 'Default route' | tr -s ' '))"
