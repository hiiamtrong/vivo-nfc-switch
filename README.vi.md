# Vivo NFC Switch

[English](README.md) | [Tiếng Việt](README.vi.md)

Đổi ví NFC thanh toán mặc định trên điện thoại **vivo** chỉ bằng một chạm. Chỉ hoạt động trên điện thoại vivo chạy OriginOS. Chọn hai ví bất kỳ (ví dụ vivo Wallet và Google Pay) rồi đổi qua lại từ app, ô Quick Settings hoặc Smart Button.

Được viết và kiểm tra trên vivo X300 Pro (V2502DA, OriginOS, Android 17).

## Vì sao có app này

OriginOS có hai thế giới thanh toán chạy song song. Thẻ trong vivo Wallet nằm trên secure element nhúng, còn Google Pay dùng giả lập thẻ phía host (HCE). Chỉ một bên trả lời được máy POS, và mỗi lần đổi bạn phải vào cài đặt hệ thống.

App biến việc đó thành một thao tác.

## Tính năng

- **Đổi qua lại giữa hai ví.** Bạn chọn hai ví sẽ đổi cho nhau. Danh sách gồm vivo Wallet và mọi app đã cài có dịch vụ NFC thanh toán (Google Pay, ShopeePay, VPBank NEO, v.v.).
- **Màn hình có animation.** Hai thẻ ví cùng nút đổi xoay. Thẻ đang dùng sáng lên, đổi lỗi thì khu vực hai thẻ rung.
- **Ô Quick Settings.** Chạm một lần là đổi ví. Phụ đề của ô hiện ví đang dùng.
- **Shortcut cho Smart Button.** Bốn shortcut chạy không cần mở màn hình, phản hồi bằng rung nhẹ và toast:
  - Đổi ví NFC
  - Mở ví đang dùng
  - Dùng *ví thứ nhất*
  - Dùng *ví thứ hai*
- **Cài đặt ngôn ngữ.** Theo hệ thống, Tiếng Việt hoặc English. Cũng xuất hiện trong mục "Ngôn ngữ ứng dụng" của hệ thống.
- **Công cụ dòng lệnh.** `switch.sh` đổi ví từ máy tính qua adb, `shizuku.sh` bật Shizuku sau khi khởi động lại.

## Cách hoạt động

Android quyết định app nào trả lời thanh toán NFC qua role `android.app.role.WALLET`. Đổi holder của role này là đổi ví mặc định, và bộ điều khiển NFC tự đổi đường định tuyến giữa secure element (`secure element`) và host (`host`).

Đổi holder của role cần quyền `INTERACT_ACROSS_USERS_FULL`. Quyền này do role quản lý nên không thể cấp cho app thường bằng `pm grant`. Vì vậy app chạy `cmd role add-role-holder` với quyền shell thông qua [Shizuku](https://github.com/RikkaApps/Shizuku).

```text
App / ô Quick Settings / shortcut  ->  Shizuku (shell)  ->  cmd role add-role-holder WALLET <package>
```

## Yêu cầu

- Điện thoại vivo chạy OriginOS (bản Trung Quốc) hoặc Funtouch OS (bản Global) trên Android 14 trở lên (minSdk 34). Mới chỉ kiểm tra trên X300 Pro chạy OriginOS.
- [Shizuku](https://github.com/RikkaApps/Shizuku/releases) đã cài và đang chạy.
- Để build: JDK 17 và Android SDK có platform 35. Gradle wrapper tự tải Gradle.
- Để bật Shizuku từ máy tính: `adb` với Wireless debugging đã bật.

## Build và cài đặt

```sh
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Mỗi lần push và pull request đều được GitHub Actions build (`.github/workflows/build.yml`), APK debug được đính kèm vào lần chạy dưới dạng artifact `vivo-nfc-switch-debug-apk`.

## Phát hành

Push một tag như `v1.0.0` sẽ chạy `.github/workflows/release.yml`: build APK release đã ký và đính kèm vào một GitHub release. Cần bốn secret của repo: `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` và `RELEASE_KEY_PASSWORD`.

Để ký release trên máy, tạo keystore bằng `keytool`, rồi ghi `storeFile`, `storePassword`, `keyAlias`, `keyPassword` vào file `keystore.properties` ở thư mục gốc dự án (đã git-ignore) và chạy `./gradlew assembleRelease`. APK release ký bằng khoá khác với APK debug nên phải gỡ bản debug trước khi cài.

## Thiết lập

### Chỉ trên điện thoại (không cần máy tính)

1. **Cài app.** Trên điện thoại, mở [bản phát hành mới nhất](https://github.com/hiiamtrong/vivo-nfc-switch/releases/latest), tải file `.apk` rồi mở nó. Nếu OriginOS hỏi, cho phép trình duyệt hoặc trình quản lý file được cài app không rõ nguồn gốc.
2. **Cài Shizuku.** Lấy từ Google Play hoặc từ [trang phát hành của Shizuku](https://github.com/RikkaApps/Shizuku/releases).
3. **Bật Tuỳ chọn nhà phát triển.** Vào Cài đặt, mở Giới thiệu điện thoại, chạm 7 lần vào Phiên bản phần mềm. Tên menu khác nhau giữa các bản OriginOS nên bạn cũng có thể tìm "Tuỳ chọn nhà phát triển" trong ô tìm kiếm của Cài đặt.
4. **Bật Gỡ lỗi không dây.** Kết nối Wi-Fi, mở Tuỳ chọn nhà phát triển và bật Gỡ lỗi không dây (Wireless debugging).
5. **Ghép đôi Shizuku (chỉ một lần).** Mở Shizuku, chọn "Start via Wireless debugging" rồi bắt đầu ghép đôi. Mở Gỡ lỗi không dây trong Cài đặt, bấm "Ghép nối thiết bị bằng mã ghép nối" (Pair device with pairing code) và nhập mã vào thông báo của Shizuku.
6. **Khởi động Shizuku.** Quay lại Shizuku và bấm Start. Màn hình sẽ báo Shizuku đang chạy.
7. **Cấp quyền cho app.** Mở **Đổi ví NFC vivo**, bấm **Cấp quyền Shizuku** và chọn "Allow all the time".
8. **Chọn ví.** Trong phần **Cài đặt** của app, chọn ví thứ nhất và ví thứ hai. Sau đó thêm ô **Ví NFC** vào Quick Settings hoặc gán shortcut cho Smart Button.

Nếu gặp lỗi:

- Shizuku cứ hiện "Searching for pairing service": cho phép Shizuku chạy nền trong phần cài đặt pin và tự khởi động của máy.
- Shizuku không khởi động: tắt rồi bật lại Gỡ lỗi không dây, và kiểm tra điện thoại đang dùng Wi-Fi.

### Từ máy tính (tuỳ chọn)

Kết nối adb với điện thoại và khởi động Shizuku bằng:

```sh
./shizuku.sh
```

Sau đó làm bước 7 và 8 ở trên.

## Sử dụng

- **Trong app.** Bấm nút tròn giữa hai thẻ.
- **Quick Settings.** Sửa danh sách ô và thêm **Ví NFC**. Một chạm là đổi, phụ đề cho biết ví hiện tại.
- **Smart Button.** Trong cài đặt Smart Button của vivo, gán một shortcut của app cho một kiểu bấm. Gợi ý: *Đổi ví NFC* cho một kiểu bấm và *Mở ví đang dùng* cho kiểu khác. Nhấn giữ icon app cũng hiện các shortcut này.

### Từ máy tính

```sh
./switch.sh     # đổi qua lại giữa vivo Wallet và Google Pay qua adb
./shizuku.sh    # bật Shizuku sau khi điện thoại khởi động lại
```

## Sau khi khởi động lại điện thoại

Shizuku tắt khi máy khởi động lại vì nó chạy không cần root. Bật lại: trên điện thoại, bật Wi-Fi và Gỡ lỗi không dây, mở Shizuku và bấm Start (không cần ghép đôi lại). Hoặc chạy `./shizuku.sh` từ máy tính. Ví mặc định, quyền Shizuku, cặp ví, shortcut và ngôn ngữ đều được giữ nguyên.

Khi Shizuku chưa chạy, ô Quick Settings mở màn thanh toán của hệ thống, còn shortcut hiện thông báo nhắc bật Shizuku.

## Giới hạn đã biết

- **vivo Global (Funtouch OS).** App Access Card (`com.vivo.seclient`) được tự nhận diện và thay chỗ vivo Wallet trong danh sách, cũng là ví thứ nhất mặc định. Việc đổi qua lại giữa Access Card và Google Pay đã được thử trên vivo V2413 (Android 16) và hệ thống chấp nhận Access Card làm ví mặc định. Thanh toán ở máy POS chưa được thử, và shortcut *Mở ví đang dùng* cũng chưa được thử trên máy Global.
- **Nút nguồn trên Global.** Bấm nguồn hai lần nhanh (dưới khoảng 300 ms) luôn mở Google Wallet, bất kể ví mặc định là gì. Cử chỉ của Android bỏ qua Access Card vì dịch vụ ví của nó khai báo quyền tuỳ biến thay vì `android.permission.BIND_QUICK_ACCESS_WALLET_SERVICE`, và việc này không thể sửa từ bên ngoài. Bấm đôi chậm hơn (khoảng 400 ms) có thể mở Access Card khi Cài đặt, Power button, "Double-press the Power button" được đặt là "Open Access Card". Hãy dùng shortcut *Mở ví đang dùng* trên Smart Button để có kết quả ổn định.
- **Bấm nút nguồn 2 lần.** OriginOS luôn mở màn quẹt thẻ của vivo Wallet khi bấm nguồn 2 lần, bất kể ví mặc định là gì. Công tắc đó thuộc về app vivo Wallet và không thể ghi từ bên ngoài. Nếu bạn tắt nó trong vivo Wallet thì bấm nguồn 2 lần chỉ khoá máy, vì ROM này không có cử chỉ double-press chuẩn của Android. Hãy dùng shortcut *Mở ví đang dùng* trên Smart Button thay thế.
- **Màn nhắc nhảy app.** vivo có thể hiện một màn nhắc "chuyển sang app khác" trong khoảng 1 giây khi shortcut mở ví.
- **App Google Wallet.** App Google Wallet (`com.google.android.apps.walletnfcrel`) không giữ được role ví. Google Pay được đổi qua Google Play services (`com.google.android.gms`), và app dùng đúng package này.
- **Chưa thử ở máy POS.** Việc đổi ví được kiểm chứng qua holder của role và trạng thái định tuyến NFC. Thanh toán thật ở máy POS chưa được thử.
- **API không công khai.** App gọi `Shizuku.newProcess` bằng reflection vì hàm này thành private từ Shizuku API 13.1. Bản Shizuku sau này có thể buộc chuyển sang UserService.

## Cấu trúc dự án

```text
app/src/main/java/com/hiiamtrong/nfcswitch/
  Wallet.java          logic role holder, liệt kê ví, gọi shell qua Shizuku
  MainActivity.java    màn hình animation, cài đặt cặp ví, ngôn ngữ, shortcut
  SwitchActivity.java  đích không giao diện của các shortcut
  SwitchTile.java      ô Quick Settings
  App.java             ngôn ngữ mặc định ở lần chạy đầu
app/src/main/res/      chuỗi (mặc định English, tiếng Việt ở values-vi), icon, shortcut
switch.sh              đổi ví qua adb
shizuku.sh             bật Shizuku qua adb
```

## Đóng góp

Hoan nghênh issue và pull request. Nếu bạn thử trên dòng vivo hoặc phiên bản OriginOS khác, hãy báo model, phiên bản OS và việc đổi ví cùng định tuyến NFC có đổi đúng không.

## Giấy phép

[MIT](LICENSE)

## Miễn trừ trách nhiệm

Dự án thay đổi mặc định thanh toán của hệ thống bằng đường không chính thức và không liên kết với vivo, Google hay Shizuku. Tự chịu rủi ro khi sử dụng.
