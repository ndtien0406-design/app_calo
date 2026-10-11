# CaloTrack — App theo dõi calo hàng ngày

Ứng dụng Android ghi nhận lượng calo nạp vào mỗi ngày, được viết bằng **Kotlin** với kiến trúc Single Activity + 4 Fragment + BottomNavigationView.

## Tính năng

- **Trang chủ**: tổng quan calo hôm nay (đã nạp / mục tiêu / còn lại hoặc vượt), thanh tiến trình đổi màu khi vượt mục tiêu, danh sách bữa ăn trong ngày, nút thêm bữa ăn nhanh.
- **Thực phẩm**: thư viện thực phẩm với tìm kiếm, thêm / sửa / xóa món.
- **Lịch sử**: biểu đồ cột calo 7 ngày gần nhất (MPAndroidChart) và danh sách tổng calo từng ngày.
- **Cài đặt**: đặt mục tiêu calo mỗi ngày (500–10000 kcal), bật/tắt giao diện tối, xem đơn vị năng lượng và phiên bản app.
- Cơ sở dữ liệu **Room** lưu món ăn và bữa ăn; **seed sẵn 89 món ăn Việt Nam (chèn đồng bộ khi DB được tạo lần đầu)** phổ biến ngay lần đầu mở app.

## Yêu cầu

- Android Studio **Hedgehog (2023.1.1)** trở lên
- **JDK 17** (Android Studio Hedgehog đã đi kèm)
- Thiết bị / máy ảo chạy **Android 7.0 (API 24)** trở lên

## Các bước build & chạy

1. Mở **Android Studio** → chọn **Open** → trỏ tới thư mục `calorie-tracker-android`.
2. Đợi **Sync Gradle** hoàn tất (lần đầu cần mạng để tải dependencies, trong đó có MPAndroidChart qua JitPack).
3. Kết nối thiết bị Android (bật USB debugging) hoặc khởi động Emulator API 24+.
4. Nhấn **Run ▶** (Shift+F10).

## Cấu trúc thư mục

```
calorie-tracker-android/
├── settings.gradle / build.gradle / gradle.properties
├── README.md
└── app/
    ├── build.gradle                 # namespace com.ndtien.calotrack, compileSdk/targetSdk 34
    └── src/main/
        ├── AndroidManifest.xml      # MainActivity là launcher, theme Theme.CaloTrack
        ├── java/com/ndtien/calotrack/
        │   ├── MainActivity.kt      # BottomNavigationView, giữ 4 instance Fragment, dark mode
        │   ├── data/                # Food, MealEntry, FoodDao, MealDao, AppDatabase, SeedData
        │   ├── ui/home/             # HomeFragment, HomeViewModel, MealAdapter
        │   ├── ui/meal/             # AddEditMealDialog
        │   ├── ui/food/             # FoodListFragment, FoodAdapter, AddEditFoodDialog
        │   ├── ui/history/          # HistoryFragment, HistoryViewModel, HistoryAdapter
        │   ├── ui/settings/         # SettingsFragment
        │   └── util/                # DateUtils, PrefsManager
        └── res/
            ├── layout/              # activity_main, fragment_*, item_*, dialog_*
            ├── menu/bottom_nav_menu.xml
            ├── drawable/            # icon vector 24dp + bg_badge
            ├── values/              # strings, colors, themes (tiếng Việt)
            └── values-night/themes.xml  # theme tối
```

## Lưu ý

- **Lần đầu mở app**: Room tự động seed cơ sở dữ liệu với danh sách món ăn Việt Nam (chỉ seed khi DB mới tạo và bảng `foods` còn trống).
- **Chế độ tối**: được áp dụng trong `MainActivity.onCreate()` **trước** `setContentView()` theo lựa chọn trong Cài đặt (`PrefsManager.KEY_DARK_MODE`); tắt thì theo hệ thống (`MODE_NIGHT_FOLLOW_SYSTEM`).
- Định dạng ngày lưu trong DB: `yyyy-MM-dd`; giờ: `HH:mm`.
- Số calo hiển thị không thập phân nếu là số nguyên.

## Đẩy lên GitHub

Repo đã tạo sẵn: **`ndtien0406-design/app_calo`**. Để đẩy code lên:

```bash
cd calorie-tracker-android
git init
git add .
git commit -m "Khởi tạo project CaloTrack"
git branch -M main
git remote add origin https://github.com/ndtien0406-design/app_calo.git
git push -u origin main
```

(Nếu dùng SSH: `git@github.com:ndtien0406-design/app_calo.git`)
