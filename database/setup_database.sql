-- =====================================================================
-- HỆ THỐNG QUẢN LÝ NHÀ HÀNG POS (RESTAURANT MANAGEMENT SYSTEM v3.0)
-- TẬP LỆNH KHỞI TẠO TOÀN DIỆN DATABASE & DỮ LIỆU MẪU ĐẦY ĐỦ
-- Hệ quản trị CSDL: Microsoft SQL Server (2016, 2017, 2019, 2022, Express/Developer)
-- Tương thích: Windows, Linux / Docker SQL Server
-- =====================================================================

-- 1. TẠO CƠ SỞ DỮ LIỆU NẾU CHƯA CÓ
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'RestaurantManagementSys')
BEGIN
    CREATE DATABASE RestaurantManagementSys;
    PRINT N'>>> Đã tạo mới cơ sở dữ liệu: RestaurantManagementSys';
END
GO

USE RestaurantManagementSys;
GO

-- 2. DỌN DẸP BẢNG CŨ (THEO THỨ TỰ RÀNG BUỘC KHÓA NGOẠI)
-- Dọn dẹp bảng cũ của v2 nếu có
IF OBJECT_ID('dbo.recipes','U') IS NOT NULL DROP TABLE dbo.recipes;
IF OBJECT_ID('dbo.ingredients','U') IS NOT NULL DROP TABLE dbo.ingredients;

-- Dọn dẹp 17 bảng của v3
IF OBJECT_ID('dbo.employee_attendances','U') IS NOT NULL DROP TABLE dbo.employee_attendances;
IF OBJECT_ID('dbo.cash_transactions','U') IS NOT NULL DROP TABLE dbo.cash_transactions;
IF OBJECT_ID('dbo.inventory_audit_details','U') IS NOT NULL DROP TABLE dbo.inventory_audit_details;
IF OBJECT_ID('dbo.inventory_audits','U') IS NOT NULL DROP TABLE dbo.inventory_audits;
IF OBJECT_ID('dbo.stock_movements','U') IS NOT NULL DROP TABLE dbo.stock_movements;
IF OBJECT_ID('dbo.inventory_items','U') IS NOT NULL DROP TABLE dbo.inventory_items;
IF OBJECT_ID('dbo.suppliers','U') IS NOT NULL DROP TABLE dbo.suppliers;
IF OBJECT_ID('dbo.order_details','U') IS NOT NULL DROP TABLE dbo.order_details;
IF OBJECT_ID('dbo.orders','U') IS NOT NULL DROP TABLE dbo.orders;
IF OBJECT_ID('dbo.reservations','U') IS NOT NULL DROP TABLE dbo.reservations;
IF OBJECT_ID('dbo.dining_tables','U') IS NOT NULL DROP TABLE dbo.dining_tables;
IF OBJECT_ID('dbo.menu_items','U') IS NOT NULL DROP TABLE dbo.menu_items;
IF OBJECT_ID('dbo.categories','U') IS NOT NULL DROP TABLE dbo.categories;
IF OBJECT_ID('dbo.vouchers','U') IS NOT NULL DROP TABLE dbo.vouchers;
IF OBJECT_ID('dbo.customers','U') IS NOT NULL DROP TABLE dbo.customers;
IF OBJECT_ID('dbo.shifts','U') IS NOT NULL DROP TABLE dbo.shifts;
IF OBJECT_ID('dbo.users','U') IS NOT NULL DROP TABLE dbo.users;
GO

PRINT N'>>> Bắt đầu tạo 15 bảng chuẩn Enterprise v3.0...';
GO

-- ==================== 1. USERS ====================
CREATE TABLE users (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    username      NVARCHAR(50)  NOT NULL UNIQUE,
    password_hash NVARCHAR(255) NOT NULL,
    full_name     NVARCHAR(100) NOT NULL,
    role          NVARCHAR(20)  NOT NULL CHECK (role IN ('MANAGER','CASHIER','KITCHEN')),
    is_active     BIT           NOT NULL DEFAULT 1
);
GO

-- ==================== 2. SHIFTS ====================
CREATE TABLE shifts (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    user_id         INT           NOT NULL REFERENCES users(id),
    start_time      DATETIME2     NOT NULL DEFAULT GETDATE(),
    end_time        DATETIME2     NULL,
    initial_cash    DECIMAL(15,2) NOT NULL DEFAULT 0,
    actual_cash     DECIMAL(15,2) NULL,
    cash_sales      DECIMAL(15,2) NOT NULL DEFAULT 0,
    transfer_sales  DECIMAL(15,2) NOT NULL DEFAULT 0,
    difference_cash DECIMAL(15,2) NULL,
    status          NVARCHAR(10)  NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','CLOSED')),
    note            NVARCHAR(500) NULL
);
GO

-- ==================== 3. CUSTOMERS ====================
CREATE TABLE customers (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    name            NVARCHAR(100) NOT NULL,
    phone           NVARCHAR(20)  NOT NULL UNIQUE,
    email           NVARCHAR(150) NULL,
    address         NVARCHAR(255) NULL,
    loyalty_points  INT           NOT NULL DEFAULT 0,
    total_spent     DECIMAL(15,2) NOT NULL DEFAULT 0,
    membership_tier NVARCHAR(10)  NOT NULL DEFAULT 'STANDARD' CHECK (membership_tier IN ('STANDARD','SILVER','GOLD','DIAMOND')),
    created_at      DATETIME2     NOT NULL DEFAULT GETDATE()
);
GO

-- ==================== 4. VOUCHERS ====================
CREATE TABLE vouchers (
    id                 INT IDENTITY(1,1) PRIMARY KEY,
    code               NVARCHAR(50)  NOT NULL UNIQUE,
    discount_type      NVARCHAR(10)  NOT NULL CHECK (discount_type IN ('PERCENT','FIXED')),
    discount_val       DECIMAL(15,2) NOT NULL,
    min_order_amount   DECIMAL(15,2) NOT NULL DEFAULT 0,
    max_discount_amount DECIMAL(15,2) NULL,
    start_date         DATE          NOT NULL,
    end_date           DATE          NOT NULL,
    usage_limit        INT           NOT NULL DEFAULT 1,
    used_count         INT           NOT NULL DEFAULT 0,
    is_active          BIT           NOT NULL DEFAULT 1
);
GO

-- ==================== 5. CATEGORIES ====================
CREATE TABLE categories (
    id   INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL UNIQUE,
    type NVARCHAR(10)  NOT NULL CHECK (type IN ('FOOD','DRINK'))
);
GO

-- ==================== 6. MENU_ITEMS ====================
CREATE TABLE menu_items (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    category_id  INT           NOT NULL REFERENCES categories(id),
    name         NVARCHAR(150) NOT NULL,
    price        DECIMAL(12,2) NOT NULL,
    image_path   NVARCHAR(255) NULL,
    is_available BIT           NOT NULL DEFAULT 1,
    version      INT           NOT NULL DEFAULT 0
);
GO

-- ==================== 7. DINING_TABLES ====================
CREATE TABLE dining_tables (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    name          NVARCHAR(50)  NOT NULL UNIQUE,
    status        NVARCHAR(15)  NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE','OCCUPIED','RESERVED','CLEANING')),
    capacity      INT           NOT NULL DEFAULT 4,
    qr_code_token NVARCHAR(100) NULL,
    version       INT           NOT NULL DEFAULT 0
);
GO

-- ==================== 8. RESERVATIONS ====================
CREATE TABLE reservations (
    id               INT IDENTITY(1,1) PRIMARY KEY,
    table_id         INT           NOT NULL REFERENCES dining_tables(id),
    customer_id      INT           NULL REFERENCES customers(id),
    customer_name    NVARCHAR(100) NOT NULL,
    customer_phone   NVARCHAR(20)  NOT NULL,
    party_size       INT           NOT NULL DEFAULT 2,
    reservation_time DATETIME2     NOT NULL,
    deposit_amount   DECIMAL(15,2) NOT NULL DEFAULT 0,
    status           NVARCHAR(15)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','CONFIRMED','SEATED','CANCELLED')),
    channel          NVARCHAR(30)  NOT NULL DEFAULT 'DIRECT',
    note             NVARCHAR(500) NULL,
    created_at       DATETIME2     NOT NULL DEFAULT GETDATE()
);
GO

-- ==================== 9. ORDERS ====================
CREATE TABLE orders (
    id               INT IDENTITY(1,1) PRIMARY KEY,
    shift_id         INT           NULL REFERENCES shifts(id),
    user_id          INT           NULL REFERENCES users(id),
    table_id         INT           NULL REFERENCES dining_tables(id),
    customer_id      INT           NULL REFERENCES customers(id),
    voucher_id       INT           NULL REFERENCES vouchers(id),
    order_type       NVARCHAR(15)  NOT NULL DEFAULT 'DINE_IN' CHECK (order_type IN ('DINE_IN','TAKEAWAY','DELIVERY')),
    status           NVARCHAR(20)  NOT NULL DEFAULT 'OPEN' CHECK (status IN ('PENDING','IN_PROGRESS','READY','COMPLETED','PAID','OPEN','CANCELLED')),
    payment_method   NVARCHAR(20)  NULL CHECK (payment_method IN ('CASH','TRANSFER','MIXED')),
    subtotal         DECIMAL(15,2) NOT NULL DEFAULT 0,
    discount_amount  DECIMAL(15,2) NOT NULL DEFAULT 0,
    delivery_fee     DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_amount     DECIMAL(15,2) NOT NULL DEFAULT 0,
    points_earned    INT           NOT NULL DEFAULT 0,
    points_used      INT           NOT NULL DEFAULT 0,
    delivery_address NVARCHAR(500) NULL,
    delivery_phone   NVARCHAR(20)  NULL,
    shipper_name     NVARCHAR(100) NULL,
    note             NVARCHAR(500) NULL,
    created_at       DATETIME2     NOT NULL DEFAULT GETDATE(),
    completed_at     DATETIME2     NULL
);
GO

-- ==================== 10. ORDER_DETAILS ====================
CREATE TABLE order_details (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    order_id        INT           NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id    INT           NOT NULL REFERENCES menu_items(id),
    quantity        INT           NOT NULL CHECK (quantity > 0),
    unit_price      DECIMAL(12,2) NOT NULL,
    status          NVARCHAR(20)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','ACCEPTED','COOKING','DONE','SERVED','REJECTED','CANCELLED','PREPARING')),
    note            NVARCHAR(255) NULL,
    sent_to_kitchen BIT           NOT NULL DEFAULT 0
);
GO

-- ==================== 11. SUPPLIERS ====================
CREATE TABLE suppliers (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    name           NVARCHAR(150) NOT NULL UNIQUE,
    contact_person NVARCHAR(100) NULL,
    phone          NVARCHAR(20)  NULL,
    email          NVARCHAR(150) NULL,
    address        NVARCHAR(255) NULL,
    is_active      BIT           NOT NULL DEFAULT 1
);
GO

-- ==================== 12. INVENTORY_ITEMS ====================
CREATE TABLE inventory_items (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    supplier_id INT           NULL REFERENCES suppliers(id),
    name        NVARCHAR(150) NOT NULL UNIQUE,
    category    NVARCHAR(50)  NOT NULL DEFAULT N'Hàng hóa',
    unit        NVARCHAR(20)  NOT NULL DEFAULT N'cái',
    cost_price  DECIMAL(12,2) NOT NULL DEFAULT 0,
    stock_qty   DECIMAL(12,2) NOT NULL DEFAULT 0,
    min_qty     DECIMAL(12,2) NOT NULL DEFAULT 5,
    is_active   BIT           NOT NULL DEFAULT 1
);
GO

-- ==================== 13. STOCK_MOVEMENTS ====================
CREATE TABLE stock_movements (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    item_id     INT           NOT NULL REFERENCES inventory_items(id),
    type        NVARCHAR(20)  NOT NULL CHECK (type IN ('IMPORT','EXPORT','ADJUSTMENT','ADJUST','WASTE','RETURN','ADJUST_AUDIT')),
    quantity    DECIMAL(12,2) NOT NULL,
    unit_price  DECIMAL(12,2) NOT NULL DEFAULT 0,
    supplier_id INT           NULL REFERENCES suppliers(id),
    user_id     INT           NOT NULL REFERENCES users(id),
    note        NVARCHAR(500) NULL,
    created_at  DATETIME2     NOT NULL DEFAULT GETDATE()
);
GO

-- ==================== 14. INVENTORY_AUDITS ====================
CREATE TABLE inventory_audits (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    audit_code NVARCHAR(50)  NOT NULL UNIQUE,
    user_id    INT           NOT NULL REFERENCES users(id),
    audit_date DATETIME2     NOT NULL DEFAULT GETDATE(),
    status     NVARCHAR(15)  NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','COMPLETED','CANCELLED')),
    note       NVARCHAR(500) NULL
);
GO

-- ==================== 15. INVENTORY_AUDIT_DETAILS ====================
CREATE TABLE inventory_audit_details (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    audit_id      INT           NOT NULL REFERENCES inventory_audits(id) ON DELETE CASCADE,
    item_id       INT           NOT NULL REFERENCES inventory_items(id),
    system_qty    DECIMAL(12,2) NOT NULL,
    actual_qty    DECIMAL(12,2) NOT NULL DEFAULT 0,
    difference_qty DECIMAL(12,2) NOT NULL DEFAULT 0,
    reason        NVARCHAR(500) NULL
);
GO

-- ==================== 16. CASH_TRANSACTIONS (SỔ QUỸ THU CHI) ====================
CREATE TABLE cash_transactions (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    code           NVARCHAR(50)  NOT NULL UNIQUE,
    type           NVARCHAR(20)  NOT NULL, -- 'RECEIPT' (Thu) or 'PAYMENT' (Chi)
    category       NVARCHAR(100) NOT NULL,
    amount         DECIMAL(18,2) NOT NULL,
    payment_method NVARCHAR(50)  DEFAULT N'Tiền mặt',
    person_name    NVARCHAR(150),
    note           NVARCHAR(500),
    user_id        INT           FOREIGN KEY REFERENCES users(id),
    created_at     DATETIME2     DEFAULT GETDATE()
);
GO

-- ==================== 17. EMPLOYEE_ATTENDANCES (CHẤM CÔNG NHÂN VIÊN) ====================
CREATE TABLE employee_attendances (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    user_id        INT           NOT NULL FOREIGN KEY REFERENCES users(id),
    work_date      DATE          NOT NULL,
    shift_name     NVARCHAR(50)  NOT NULL,
    check_in_time  DATETIME2,
    check_out_time DATETIME2,
    hours_worked   DECIMAL(5,2)  DEFAULT 0,
    status         NVARCHAR(50)  DEFAULT N'Đúng giờ',
    note           NVARCHAR(255),
    created_at     DATETIME2     DEFAULT GETDATE()
);
GO

-- ==================== TẠO CÁC CHỈ MỤC (INDEXES) ====================
CREATE INDEX IX_orders_shift ON orders(shift_id);
CREATE INDEX IX_orders_table ON orders(table_id);
CREATE INDEX IX_orders_customer ON orders(customer_id);
CREATE INDEX IX_orders_status ON orders(status);
CREATE INDEX IX_orders_created ON orders(created_at);
CREATE INDEX IX_order_details_order ON order_details(order_id);
CREATE INDEX IX_stock_movements_item ON stock_movements(item_id);
CREATE INDEX IX_reservations_time ON reservations(reservation_time);
CREATE INDEX IX_reservations_table ON reservations(table_id);
GO

-- =====================================================================
-- NẠP DỮ LIỆU MẪU ĐẦY ĐỦ (SEED DATA)
-- =====================================================================
PRINT N'>>> Bắt đầu nạp dữ liệu mẫu (Seed Data)...';
GO

-- 1. USERS (Mật khẩu: admin123 | cashier123 | kitchen123)
INSERT INTO users (username, password_hash, full_name, role) VALUES
('admin',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', N'Nguyễn Văn Quản Lý', 'MANAGER'),
('cashier1','$2a$10$8K1p/a0dR1lVFKq6xcJSLOUuVcVfEELb7D4eoKjVYpAi3bJQIvWri', N'Trần Thị Thu Ngân', 'CASHIER'),
('kitchen1','$2a$10$8K1p/a0dR1lVFKq6xcJSLOUuVcVfEELb7D4eoKjVYpAi3bJQIvWri', N'Lê Văn Đầu Bếp', 'KITCHEN');
GO

-- 2. CATEGORIES (13 Danh mục thực đơn)
INSERT INTO categories (name, type) VALUES
(N'Món nước',    'FOOD'),
(N'Món bún',     'FOOD'),
(N'Món mì',      'FOOD'),
(N'Cơm',         'FOOD'),
(N'Bánh mì',     'FOOD'),
(N'Món bánh',    'FOOD'),
(N'Món ăn vặt',  'FOOD'),
(N'Món gỏi',     'FOOD'),
(N'Món mặn',     'FOOD'),
(N'Hải sản',     'FOOD'),
(N'Lẩu',         'FOOD'),
(N'Tráng miệng', 'FOOD'),
(N'Đồ uống',     'DRINK');
GO

-- 3. MENU ITEMS (53 Món ăn Việt Nam chuẩn - Khớp chính xác 1:1 với ảnh 1.jpg -> 53.jpg)
DECLARE @CatNuoc INT = (SELECT id FROM categories WHERE name = N'Món nước');
DECLARE @CatBun INT = (SELECT id FROM categories WHERE name = N'Món bún');
DECLARE @CatMi INT = (SELECT id FROM categories WHERE name = N'Món mì');
DECLARE @CatCom INT = (SELECT id FROM categories WHERE name = N'Cơm');
DECLARE @CatBanhMi INT = (SELECT id FROM categories WHERE name = N'Bánh mì');
DECLARE @CatBanh INT = (SELECT id FROM categories WHERE name = N'Món bánh');
DECLARE @CatAnVat INT = (SELECT id FROM categories WHERE name = N'Món ăn vặt');
DECLARE @CatGoi INT = (SELECT id FROM categories WHERE name = N'Món gỏi');
DECLARE @CatMan INT = (SELECT id FROM categories WHERE name = N'Món mặn');
DECLARE @CatHaiSan INT = (SELECT id FROM categories WHERE name = N'Hải sản');
DECLARE @CatLau INT = (SELECT id FROM categories WHERE name = N'Lẩu');
DECLARE @CatTrangMieng INT = (SELECT id FROM categories WHERE name = N'Tráng miệng');
DECLARE @CatDoUong INT = (SELECT id FROM categories WHERE name = N'Đồ uống');

-- 1-4: Món nước
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatNuoc, N'Phở bò đặc biệt Hà Nội', 75000, '1.jpg', 1),
(@CatNuoc, N'Phở gà đồi lá chanh', 65000, '2.jpg', 1),
(@CatNuoc, N'Hủ tiếu Nam Vang sườn tôm', 68000, '3.jpg', 1),
(@CatNuoc, N'Bánh canh cua biển giò heo', 72000, '4.jpg', 1);

-- 5-8: Món bún
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatBun, N'Bún bò Huế giò heo chả cua', 68000, '5.jpg', 1),
(@CatBun, N'Bún chả Hà Nội nướng than hoa', 65000, '6.jpg', 1),
(@CatBun, N'Bún đậu mắm tôm thập cẩm', 70000, '7.jpg', 1),
(@CatBun, N'Bún riêu cua đồng bắp bò', 60000, '8.jpg', 1);

-- 9-12: Món mì
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatMi, N'Mì Quảng tôm thịt trứng cút', 60000, '9.jpg', 1),
(@CatMi, N'Cao lầu Hội An thịt xíu', 62000, '10.jpg', 1),
(@CatMi, N'Mì xào giòn hải sản rau củ', 75000, '11.jpg', 1),
(@CatMi, N'Mì vịt tiềm thuốc bắc hạt sen', 95000, '12.jpg', 1);

-- 13-16: Cơm
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatCom, N'Cơm tấm sườn bì chả trứng ốp la', 68000, '13.jpg', 1),
(@CatCom, N'Cơm gà Hải Nam luộc chấm gừng', 70000, '14.jpg', 1),
(@CatCom, N'Cơm rang dưa bò giòn rụm', 65000, '15.jpg', 1),
(@CatCom, N'Cơm niêu cá kho tộ đậm đà', 85000, '16.jpg', 1);

-- 17-20: Bánh mì
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatBanhMi, N'Bánh mì thịt nướng sả ớt', 35000, '17.jpg', 1),
(@CatBanhMi, N'Bánh mì chảo xíu mại trứng ốp', 55000, '18.jpg', 1),
(@CatBanhMi, N'Bánh mì pate chả lụa truyền thống', 30000, '19.jpg', 1),
(@CatBanhMi, N'Bánh mì bò kho tiêu xanh thơm lừng', 65000, '20.jpg', 1);

-- 21-25: Món bánh
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatBanh, N'Bánh xèo miền Tây tôm nhảy thịt ba chỉ', 55000, '21.jpg', 1),
(@CatBanh, N'Bánh khọt Vũng Tàu tôm tươi', 50000, '22.jpg', 1),
(@CatBanh, N'Bánh bèo chén tôm chấy da heo chiên giòn', 45000, '23.jpg', 1),
(@CatBanh, N'Bánh cuốn nóng thịt băm mộc nhĩ', 45000, '24.jpg', 1),
(@CatBanh, N'Bánh bột lọc Huế nhân tôm thịt', 45000, '25.jpg', 1);

-- 26-29: Món ăn vặt
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatAnVat, N'Gỏi cuốn tôm thịt chấm tương đen', 40000, '26.jpg', 1),
(@CatAnVat, N'Chả giò hải sản sốt mayonnaise', 55000, '27.jpg', 1),
(@CatAnVat, N'Nem lụi nướng sả Nha Trang', 65000, '28.jpg', 1),
(@CatAnVat, N'Ốc bươu nhồi thịt hấp sả gừng', 75000, '29.jpg', 1);

-- 30-33: Món gỏi
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatGoi, N'Gỏi ngó sen tôm thịt chua ngọt', 75000, '30.jpg', 1),
(@CatGoi, N'Gỏi bò bóp thấu mè rang', 85000, '31.jpg', 1),
(@CatGoi, N'Gỏi gà xé phay bắp cải hoa chuối', 75000, '32.jpg', 1),
(@CatGoi, N'Gỏi xoài xanh khô cá sặc', 70000, '33.jpg', 1);

-- 34-38: Món mặn
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatMan, N'Thịt kho tàu nước dừa trứng cút', 75000, '34.jpg', 1),
(@CatMan, N'Cá lóc kho tộ thơm cay', 80000, '35.jpg', 1),
(@CatMan, N'Gà ta kho gừng lá chanh', 75000, '36.jpg', 1),
(@CatMan, N'Sườn non xào chua ngọt', 85000, '37.jpg', 1),
(@CatMan, N'Bò lúc lắc khoai tây chiên giòn', 110000, '38.jpg', 1);

-- 39-42: Hải sản
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatHaiSan, N'Mực sữa hấp gừng hành lá', 125000, '39.jpg', 1),
(@CatHaiSan, N'Tôm sú nướng muối ớt cay nồng', 135000, '40.jpg', 1),
(@CatHaiSan, N'Cua biển hấp bia sả tươi', 195000, '41.jpg', 1),
(@CatHaiSan, N'Nghêu hai còi hấp thái chua cay', 85000, '42.jpg', 1);

-- 43-46: Lẩu
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatLau, N'Lẩu mắm miền Tây cá linh bông điên điển', 280000, '43.jpg', 1),
(@CatLau, N'Lẩu thái hải sản chua cay thập cẩm', 290000, '44.jpg', 1),
(@CatLau, N'Lẩu gà lá é đặc sản Phú Yên', 260000, '45.jpg', 1),
(@CatLau, N'Lẩu cá kèo lá giang thanh mát', 250000, '46.jpg', 1);

-- 47-49: Tráng miệng
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatTrangMieng, N'Chè khúc bạch hạnh nhân', 35000, '47.jpg', 1),
(@CatTrangMieng, N'Sữa chua mít trân châu', 30000, '48.jpg', 1),
(@CatTrangMieng, N'Kem dừa Tràng Tiền', 30000, '49.jpg', 1);

-- 50-53: Đồ uống
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatDoUong, N'Trà sen vàng macchiato', 35000, '50.jpg', 1),
(@CatDoUong, N'Trà đào cam sả', 35000, '51.jpg', 1),
(@CatDoUong, N'Cà phê sữa đá pha phin', 25000, '52.jpg', 1),
(@CatDoUong, N'Nước suối khoáng', 15000, '53.jpg', 1);
GO

-- 4. DINING TABLES (15 Bàn ban đầu, có sẵn QR Code Token và Version = 0)
INSERT INTO dining_tables (name, status, capacity, qr_code_token, version) VALUES
(N'Bàn 01', 'AVAILABLE', 4,  'QR-TABLE-001', 0),
(N'Bàn 02', 'AVAILABLE', 4,  'QR-TABLE-002', 0),
(N'Bàn 03', 'AVAILABLE', 4,  'QR-TABLE-003', 0),
(N'Bàn 04', 'AVAILABLE', 6,  'QR-TABLE-004', 0),
(N'Bàn 05', 'AVAILABLE', 6,  'QR-TABLE-005', 0),
(N'Bàn 06', 'AVAILABLE', 2,  'QR-TABLE-006', 0),
(N'Bàn 07', 'AVAILABLE', 2,  'QR-TABLE-007', 0),
(N'Bàn 08', 'AVAILABLE', 8,  'QR-TABLE-008', 0),
(N'Bàn 09', 'AVAILABLE', 8,  'QR-TABLE-009', 0),
(N'Bàn 10', 'AVAILABLE', 4,  'QR-TABLE-010', 0),
(N'Bàn 11', 'AVAILABLE', 4,  'QR-TABLE-011', 0),
(N'Bàn 12', 'AVAILABLE', 6,  'QR-TABLE-012', 0),
(N'Bàn VIP 01', 'AVAILABLE', 10, 'QR-VIP-001', 0),
(N'Bàn VIP 02', 'AVAILABLE', 10, 'QR-VIP-002', 0),
(N'Sân ngoài 01', 'AVAILABLE', 6, 'QR-OUT-001', 0);
GO

-- 5. CUSTOMERS (Khách hàng mẫu theo các hạng thành viên)
INSERT INTO customers (name, phone, email, loyalty_points, total_spent, membership_tier) VALUES
(N'Nguyễn Thị Lan',    '0901234567', 'lan.nguyen@gmail.com',    250, 2500000, 'SILVER'),
(N'Trần Văn Nam',      '0912345678', 'nam.tran@gmail.com',      520, 5200000, 'GOLD'),
(N'Lê Thị Mai',        '0923456789', NULL,                      50,  500000,  'STANDARD'),
(N'Phạm Văn Hùng',     '0934567890', 'hung.pham@gmail.com',     1000,10000000,'DIAMOND'),
(N'Hoàng Thị Thu',     '0945678901', NULL,                      80,  800000,  'STANDARD');
GO

-- 6. VOUCHERS (Mã giảm giá mẫu)
INSERT INTO vouchers (code, discount_type, discount_val, min_order_amount, max_discount_amount, start_date, end_date, usage_limit, is_active) VALUES
('WELCOME10', 'PERCENT', 10, 100000, 50000,  '2026-01-01', '2026-12-31', 100, 1),
('SAVE50K',   'FIXED',   50000, 200000, NULL,  '2026-01-01', '2026-12-31', 50,  1),
('VIP20',     'PERCENT', 20, 300000, 100000, '2026-01-01', '2026-12-31', 20,  1),
('FREESHIP',  'FIXED',   30000, 150000, NULL,  '2026-01-01', '2026-12-31', 200, 1);
GO

-- 7. SUPPLIERS (Nhà cung cấp hàng hóa)
INSERT INTO suppliers (name, contact_person, phone, email, address) VALUES
(N'Công ty TNHH Thực phẩm Sạch ABC', N'Nguyễn Văn A', '0281234567', 'abc@thucpham.vn', N'Quận 12, TP.HCM'),
(N'Nhà phân phối bia Tiger',          N'Trần Văn B',   '0282345678', 'tiger@dist.vn',   N'Bình Thạnh, TP.HCM'),
(N'Công ty Nước Giải Khát Việt Nam',  N'Lê Thị C',     '0283456789', 'douong@vn.com',    N'Tân Bình, TP.HCM'),
(N'Chợ đầu mối Nông sản Thủ Đức',    N'Phạm Văn D',   '0284567890', 'chodaumoi@thuduc.vn', N'Thủ Đức, TP.HCM');
GO

-- 8. INVENTORY ITEMS (Hàng hóa dễ đếm trong kho)
INSERT INTO inventory_items (supplier_id, name, category, unit, cost_price, stock_qty, min_qty) VALUES
(1, N'Thịt bò thăn thái sẵn', N'Thịt & Hải sản', N'khay (500g)', 150000, 25, 5),
(1, N'Thịt heo ba chỉ rút sườn', N'Thịt & Hải sản', N'kg',        110000, 20, 5),
(1, N'Gà ta làm sạch', N'Thịt & Hải sản', N'con',                90000,  15, 3),
(1, N'Tôm sú tươi sống', N'Thịt & Hải sản', N'kg',               180000, 10, 2),
(1, N'Cua biển Cà Mau', N'Thịt & Hải sản', N'kg',                250000, 8,  2),
(4, N'Rau thơm các loại', N'Rau củ', N'kg',                       20000, 12, 3),
(4, N'Hành hoa & Ngò rí', N'Rau củ', N'kg',                      30000,  8, 2),
(4, N'Cà chua Đà Lạt', N'Rau củ', N'kg',                         25000, 15, 3),
(4, N'Bánh phở tươi Hà Nội', N'Bột & Mì', N'kg',                  35000, 30, 5),
(4, N'Mì trứng tươi sợi vàng', N'Bột & Mì', N'kg',                40000, 20, 5),
(4, N'Gạo ST25 Ông Cua', N'Bột & Mì', N'bao (25kg)',             650000,  6, 2),
(2, N'Bia Tiger lon (thùng 24 lon)', N'Đồ uống', N'thùng',       360000, 15, 5),
(2, N'Bia Heineken lon (thùng 24 lon)', N'Đồ uống', N'thùng',    430000, 12, 3),
(3, N'Coca Cola lon (thùng 24 lon)', N'Đồ uống', N'thùng',       210000, 20, 5),
(3, N'Nước khoáng Lavie 500ml (thùng 24 chai)', N'Đồ uống', N'thùng', 120000, 25, 5),
(4, N'Dầu thực vật Neptune Gold (5L)', N'Gia vị & Dầu', N'can',  195000, 10, 2),
(4, N'Nước mắm Phú Quốc truyền thống (1L)', N'Gia vị & Dầu', N'chai', 65000, 20, 5),
(4, N'Đường cát Biên Hòa (1kg)', N'Gia vị & Dầu', N'gói',        26000, 30, 5),
(4, N'Muối tinh sấy Bạc Liêu (1kg)', N'Gia vị & Dầu', N'gói',    12000, 25, 5),
(4, N'Hộp bã mía Takeaway size L (50 cái)', N'Bao bì', N'lốc',    75000, 10, 2);
GO

-- 9. RESERVATIONS (Lịch đặt bàn mẫu)
INSERT INTO reservations (table_id, customer_name, customer_phone, party_size, reservation_time, deposit_amount, status, channel, note) VALUES
((SELECT id FROM dining_tables WHERE name = N'Bàn VIP 01'), N'Trần Văn Nam', '0912345678', 8, DATEADD(hour, 4, GETDATE()), 500000, 'CONFIRMED', 'HOTLINE', N'Tiệc sinh nhật, cần trang trí nhẹ'),
((SELECT id FROM dining_tables WHERE name = N'Bàn 05'), N'Nguyễn Thị Lan', '0901234567', 4, DATEADD(day, 1, DATEADD(hour, 2, GETDATE())), 200000, 'CONFIRMED', 'ZALO', N'Khách thích ngồi gần cửa sổ');
GO

-- 10. SHIFTS (Ca làm việc mẫu)
INSERT INTO shifts (user_id, start_time, initial_cash, cash_sales, transfer_sales, status, note) VALUES
(2, DATEADD(hour, -3, GETDATE()), 500000, 1500000, 2350000, 'OPEN', N'Ca làm việc sáng thu ngân Trần Thị Thu Ngân');
GO

-- 10.1. SAMPLE ORDERS (Đơn hàng mẫu hiển thị Dashboard, Thu ngân và Bếp)
DECLARE @SeedShiftId INT = (SELECT TOP 1 id FROM shifts WHERE status='OPEN' ORDER BY id DESC);
IF @SeedShiftId IS NULL SET @SeedShiftId = 1;

INSERT INTO orders (shift_id, user_id, table_id, customer_id, order_type, status, payment_method, subtotal, discount_amount, total_amount, created_at, completed_at)
VALUES 
(@SeedShiftId, 1, 1, 1, 'DINE_IN', 'PAID', 'CASH', 285000, 0, 285000, DATEADD(minute, -180, GETDATE()), DATEADD(minute, -150, GETDATE())),
(@SeedShiftId, 2, 3, 2, 'DINE_IN', 'PAID', 'TRANSFER', 560000, 50000, 510000, DATEADD(minute, -140, GETDATE()), DATEADD(minute, -100, GETDATE())),
(@SeedShiftId, 2, NULL, 3, 'TAKEAWAY', 'PAID', 'CASH', 175000, 0, 175000, DATEADD(minute, -95, GETDATE()), DATEADD(minute, -80, GETDATE())),
(@SeedShiftId, 1, 4, NULL, 'DINE_IN', 'PAID', 'TRANSFER', 890000, 0, 890000, DATEADD(minute, -85, GETDATE()), DATEADD(minute, -45, GETDATE())),
(@SeedShiftId, 2, 6, 4, 'DINE_IN', 'PAID', 'CASH', 420000, 20000, 400000, DATEADD(minute, -40, GETDATE()), DATEADD(minute, -15, GETDATE())),
(@SeedShiftId, 1, NULL, NULL, 'TAKEAWAY', 'PAID', 'TRANSFER', 240000, 0, 240000, DATEADD(minute, -25, GETDATE()), DATEADD(minute, -5, GETDATE()));

DECLARE @O1 INT = IDENT_CURRENT('orders') - 5;
DECLARE @O2 INT = IDENT_CURRENT('orders') - 4;
DECLARE @O3 INT = IDENT_CURRENT('orders') - 3;
DECLARE @O4 INT = IDENT_CURRENT('orders') - 2;
DECLARE @O5 INT = IDENT_CURRENT('orders') - 1;
DECLARE @O6 INT = IDENT_CURRENT('orders');

INSERT INTO order_details (order_id, menu_item_id, quantity, unit_price, status, sent_to_kitchen, note) VALUES
(@O1, 1, 2, 75000, 'DONE', 1, N'Ít hành'),
(@O1, 51, 3, 25000, 'DONE', 1, NULL),
(@O1, 49, 2, 30000, 'DONE', 1, NULL),

(@O2, 43, 1, 350000, 'DONE', 1, N'Lẩu chua cay ít ớt'),
(@O2, 31, 2, 85000, 'DONE', 1, NULL),
(@O2, 50, 2, 20000, 'DONE', 1, NULL),

(@O3, 13, 2, 65000, 'DONE', 1, N'Mang về nhiều bì'),
(@O3, 51, 1, 25000, 'DONE', 1, NULL),

(@O4, 38, 2, 220000, 'DONE', 1, N'Tôm càng to'),
(@O4, 39, 1, 280000, 'DONE', 1, NULL),
(@O4, 49, 5, 30000, 'DONE', 1, NULL),

(@O5, 21, 2, 90000, 'DONE', 1, N'Bánh xèo giòn rụm'),
(@O5, 26, 2, 70000, 'DONE', 1, NULL),
(@O5, 52, 4, 15000, 'DONE', 1, NULL),

(@O6, 5, 2, 70000, 'DONE', 1, N'Bún bò đầy đủ'),
(@O6, 50, 2, 35000, 'DONE', 1, NULL);

-- 2 đơn đang phục vụ tại Bàn 02 và Bàn 07
INSERT INTO orders (shift_id, user_id, table_id, customer_id, order_type, status, payment_method, subtotal, discount_amount, total_amount, created_at, completed_at)
VALUES 
(@SeedShiftId, 1, 2, 1, 'DINE_IN', 'IN_PROGRESS', 'CASH', 320000, 0, 320000, DATEADD(minute, -20, GETDATE()), NULL),
(@SeedShiftId, 2, 7, NULL, 'DINE_IN', 'IN_PROGRESS', 'CASH', 450000, 0, 450000, DATEADD(minute, -10, GETDATE()), NULL);

DECLARE @ActiveO1 INT = IDENT_CURRENT('orders') - 1;
DECLARE @ActiveO2 INT = IDENT_CURRENT('orders');

INSERT INTO order_details (order_id, menu_item_id, quantity, unit_price, status, sent_to_kitchen, note) VALUES
(@ActiveO1, 1, 2, 75000, 'COOKING', 1, N'Nóng giòn'),
(@ActiveO1, 28, 1, 95000, 'COOKING', 1, N'Gà rán giòn'),
(@ActiveO1, 51, 2, 25000, 'DONE', 1, NULL),

(@ActiveO2, 42, 1, 350000, 'ACCEPTED', 1, N'Lẩu mắm đặc biệt'),
(@ActiveO2, 30, 2, 50000, 'PENDING', 1, N'Chấm mắm nêm');

UPDATE dining_tables SET status='OCCUPIED' WHERE id IN (2, 7);
UPDATE dining_tables SET status='AVAILABLE' WHERE id NOT IN (2, 7, 13);
UPDATE dining_tables SET status='RESERVED' WHERE id = 13;
GO

-- 11. STOCK MOVEMENTS (Lịch sử nhập hàng đầu kỳ mẫu)
INSERT INTO stock_movements (item_id, type, quantity, unit_price, supplier_id, user_id, note) VALUES
(12, 'IMPORT', 15, 360000, 2, 1, N'Nhập hàng đầu kỳ từ đại lý bia Tiger'),
(14, 'IMPORT', 20, 210000, 3, 1, N'Nhập nước giải khát Coca Cola');
GO

-- 12. INVENTORY AUDITS (Phiếu kiểm kho mẫu)
INSERT INTO inventory_audits (audit_code, user_id, audit_date, status, note) VALUES
('AUDIT-2026-001', 1, DATEADD(day, -1, GETDATE()), 'COMPLETED', N'Kiểm kê định kỳ tuần cuối tháng');

DECLARE @AuditId INT = (SELECT id FROM inventory_audits WHERE audit_code = 'AUDIT-2026-001');
INSERT INTO inventory_audit_details (audit_id, item_id, system_qty, actual_qty, difference_qty, reason) VALUES
(@AuditId, 12, 15, 15, 0, N'Khớp hoàn toàn'),
(@AuditId, 14, 20, 19, -1, N'Hao hụt 1 lon do móp méo');
GO

-- 13. CASH TRANSACTIONS (Phiếu thu - chi mẫu)
INSERT INTO cash_transactions (code, type, category, amount, payment_method, person_name, note, user_id, created_at) VALUES
(N'PT-20261029-001', N'RECEIPT', N'Doanh thu bán hàng', 3850000, N'Tiền mặt', N'Trần Thị Thu Ngân', N'Tiền mặt kết chuyển ca sáng', 1, DATEADD(hour, -5, GETDATE())),
(N'PT-20261029-002', N'RECEIPT', N'Thu tiền cọc đặt bàn', 1000000, N'Chuyển khoản', N'Anh Hoàng (Bàn VIP)', N'Cọc đặt bàn tiệc 10 người tối nay', 1, DATEADD(hour, -3, GETDATE())),
(N'PC-20261029-001', N'PAYMENT', N'Nhập nguyên liệu', 1650000, N'Tiền mặt', N'Chợ đầu mối Nông sản Thủ Đức', N'Thanh toán thịt bò, tôm sú tươi', 1, DATEADD(hour, -6, GETDATE())),
(N'PC-20261029-002', N'PAYMENT', N'Điện, nước & gas', 820000, N'Chuyển khoản', N'Công ty Gas Saigon Petro', N'Đổi 2 bình gas công nghiệp 45kg', 1, DATEADD(hour, -4, GETDATE())),
(N'PC-20261029-003', N'PAYMENT', N'Tạm ứng lương', 500000, N'Tiền mặt', N'Trần Thị Thu Ngân', N'Thu ngân tạm ứng phụ cấp ca', 1, DATEADD(hour, -2, GETDATE())),
(N'PC-20261029-004', N'PAYMENT', N'Mua sắm dụng cụ & vật tư', 240000, N'Tiền mặt', N'Cửa hàng bách hóa', N'Nước rửa chén, khăn lau, túi rác', 1, DATEADD(hour, -1, GETDATE()));
GO

-- 14. EMPLOYEE ATTENDANCES (Chấm công mẫu)
INSERT INTO employee_attendances (user_id, work_date, shift_name, check_in_time, check_out_time, hours_worked, status, note) VALUES
(1, CAST(GETDATE() AS DATE), N'Ca Sáng (08:00 - 16:00)', DATEADD(hour, -6, GETDATE()), NULL, 6.0, N'Đúng giờ', N'Quản lý trực ca sáng'),
(2, CAST(GETDATE() AS DATE), N'Ca Sáng (08:00 - 16:00)', DATEADD(hour, -6, GETDATE()), NULL, 6.0, N'Đúng giờ', N'Thu ngân chính'),
(3, CAST(GETDATE() AS DATE), N'Ca Sáng (08:00 - 16:00)', DATEADD(hour, -6, GETDATE()), NULL, 6.0, N'Đúng giờ', N'Đầu bếp chính');
GO

PRINT N'=====================================================================';
PRINT N'>>> CHÚC MỪNG: Cơ sở dữ liệu RestaurantManagementSys đã được khởi tạo thành công!';
PRINT N'>>> Tài khoản đăng nhập hệ thống:';
PRINT N'    1. Quản lý:  Username: admin    | Mật khẩu: admin123';
PRINT N'    2. Thu ngân: Username: cashier1 | Mật khẩu: cashier123';
PRINT N'    3. Nhà bếp:  Username: kitchen1 | Mật khẩu: kitchen123';
PRINT N'=====================================================================';
GO
