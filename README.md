# 🍽 Restaurant Management System (Java POS)

Hệ thống Quản lý Nhà hàng chuyên nghiệp trên nền tảng Java Desktop Application (Swing), được tái cấu trúc hoàn chỉnh theo tài liệu kiến trúc kỹ thuật (`design.md`).

---

## 🏗 Kiến trúc & Công nghệ (Tech Stack)

- **Mô hình kiến trúc:** MVC (Model - View - Controller).
- **Design Patterns áp dụng:**
  - **Observer Pattern:** Đồng bộ trạng thái đơn hàng thời gian thực giữa Thu ngân (POS) và Nhà bếp (Kitchen), tự động cập nhật số suất còn lại khi kho thay đổi.
  - **Strategy Pattern:** Tính toán chiết khấu linh hoạt (`NoDiscount`, `VipDiscount` - 10%, `HolidayDiscount` - 5%).
  - **Factory Pattern:** Khởi tạo phương thức thanh toán (`PaymentFactory` -> `CashPayment`, `TransferPayment`).
  - **Singleton Pattern:** Quản lý Connection Pool (`DatabaseConnection` với HikariCP).
  - **Pessimistic Locking:** Khóa bàn (`SELECT ... WITH (UPDLOCK)`) và nguyên liệu khi thao tác đơn hàng tránh xung đột dữ liệu.
- **Giao diện người dùng:** Java Swing + giao diện hiện đại **FlatLaf** (FlatDarkLaf).
- **Cơ sở dữ liệu:** Microsoft SQL Server (kết nối qua `mssql-jdbc` + HikariCP).
- **Bảo mật:** Mã hóa mật khẩu người dùng với **BCrypt** (`org.mindrot:jbcrypt`).
- **Thư viện tiện ích:**
  - **Log4j 2:** Ghi log hệ thống vào console và thư mục `logs/restaurant-pos.log`.
  - **JFreeChart:** Vẽ biểu đồ doanh thu trực quan 7 ngày gần nhất.
  - **Apache PDFBox:** Tự động tạo và xuất hóa đơn thanh toán PDF.

---

## 📂 Cấu trúc mã nguồn

```
src/main/java/com/pos/
├── Main.java                         # Điểm khởi chạy ứng dụng (Entry point)
├── model/                            # Thực thể POJO & Enums
│   ├── User.java, Role.java
│   ├── Category.java, CategoryType.java
│   ├── MenuItem.java
│   ├── DiningTable.java, TableStatus.java
│   ├── Order.java, OrderStatus.java, DiscountType.java, PaymentMethodType.java
│   ├── OrderDetail.java, ItemStatus.java
│   ├── Ingredient.java, RecipeLine.java
│   ├── StockMovement.java, MovementType.java
│   └── OrderEvent.java
├── dao/                              # Data Access Object (JDBC PreparedStatement)
│   ├── UserDAO.java, CategoryDAO.java, MenuDAO.java, TableDAO.java
│   ├── OrderDAO.java, IngredientDAO.java, RecipeDAO.java, StockMovementDAO.java
├── controller/                       # Nghiệp vụ & Design Patterns
│   ├── AuthController.java           # Đăng nhập & hash BCrypt
│   ├── TableController.java          # Mở bàn, chuyển bàn, gộp bàn (Khóa bi quan)
│   ├── OrderController.java          # Gọi món, tính tiền, hủy đơn, checkout
│   ├── KitchenController.java        # Bếp nhận món, nấu, nấu xong, từ chối
│   ├── StockController.java          # Trừ kho, hoàn kho, nhập xuất, tính số suất còn
│   ├── ReportController.java         # Biểu đồ doanh thu & báo cáo hóa đơn
│   ├── DiscountStrategy.java, VipDiscount.java, HolidayDiscount.java, NoDiscount.java
│   ├── PaymentMethod.java, CashPayment.java, TransferPayment.java, PaymentFactory.java
│   └── OrderObserver.java, StockObserver.java
├── view/                             # Giao diện Swing (FlatLaf)
│   ├── LoginForm.java                # Màn hình đăng nhập
│   ├── MainDashboard.java            # Cửa sổ làm việc chính (Phân quyền theo Role)
│   ├── POSPanel.java                 # Màn hình Thu ngân (Sơ đồ bàn, menu, hóa đơn)
│   ├── KitchenPanel.java             # Màn hình Nhà bếp (Điều phối nấu nướng)
│   ├── InventoryPanel.java           # Quản lý kho nguyên liệu & định lượng món
│   └── ReportPanel.java              # Thống kê doanh thu & xuất lại hóa đơn
└── utils/                            # Tiện ích dùng chung
    ├── DatabaseConnection.java       # HikariCP Pool
    ├── CurrencyFormat.java           # Định dạng tiền tệ VNĐ
    ├── DateUtils.java                # Định dạng ngày giờ chuẩn Việt Nam
    └── PDFExporter.java              # Xuất file hóa đơn PDF
```

---

## 🗄 Hướng dẫn thiết lập Cơ sở dữ liệu (SQL Server)

Hệ thống hỗ trợ 2 cách cài đặt CSDL cực kỳ tiện lợi trong thư mục `database/`:
- **Cách 1 (Khuyên dùng - 100% tương thích mọi phiên bản SQL Server):** Chạy tệp **`database/setup_db.bat`** (hoặc mở file [database/setup_database.sql](file:///c:/Users/Thanhpham/Documents/GitHub/Restaurant-ManagementSys/database/setup_database.sql) trong SSMS và ấn F5).
- **Cách 2 (Phục hồi từ file backup):** Chạy tệp **`database/restore_db.bat`** để tự động restore từ tệp [RestaurantManagementSys.bak](file:///c:/Users/Thanhpham/Documents/GitHub/Restaurant-ManagementSys/database/RestaurantManagementSys.bak).

Cả hai script đều tự động dò tìm máy chủ SQL Server (`localhost` hoặc `SQLEXPRESS`) và tự động đồng bộ cấu hình vào `db.properties`.

Chi tiết xem tại tài liệu hướng dẫn: [database/HUONG_DAN_CAI_DAT_DATABASE.md](file:///c:/Users/Thanhpham/Documents/GitHub/Restaurant-ManagementSys/database/HUONG_DAN_CAI_DAT_DATABASE.md).

### 🔑 Tài khoản đăng nhập mẫu

| Tên đăng nhập | Mật khẩu | Quyền hạn (Role) | Chức năng truy cập |
|---------------|----------|-------------------|---------------------|
| `admin` | `admin123` | **MANAGER** (Quản lý) | Toàn quyền: POS, Bếp, Sơ đồ bàn, Thực đơn, Khách hàng, Voucher, Kho hàng, Sổ quỹ, Ca làm việc, Báo cáo & Tài khoản |
| `cashier1` | `cashier123` | **CASHIER** (Thu ngân) | Điểm bán hàng (POS), Sơ đồ bàn & Đặt bàn, Quản lý khách hàng, Ca làm việc & Két tiền |
| `kitchen1` | `kitchen123` | **KITCHEN** (Nhà bếp) | Màn hình KDS Bếp & Bar: Điều phối nấu món theo thời gian thực |

---

## ⚙ Cấu hình kết nối CSDL

File cấu hình kết nối: `db.properties` (hoặc `src/main/resources/db.properties`)

```properties
db.host=localhost
db.port=1433
db.name=RestaurantManagementSys
db.user=sa
db.password=12345
```
*(Nếu máy dùng SQL Server Express hoặc cổng khác, bạn chỉ cần sửa `db.host=localhost\\SQLEXPRESS` hoặc chạy file `setup_db.bat`/`restore_db.bat` để được cấu hình tự động).*

---

## 🚀 Cách biên dịch & chạy ứng dụng

### Cách 1: Chạy file JAR độc lập (Standalone Fat JAR)
```powershell
java -jar target/restaurant-pos-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Cách 2: Chạy trực tiếp từ mã nguồn qua Maven
```powershell
mvn clean compile exec:java
```
Hoặc mở thư mục dự án trực tiếp bằng **Apache NetBeans**, **IntelliJ IDEA** hoặc **Eclipse** và nhấn **Run Project** (Main class: `com.pos.Main`)
