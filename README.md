# NFC Wallet Switch

[English](README.md) | [Tiếng Việt](README.vi.md)

Switch the default NFC payment wallet on a vivo phone with one tap. Pick any two wallets (for example vivo Wallet and Google Pay) and flip between them from the app, a Quick Settings tile, or the Smart Button.

Built and tested on the vivo X300 Pro (V2502DA, OriginOS, Android 17).

## Why this exists

OriginOS keeps two payment worlds side by side. vivo Wallet stores its cards on the embedded secure element, while Google Pay uses host card emulation. Only one of them can answer a payment terminal, and changing it means digging through system settings every time.

This app turns that into a single action.

## Features

- **Two-wallet switch.** Choose which two wallets swap. The list contains vivo Wallet plus every installed app that declares a payment NFC service (Google Pay, ShopeePay, VPBank NEO, and so on).
- **Animated app screen.** Two wallet cards with a spinning switch button. The active card lights up, and a failed switch shakes the stage.
- **Quick Settings tile.** One tap switches the wallet. The tile subtitle shows the wallet currently in use.
- **Smart Button shortcuts.** Four launcher shortcuts that run without opening a screen, with a haptic tick and a toast as feedback:
  - Switch NFC wallet
  - Open current wallet
  - Use *first wallet*
  - Use *second wallet*
- **Language setting.** System default, Tiếng Việt, or English. It also appears under the system "App languages" settings.
- **Command line helpers.** `switch.sh` flips the wallet from a computer over adb, and `shizuku.sh` starts Shizuku after a reboot.

## How it works

Android decides which app answers NFC payments through the `android.app.role.WALLET` role. Changing the holder of that role changes the default wallet, and the NFC controller reroutes between the secure element (`secure element`) and the host (`host`) accordingly.

Changing the role holder requires `INTERACT_ACROSS_USERS_FULL`, a permission managed by roles that cannot be granted to a normal app with `pm grant`. The app therefore runs `cmd role add-role-holder` with shell privileges through [Shizuku](https://github.com/RikkaApps/Shizuku).

```text
App / tile / shortcut  ->  Shizuku (shell)  ->  cmd role add-role-holder WALLET <package>
```

## Requirements

- A vivo phone running OriginOS on Android 14 or newer (minSdk 34). Only the X300 Pro has been tested.
- [Shizuku](https://github.com/RikkaApps/Shizuku/releases) installed and running.
- To build: JDK 17 and the Android SDK with platform 35. The Gradle wrapper downloads Gradle itself.
- To start Shizuku from a computer: `adb` with wireless debugging enabled.

## Build and install

```sh
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Every push and pull request is built by GitHub Actions (`.github/workflows/build.yml`), and the debug APK is attached to the run as the `nfcswitch-debug-apk` artifact.

## Set up

1. Install Shizuku and start it. Either open the Shizuku app and use "Start via Wireless debugging", or connect adb to the phone and run:
   ```sh
   ./shizuku.sh
   ```
2. Open **NFC Wallet Switch**. When it asks, tap **Grant Shizuku permission** and allow it in the Shizuku dialog.
3. In **Settings** inside the app, pick the first and second wallet.

## Usage

- **In the app.** Tap the round button between the two cards.
- **Quick Settings.** Edit your tiles and add **NFC wallet**. One tap switches, and the subtitle shows the current wallet.
- **Smart Button.** In the vivo Smart Button settings, assign one of this app's shortcuts to a press type. A good split is *Switch NFC wallet* on one press and *Open current wallet* on another. Long-pressing the app icon shows the same shortcuts.

### From a computer

```sh
./switch.sh     # toggle between vivo Wallet and Google Pay over adb
./shizuku.sh    # start Shizuku after the phone reboots
```

## After a reboot

Shizuku stops when the phone restarts, because it runs without root. Start it again with the Shizuku app or `./shizuku.sh`. The default wallet, the Shizuku permission, your wallet pair, shortcuts, and language all survive the reboot.

While Shizuku is not running, the tile opens the system payment screen instead and the shortcuts show a message asking you to start Shizuku.

## Known limits

- **Power button double press.** OriginOS always opens the vivo Wallet swipe screen on a double press, whatever the default wallet is. That switch belongs to the vivo Wallet app and cannot be written from outside. If you turn it off in vivo Wallet, the double press only locks the phone, because this ROM has no standard Android double-press gesture. Use the *Open current wallet* shortcut on the Smart Button instead.
- **App jump prompt.** vivo may show a short "jump to another app" prompt when a shortcut opens a wallet.
- **Google Wallet app.** The Google Wallet app (`com.google.android.apps.walletnfcrel`) cannot hold the wallet role. Google Pay is switched through Google Play services (`com.google.android.gms`), which is what the app uses for it.
- **No terminal test.** The wallet switch was verified through the role holder and the NFC routing state. Payments at a real terminal were not tested.
- **Private API.** The app calls `Shizuku.newProcess` by reflection because it became private in Shizuku API 13.1. A future Shizuku release may require a UserService instead.

## Project layout

```text
app/src/main/java/com/hiiamtrong/nfcswitch/
  Wallet.java          role holder logic, wallet discovery, Shizuku shell calls
  MainActivity.java    animated screen, wallet pair settings, language, shortcuts
  SwitchActivity.java  no-UI target of the shortcuts
  SwitchTile.java      Quick Settings tile
  App.java             first-run language default
app/src/main/res/      strings (English default, Vietnamese in values-vi), icons, shortcuts
switch.sh              toggle the wallet over adb
shizuku.sh             start Shizuku over adb
```

## Contributing

Issues and pull requests are welcome. If you test on another vivo model or OriginOS version, please report the model, the OS version, and whether the switch and the NFC routing change worked.

## Disclaimer

This project changes system payment defaults through an unofficial route and is not affiliated with vivo, Google, or Shizuku. Use it at your own risk.
