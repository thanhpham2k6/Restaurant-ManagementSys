-- =====================================================================
-- RESTAURANT MANAGEMENT SYSTEM v3.0 - DATABASE SCHEMA
-- Engine: Microsoft SQL Server 2019/2022
-- Database: RestaurantManagementSys
-- 15 Tables, No Recipes/Ingredients
-- =====================================================================

IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'RestaurantManagementSys')
BEGIN
    CREATE DATABASE RestaurantManagementSys;
END
GO

USE RestaurantManagementSys;
GO

-- ==================== DROP ALL TABLES (CASCADE ORDER) ====================
-- Old tables cleanup first
IF OBJECT_ID('dbo.recipes','U') IS NOT NULL DROP TABLE dbo.recipes;
IF OBJECT_ID('dbo.ingredients','U') IS NOT NULL DROP TABLE dbo.ingredients;

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
    reservation_time DATETIME2     NOT NULL,
    guest_count      INT           NOT NULL DEFAULT 2,
    deposit_amount   DECIMAL(15,2) NOT NULL DEFAULT 0,
    status           NVARCHAR(15)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','CONFIRMED','SEATED','CANCELLED')),
    channel          NVARCHAR(30)  NOT NULL DEFAULT 'DIRECT' CHECK (channel IN ('HOTLINE','ZALO','FACEBOOK','DIRECT','WEBSITE')),
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
    id               INT IDENTITY(1,1) PRIMARY KEY,
    order_id         INT           NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id     INT           NOT NULL REFERENCES menu_items(id),
    status           NVARCHAR(20)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','COOKING','DONE','REJECTED')),
    quantity         INT           NOT NULL DEFAULT 1,
    unit_price       DECIMAL(12,2) NOT NULL,
    note             NVARCHAR(255) NULL,
    sent_to_kitchen  BIT           NOT NULL DEFAULT 0
);
GO

-- ==================== 11. SUPPLIERS ====================
CREATE TABLE suppliers (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    name            NVARCHAR(150) NOT NULL UNIQUE,
    contact_person  NVARCHAR(100) NULL,
    phone           NVARCHAR(20)  NULL,
    email           NVARCHAR(150) NULL,
    address         NVARCHAR(255) NULL,
    is_active       BIT           NOT NULL DEFAULT 1
);
GO

-- ==================== 12. INVENTORY_ITEMS ====================
CREATE TABLE inventory_items (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    supplier_id INT           NULL REFERENCES suppliers(id),
    name        NVARCHAR(150) NOT NULL UNIQUE,
    category    NVARCHAR(100) NULL,
    unit        NVARCHAR(30)  NOT NULL,
    cost_price  DECIMAL(12,2) NOT NULL DEFAULT 0,
    stock_qty   DECIMAL(12,2) NOT NULL DEFAULT 0,
    min_qty     DECIMAL(12,2) NOT NULL DEFAULT 0,
    is_active   BIT           NOT NULL DEFAULT 1
);
GO

-- ==================== 13. STOCK_MOVEMENTS ====================
CREATE TABLE stock_movements (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    item_id     INT           NOT NULL REFERENCES inventory_items(id),
    supplier_id INT           NULL REFERENCES suppliers(id),
    user_id     INT           NOT NULL REFERENCES users(id),
    type        NVARCHAR(20)  NOT NULL CHECK (type IN ('IMPORT','EXPORT','ADJUST','RETURN','ADJUST_AUDIT')),
    quantity    DECIMAL(12,2) NOT NULL,
    unit_price  DECIMAL(12,2) NOT NULL DEFAULT 0,
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
    status     NVARCHAR(15)  NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS','COMPLETED')),
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

-- ==================== INDEXES ====================
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
-- SEED DATA
-- =====================================================================

-- USERS (password: admin123 | cashier123 | kitchen123)
INSERT INTO users (username, password_hash, full_name, role) VALUES
('admin',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', N'Nguyễn Văn Quản Lý', 'MANAGER'),
('cashier1','$2a$10$8K1p/a0dR1lVFKq6xcJSLOUuVcVfEELb7D4eoKjVYpAi3bJQIvWri', N'Trần Thị Thu Ngân', 'CASHIER'),
('kitchen1','$2a$10$8K1p/a0dR1lVFKq6xcJSLOUuVcVfEELb7D4eoKjVYpAi3bJQIvWri', N'Lê Văn Đầu Bếp', 'KITCHEN');
GO

-- CATEGORIES (13 Danh mục thực đơn gốc)
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

-- MENU ITEMS (53 Món ăn Việt Nam chuẩn - Khớp chính xác 1:1 với ảnh 1.jpg -> 53.jpg)
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

-- 1-5: Món nước
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatNuoc, N'Phở bò tái nạm', 55000, '1.jpg', 1),
(@CatNuoc, N'Phở gà ta lá chanh', 50000, '2.jpg', 1),
(@CatNuoc, N'Hủ tiếu Nam Vang', 55000, '3.jpg', 1),
(@CatNuoc, N'Hủ tiếu mực', 55000, '4.jpg', 1),
(@CatNuoc, N'Bánh canh cua', 60000, '5.jpg', 1);

-- 6-11: Món bún
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatBun, N'Bún chả Hà Nội', 50000, '6.jpg', 1),
(@CatBun, N'Bún bò Huế đặc biệt', 60000, '7.jpg', 1),
(@CatBun, N'Bún riêu cua đồng', 45000, '8.jpg', 1),
(@CatBun, N'Bún thịt nướng chả giò', 50000, '9.jpg', 1),
(@CatBun, N'Bún cá Nha Trang', 45000, '10.jpg', 1),
(@CatBun, N'Bún đậu mắm tôm thập cẩm', 65000, '11.jpg', 1);

-- 12-15: Món mì
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatMi, N'Mì Quảng gà trứng', 50000, '12.jpg', 1),
(@CatMi, N'Cao lầu Hội An', 55000, '13.jpg', 1),
(@CatMi, N'Mì xào giòn hải sản', 65000, '14.jpg', 1),
(@CatMi, N'Mì vịt tiềm', 80000, '15.jpg', 1);

-- 16-21: Cơm
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatCom, N'Cơm tấm sườn bì chả', 55000, '16.jpg', 1),
(@CatCom, N'Cơm gà xối mỡ giòn rụm', 55000, '17.jpg', 1),
(@CatCom, N'Cơm rang dưa bò', 50000, '18.jpg', 1),
(@CatCom, N'Cơm chiên hải sản Dương Châu', 60000, '19.jpg', 1),
(@CatCom, N'Cơm niêu cá kho tộ', 70000, '20.jpg', 1),
(@CatCom, N'Cơm sườn nướng mật ong', 60000, '21.jpg', 1);

-- 22-24: Bánh mì
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatBanhMi, N'Bánh mì thịt nướng pate', 35000, '22.jpg', 1),
(@CatBanhMi, N'Bánh mì xíu mại trứng muối', 40000, '23.jpg', 1),
(@CatBanhMi, N'Bánh mì chảo đặc biệt', 50000, '24.jpg', 1);

-- 25-28: Món bánh
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatBanh, N'Bánh xèo tôm thịt miền Tây', 50000, '25.jpg', 1),
(@CatBanh, N'Bánh cuốn nóng chả lụa', 40000, '26.jpg', 1),
(@CatBanh, N'Bánh khọt Vũng Tàu', 45000, '27.jpg', 1),
(@CatBanh, N'Bánh bèo tôm cháy chén', 40000, '28.jpg', 1);

-- 29-32: Món ăn vặt
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatAnVat, N'Nem nướng Nha Trang cuốn', 55000, '29.jpg', 1),
(@CatAnVat, N'Gỏi cuốn tôm thịt (4 cuốn)', 40000, '30.jpg', 1),
(@CatAnVat, N'Chả giò hải sản giòn rụm', 45000, '31.jpg', 1),
(@CatAnVat, N'Khoai tây chiên lắc phô mai', 35000, '32.jpg', 1);

-- 33-35: Món gỏi
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatGoi, N'Gỏi gà xé phay bắp cải', 60000, '33.jpg', 1),
(@CatGoi, N'Gỏi ngó sen tôm thịt', 65000, '34.jpg', 1),
(@CatGoi, N'Gỏi bò bóp thấu', 70000, '35.jpg', 1);

-- 36-40: Món mặn
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatMan, N'Cá kho tộ đậm đà', 70000, '36.jpg', 1),
(@CatMan, N'Thịt kho tàu trứng cút', 65000, '37.jpg', 1),
(@CatMan, N'Sườn xào chua ngọt', 75000, '38.jpg', 1),
(@CatMan, N'Gà ta rang muối giòn', 80000, '39.jpg', 1),
(@CatMan, N'Bò lúc lắc khoai tây', 90000, '40.jpg', 1);

-- 41-43: Hải sản
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatHaiSan, N'Tôm sú rang me', 95000, '41.jpg', 1),
(@CatHaiSan, N'Mực tươi xào cần tỏi', 90000, '42.jpg', 1),
(@CatHaiSan, N'Cá chẽm chiên sốt chua ngọt', 110000, '43.jpg', 1);

-- 44-46: Lẩu
INSERT INTO menu_items (category_id, name, price, image_path, is_available) VALUES
(@CatLau, N'Lẩu Thái tomyum hải sản', 260000, '44.jpg', 1),
(@CatLau, N'Lẩu gà lá é đặc sản', 240000, '45.jpg', 1),
(@CatLau, N'Lẩu riêu cua bắp bò sườn sụn', 280000, '46.jpg', 1);

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

-- DINING TABLES (15 tables)
INSERT INTO dining_tables (name, status, capacity, qr_code_token) VALUES
(N'Bàn 01', 'AVAILABLE', 4,  'QR-TABLE-001'),
(N'Bàn 02', 'AVAILABLE', 4,  'QR-TABLE-002'),
(N'Bàn 03', 'AVAILABLE', 4,  'QR-TABLE-003'),
(N'Bàn 04', 'AVAILABLE', 6,  'QR-TABLE-004'),
(N'Bàn 05', 'AVAILABLE', 6,  'QR-TABLE-005'),
(N'Bàn 06', 'AVAILABLE', 2,  'QR-TABLE-006'),
(N'Bàn 07', 'AVAILABLE', 2,  'QR-TABLE-007'),
(N'Bàn 08', 'AVAILABLE', 8,  'QR-TABLE-008'),
(N'Bàn 09', 'AVAILABLE', 8,  'QR-TABLE-009'),
(N'Bàn 10', 'AVAILABLE', 4,  'QR-TABLE-010'),
(N'Bàn 11', 'AVAILABLE', 4,  'QR-TABLE-011'),
(N'Bàn 12', 'AVAILABLE', 6,  'QR-TABLE-012'),
(N'Bàn VIP 01', 'AVAILABLE', 10, 'QR-VIP-001'),
(N'Bàn VIP 02', 'AVAILABLE', 10, 'QR-VIP-002'),
(N'Sân ngoài 01', 'AVAILABLE', 6, 'QR-OUT-001');
GO

-- CUSTOMERS
INSERT INTO customers (name, phone, email, loyalty_points, total_spent, membership_tier) VALUES
(N'Nguyễn Thị Lan',    '0901234567', 'lan.nguyen@gmail.com',    250, 2500000, 'SILVER'),
(N'Trần Văn Nam',      '0912345678', 'nam.tran@gmail.com',      520, 5200000, 'GOLD'),
(N'Lê Thị Mai',        '0923456789', NULL,                      50,  500000,  'STANDARD'),
(N'Phạm Văn Hùng',     '0934567890', 'hung.pham@gmail.com',     1000,10000000,'DIAMOND'),
(N'Hoàng Thị Thu',     '0945678901', NULL,                      80,  800000,  'STANDARD');
GO

-- VOUCHERS
INSERT INTO vouchers (code, discount_type, discount_val, min_order_amount, max_discount_amount, start_date, end_date, usage_limit, is_active) VALUES
('WELCOME10', 'PERCENT', 10, 100000, 50000,  '2026-01-01', '2026-12-31', 100, 1),
('SAVE50K',   'FIXED',   50000, 200000, NULL,  '2026-01-01', '2026-12-31', 50,  1),
('VIP20',     'PERCENT', 20, 300000, 100000, '2026-01-01', '2026-12-31', 20,  1),
('FREESHIP',  'FIXED',   30000, 150000, NULL,  '2026-10-01', '2026-10-31', 200, 1);
GO

-- SUPPLIERS
INSERT INTO suppliers (name, contact_person, phone, email) VALUES
(N'Công ty TNHH Thực phẩm Sạch ABC', N'Nguyễn Văn A', '0281234567', 'abc@thucpham.vn'),
(N'Nhà phân phối bia Tiger',          N'Trần Văn B',   '0282345678', 'tiger@dist.vn'),
(N'Công ty đồ uống Việt Nam',         N'Lê Thị C',     '0283456789', 'douan@vn.com'),
(N'Chợ đầu mối Thủ Đức',             NULL,             '0284567890', NULL);
GO

-- INVENTORY ITEMS
INSERT INTO inventory_items (supplier_id, name, category, unit, cost_price, stock_qty, min_qty) VALUES
(1, N'Thịt bò (thăn)', N'Thịt & Hải sản', N'kg',   150000, 20,  5),
(1, N'Thịt heo (ba chỉ)', N'Thịt & Hải sản', N'kg', 110000, 15,  5),
(1, N'Gà nguyên con', N'Thịt & Hải sản', N'con',    80000,  10,  3),
(1, N'Tôm tươi', N'Thịt & Hải sản', N'kg',          180000, 8,   2),
(1, N'Cua đồng', N'Thịt & Hải sản', N'kg',           120000, 5,   2),
(1, N'Rau sống các loại', N'Rau củ', N'kg',           20000,  10,  3),
(1, N'Hành lá', N'Rau củ', N'kg',                    30000,  5,   1),
(1, N'Cà chua', N'Rau củ', N'kg',                    25000,  8,   2),
(4, N'Bánh phở khô', N'Bột & Mì', N'kg',             40000,  20,  5),
(4, N'Mì trứng', N'Bột & Mì', N'kg',                 35000,  15,  5),
(4, N'Gạo tẻ ST25', N'Bột & Mì', N'kg',              30000,  50,  10),
(2, N'Bia Tiger lon', N'Đồ uống', N'thùng (24lon)',  350000, 10,  3),
(2, N'Bia Heineken lon', N'Đồ uống', N'thùng (24lon)', 420000, 8, 2),
(3, N'Coca Cola lon', N'Đồ uống', N'thùng (24lon)',  200000, 15,  5),
(3, N'Nước suối Lavie (500ml)', N'Đồ uống', N'thùng (24chai)', 120000, 20, 5),
(4, N'Dầu ăn Neptune', N'Gia vị & Dầu', N'chai (5L)', 80000, 10, 3),
(4, N'Nước mắm Phú Quốc', N'Gia vị & Dầu', N'chai (1L)', 45000, 20, 5),
(4, N'Đường tinh luyện', N'Gia vị & Dầu', N'kg',     25000,  15,  5),
(4, N'Muối i-ốt', N'Gia vị & Dầu', N'kg',            10000,  10,  3),
(4, N'Hộp đựng takeaway (L)', N'Dụng cụ', N'hộp',    1500,   200, 50);
GO

PRINT N'Schema v3.0 đã được tạo thành công!';
GO
