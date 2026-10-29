# HƯỚNG DẪN CÀI ĐẶT & TEST CƠ SỞ DỮ LIỆU TRÊN MÁY KHÁC
**Dự án:** Hệ Thống Quản Lý Nhà Hàng POS (Restaurant Management System v3.0)  
**Hệ quản trị CSDL:** Microsoft SQL Server (2016 / 2017 / 2019 / 2022 / Express / Developer / Docker)  
**Tên Database:** `RestaurantManagementSys`

---

## 1. THÔNG TIN CẤU HÌNH MẶC ĐỊNH

Tệp cấu hình kết nối của phần mềm nằm tại: `src/main/resources/database.properties`
```properties
db.url=jdbc:sqlserver://localhost:1433;databaseName=RestaurantManagementSys;encrypt=false;trustServerCertificate=true
db.user=sa
db.password=12345
```
*(Nếu máy mới dùng cổng hoặc tài khoản khác, bạn chỉ cần sửa trong tệp trên).*

---

## 2. CÁCH 1: KHỞI TẠO BẰNG SCRIPT SQL (`setup_database.sql`)
> **Ưu điểm vượt trội:** 100% tương thích với mọi phiên bản SQL Server (không bao giờ bị lỗi *"The database was backed up on a server running version X and cannot be restored on this server"*).

### Lựa chọn 1A: Dùng tệp chạy tự động (.bat)
1. Vào thư mục `database/`
2. Nhấp đúp chuột vào tệp: **`setup_db.bat`**
3. Nhập mật khẩu SQL Server (mặc định là `12345` hoặc bấm Enter nếu dùng mặc định).

### Lựa chọn 1B: Chạy bằng SQL Server Management Studio (SSMS)
1. Mở phần mềm **SSMS**, kết nối tới máy chủ SQL Server của bạn.
2. Kéo thả hoặc mở tệp **`database/setup_database.sql`** vào SSMS.
3. Bấm nút **Execute (F5)** để thực thi toàn bộ script.

### Lựa chọn 1C: Chạy bằng dòng lệnh (CMD / PowerShell / Terminal)
```bash
sqlcmd -S localhost -U sa -P 12345 -C -f 65001 -i "database/setup_database.sql"
```

---

## 3. CÁCH 2: PHỤC HỒI TỪ TỆP SAO LƯU (.BAK)
> Tệp sao lưu mới nhất: **`database/RestaurantManagementSys.bak`**

### Lựa chọn 2A: Dùng tệp chạy tự động (.bat)
1. Vào thư mục `database/`
2. Nhấp đúp chuột vào tệp: **`restore_db.bat`**

### Lựa chọn 2B: Lệnh T-SQL trực tiếp
Mở New Query trong SSMS và chạy:
```sql
USE master;
GO

-- Đóng các kết nối đang mở
IF EXISTS (SELECT name FROM sys.databases WHERE name = N'RestaurantManagementSys')
BEGIN
    ALTER DATABASE RestaurantManagementSys SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
END
GO

-- Restore đè lên database
RESTORE DATABASE RestaurantManagementSys
FROM DISK = N'C:\duong_dan_chinh_xac_toi_du_an\database\RestaurantManagementSys.bak'
WITH REPLACE;
GO

ALTER DATABASE RestaurantManagementSys SET MULTI_USER;
GO
```

---

## 4. TÀI KHOẢN ĐĂNG NHẬP MẪU SAU KHI CÀI ĐẶT

Sau khi cài đặt xong, bạn có thể khởi chạy ứng dụng và đăng nhập bằng 3 tài khoản mẫu có quyền hạn phân cấp rõ ràng:

| Vai trò (Role) | Tên đăng nhập (Username) | Mật khẩu (Password) | Chức năng chính |
| :--- | :--- | :--- | :--- |
| **Quản lý (Manager)** | `admin` | `admin123` | Toàn quyền: Bảng điều khiển, Báo cáo doanh thu, Quản lý kho, Quản lý nhân viên, Voucher, Ca làm việc |
| **Thu ngân (Cashier)** | `cashier1` | `cashier123` | Bán hàng POS, Sơ đồ bàn & Đặt bàn, Quản lý khách hàng CRM, Mở/Đóng ca làm việc & In Z-Report |
| **Nhà bếp (Kitchen)** | `kitchen1` | `kitchen123` | Màn hình nhà bếp (KDS), Cập nhật chế biến món, Trả món đã nấu xong |

---

## 5. DỮ LIỆU ĐÃ ĐƯỢC TÍCH HỢP SẴN
- **13 Danh mục** món ăn & đồ uống.
- **53 Món ăn** chuẩn ẩm thực Việt Nam (khớp 1:1 với bộ ảnh trong `resources/images/dishes/`).
- **15 Bàn ăn** với đầy đủ QR Code Token và Version quản trị đồng thời.
- **5 Khách hàng thân thiết** đầy đủ các hạng Standard, Silver, Gold, Diamond.
- **4 Mã giảm giá (Vouchers)**: `WELCOME10`, `SAVE50K`, `VIP20`, `FREESHIP`.
- **4 Nhà cung cấp & 20 mặt hàng kho** theo dõi số lượng tồn.
- **Lịch đặt bàn, ca làm việc và phiếu kiểm kho mẫu** để kiểm thử ngay lập tức.
