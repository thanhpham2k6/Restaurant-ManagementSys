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

1. Mở **SQL Server Management Studio (SSMS)**.
2. Mở file [database/schema.sql](file:///c:/Users/Thanhpham/Documents/GitHub/Restaurant-ManagementSys/database/schema.sql) và thực thi (Execute / F5).
3. Script sẽ tự động:
   - Tạo database `RestaurantManagementSys`.
   - Tạo đầy đủ 9 bảng quan hệ: `users`, `categories`, `menu_items`, `dining_tables`, `orders`, `order_details`, `ingredients`, `recipes`, `stock_movements`.
   - Thêm 3 tài khoản mặc định (Manager, Cashier, Kitchen).
   - Thêm 10 bàn ăn mẫu.
   - Thêm 50 món ăn Việt Nam phong phú và các danh mục.
   - Thêm nguyên liệu kho ban đầu và công thức định lượng (Recipes) mẫu.

### 🔑 Tài khoản đăng nhập mẫu

| Tên đăng nhập | Mật khẩu | Quyền hạn (Role) | Chức năng truy cập |
|---------------|----------|-------------------|---------------------|
| `manager` | `password123` | **MANAGER** (Quản lý) | Toàn quyền: POS, Bếp, Kho, Định lượng, Báo cáo |
| `cashier1` | `password123` | **CASHIER** (Thu ngân) | Bán hàng (POS), Sơ đồ bàn, Theo dõi trạng thái bếp |
| `kitchen1` | `password123` | **KITCHEN** (Nhà bếp) | Nhận món, Báo nấu, Kho nguyên vật liệu |

---

## ⚙ Cấu hình kết nối CSDL

File cấu hình: [DatabaseConnection.java](file:///c:/Users/Thanhpham/Documents/GitHub/Restaurant-ManagementSys/src/main/java/com/pos/utils/DatabaseConnection.java)

```java
private static final String JDBC_URL =
    "jdbc:sqlserver://26.97.143.116;databaseName=RestaurantManagementSys;encrypt=true;trustServerCertificate=true;";
private static final String DB_USER = "sa";
private static final String DB_PASS = "12345";
```
*(Bạn có thể đổi IP máy chủ `26.97.143.116` sang `localhost` hoặc IP SQL Server của bạn nếu cần)*.

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
Hoặc mở thư mục dự án trực tiếp bằng **Apache NetBeans**, **IntelliJ IDEA** hoặc **Eclipse** và nhấn **Run Project** (Main class: `com.pos.Main`).
