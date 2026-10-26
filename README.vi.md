# NFC Wallet Switch

[English](README.md) | [Tiếng Việt](README.vi.md)

Đổi ví NFC thanh toán mặc định trên điện thoại vivo chỉ bằng một chạm. Chọn hai ví bất kỳ (ví dụ vivo Wallet và Google Pay) rồi đổi qua lại từ app, ô Quick Settings hoặc Smart Button.

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

- Điện thoại vivo chạy OriginOS trên Android 14 trở lên (minSdk 34). Mới chỉ kiểm tra trên X300 Pro.
- [Shizuku](https://github.com/RikkaApps/Shizuku/releases) đã cài và đang chạy.
- Để build: JDK 17 và Android SDK có platform 35. Gradle wrapper tự tải Gradle.
- Để bật Shizuku từ máy tính: `adb` với Wireless debugging đã bật.

## Build và cài đặt

```sh
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Mỗi lần push và pull request đều được GitHub Actions build (`.github/workflows/build.yml`), APK debug được đính kèm vào lần chạy dưới dạng artifact `nfcswitch-debug-apk`.

## Thiết lập

1. Cài Shizuku và khởi động nó. Mở app Shizuku rồi chọn "Start via Wireless debugging", hoặc kết nối adb với điện thoại và chạy:
   ```sh
   ./shizuku.sh
   ```
2. Mở **NFC Wallet Switch**. Khi app hỏi, bấm **Cấp quyền Shizuku** và chọn cho phép trong hộp thoại của Shizuku.
3. Trong phần **Cài đặt** của app, chọn ví thứ nhất và ví thứ hai.

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

Shizuku tắt khi máy khởi động lại vì nó chạy không cần root. Bật lại bằng app Shizuku hoặc `./shizuku.sh`. Ví mặc định, quyền Shizuku, cặp ví, shortcut và ngôn ngữ đều được giữ nguyên.

Khi Shizuku chưa chạy, ô Quick Settings mở màn thanh toán của hệ thống, còn shortcut hiện thông báo nhắc bật Shizuku.

## Giới hạn đã biết

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

## Miễn trừ trách nhiệm

Dự án thay đổi mặc định thanh toán của hệ thống bằng đường không chính thức và không liên kết với vivo, Google hay Shizuku. Tự chịu rủi ro khi sử dụng.
