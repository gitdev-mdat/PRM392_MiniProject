# Mini Delivery App
Ứng dụng Android hiển thị bản đồ và chỉ đường giữa hai địa điểm, sử dụng thư viện **osmdroid** và **osmbonuspack**.


## 🧩 Tính năng

- Tìm kiếm địa điểm theo tên (Geocoding)
- Gợi ý địa điểm khi nhập (AutoComplete)
- Hiển thị điểm bắt đầu / điểm đến bằng Marker
- Tính đường đi bằng OSRM (Open Source Routing Machine)
- Tự động zoom bản đồ để hiển thị toàn tuyến đường
- Tăng hiệu suất với cơ chế **cache kết quả tuyến đường**
- Hiển thị tiến trình "Đang tính đường..."

---

## ⚙️ Yêu cầu hệ thống

- Android 6.0 trở lên (API 23+)
- Android Studio (đề xuất từ phiên bản Chipmunk trở lên)
- Kết nối Internet khi tìm kiếm và tính đường đi

---

## 📦 Cài đặt thư viện

### ✅ Cách 1: Thêm bằng Gradle (Khuyến nghị)

Trong `build.gradle (Module)`:

```gradle
implementation 'org.osmdroid:osmdroid-android:6.1.16'
implementation 'org.osmdroid:osmdroid-bonus-pack:6.9.0'

🗺️ Thư viện sử dụng
Thư viện	Mô tả
osmdroid	Hiển thị bản đồ offline từ OpenStreetMap
osmbonuspack	Tính năng nâng cao: Routing, Geocoding, Marker, Polyline,...
OSRM	Máy chủ tính lộ trình mã nguồn mở

📘 Tài liệu hướng dẫn
👉 Xem tài liệu hướng dẫn chi tiết tại đây: https://www.canva.com/design/DAGp7JsbOAs/GTrvrH97kthuEDxc29xqxw/edit?utm_content=DAGp7JsbOAs&utm_campaign=designshare&utm_medium=link2&utm_source=sharebutton

👨‍💻 Tác giả
Dự án được thực hiện bởi Nhóm 1 – Sinh viên môn PRM392

