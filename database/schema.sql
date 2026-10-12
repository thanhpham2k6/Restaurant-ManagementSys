-- =====================================================================
-- RESTAURANT MANAGEMENT SYSTEM (JAVA POS) - DATABASE SCHEMA & DATA
-- Engine: Microsoft SQL Server
-- Database: RestaurantManagementSys
-- =====================================================================

IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'RestaurantManagementSys')
BEGIN
    CREATE DATABASE RestaurantManagementSys;
END
GO

USE RestaurantManagementSys;
GO

-- 1. BẢNG USERS (Người dùng hệ thống)
IF OBJECT_ID('dbo.stock_movements', 'U') IS NOT NULL DROP TABLE dbo.stock_movements;
IF OBJECT_ID('dbo.recipes', 'U') IS NOT NULL DROP TABLE dbo.recipes;
IF OBJECT_ID('dbo.order_details', 'U') IS NOT NULL DROP TABLE dbo.order_details;
IF OBJECT_ID('dbo.orders', 'U') IS NOT NULL DROP TABLE dbo.orders;
IF OBJECT_ID('dbo.ingredients', 'U') IS NOT NULL DROP TABLE dbo.ingredients;
IF OBJECT_ID('dbo.menu_items', 'U') IS NOT NULL DROP TABLE dbo.menu_items;
IF OBJECT_ID('dbo.dining_tables', 'U') IS NOT NULL DROP TABLE dbo.dining_tables;
IF OBJECT_ID('dbo.categories', 'U') IS NOT NULL DROP TABLE dbo.categories;
IF OBJECT_ID('dbo.users', 'U') IS NOT NULL DROP TABLE dbo.users;
GO

CREATE TABLE users (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    username      NVARCHAR(50) NOT NULL UNIQUE,
    password_hash NVARCHAR(255) NOT NULL,
    full_name     NVARCHAR(100) NOT NULL,
    role          NVARCHAR(20) NOT NULL CHECK (role IN ('MANAGER','CASHIER','KITCHEN')),
    is_active     BIT NOT NULL DEFAULT 1
);
GO

-- 2. BẢNG CATEGORIES (Danh mục thực đơn)
CREATE TABLE categories (
    id   INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL UNIQUE,
    type NVARCHAR(10) NOT NULL CHECK (type IN ('FOOD','DRINK'))
);
GO

-- 3. BẢNG MENU_ITEMS (Món ăn / Đồ uống)
CREATE TABLE menu_items (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    category_id  INT NOT NULL REFERENCES categories(id),
    name         NVARCHAR(150) NOT NULL,
    price        DECIMAL(12,2) NOT NULL,
    is_available BIT NOT NULL DEFAULT 1,
    version      INT NOT NULL DEFAULT 0
);
GO

-- 4. BẢNG DINING_TABLES (Bàn ăn)
CREATE TABLE dining_tables (
    id       INT IDENTITY(1,1) PRIMARY KEY,
    name     NVARCHAR(50) NOT NULL UNIQUE,
    capacity INT NOT NULL DEFAULT 4,
    status   NVARCHAR(20) NOT NULL DEFAULT 'EMPTY' CHECK (status IN ('EMPTY','OCCUPIED','RESERVED')),
    version  INT NOT NULL DEFAULT 0
);
GO

-- 5. BẢNG ORDERS (Hóa đơn / Đơn hàng)
CREATE TABLE orders (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    table_id        INT NOT NULL REFERENCES dining_tables(id),
    user_id         INT NOT NULL REFERENCES users(id),
    status          NVARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','PAID','CANCELLED')),
    discount_type   NVARCHAR(20) NOT NULL DEFAULT 'NONE' CHECK (discount_type IN ('NONE','VIP','HOLIDAY')),
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    vat_rate        DECIMAL(5,2) NOT NULL DEFAULT 0.10,
    total_amount    DECIMAL(12,2) NOT NULL DEFAULT 0,
    payment_method  NVARCHAR(20) NULL CHECK (payment_method IN ('CASH','TRANSFER')),
    created_at      DATETIME2 NOT NULL DEFAULT GETDATE(),
    paid_at         DATETIME2 NULL
);
GO

-- 6. BẢNG ORDER_DETAILS (Chi tiết món ăn trong đơn)
CREATE TABLE order_details (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    order_id     INT NOT NULL REFERENCES orders(id),
    menu_item_id INT NOT NULL REFERENCES menu_items(id),
    status       NVARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','ACCEPTED','COOKING','DONE','REJECTED')),
    quantity     INT NOT NULL CHECK (quantity > 0),
    unit_price   DECIMAL(12,2) NOT NULL,
    note         NVARCHAR(255) NULL
);
GO

-- 7. BẢNG INGREDIENTS (Kho nguyên vật liệu)
CREATE TABLE ingredients (
    id        INT IDENTITY(1,1) PRIMARY KEY,
    name      NVARCHAR(100) NOT NULL UNIQUE,
    unit      NVARCHAR(20) NOT NULL,
    stock_qty DECIMAL(12,3) NOT NULL DEFAULT 0,
    min_qty   DECIMAL(12,3) NOT NULL DEFAULT 0,
    is_active BIT NOT NULL DEFAULT 1
);
GO

-- 8. BẢNG RECIPES (Định lượng công thức món ăn)
CREATE TABLE recipes (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    menu_item_id    INT NOT NULL REFERENCES menu_items(id),
    ingredient_id   INT NOT NULL REFERENCES ingredients(id),
    qty_per_serving DECIMAL(12,3) NOT NULL,
    CONSTRAINT UQ_Recipe UNIQUE (menu_item_id, ingredient_id)
);
GO

-- 9. BẢNG STOCK_MOVEMENTS (Nhật ký biến động kho)
CREATE TABLE stock_movements (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    ingredient_id INT NOT NULL REFERENCES ingredients(id),
    user_id       INT NOT NULL REFERENCES users(id),
    order_id      INT NULL REFERENCES orders(id),
    type          NVARCHAR(20) NOT NULL CHECK (type IN ('IMPORT','EXPORT','ADJUST','RETURN')),
    quantity      DECIMAL(12,3) NOT NULL,
    note          NVARCHAR(255) NULL,
    created_at    DATETIME2 NOT NULL DEFAULT GETDATE()
);
GO

-- =====================================================================
-- DỮ LIỆU KHỞI TẠO (SEED DATA)
-- =====================================================================

-- 1. Tài khoản mẫu (Mật khẩu cho tất cả: password123)
-- BCrypt hash chính xác cho 'password123':
INSERT INTO users (username, password_hash, full_name, role, is_active) VALUES
('manager',  '$2a$10$K6tqZhFTSRsTY1T51SdBeO1O4ztZiJLBXqYq6RSijq7emwXw22DRS', N'Quản Lý Cửa Hàng', 'MANAGER', 1),
('cashier1', '$2a$10$K6tqZhFTSRsTY1T51SdBeO1O4ztZiJLBXqYq6RSijq7emwXw22DRS', N'Nguyễn Văn Thu Ngân', 'CASHIER', 1),
('kitchen1', '$2a$10$K6tqZhFTSRsTY1T51SdBeO1O4ztZiJLBXqYq6RSijq7emwXw22DRS', N'Trần Văn Bếp Trưởng', 'KITCHEN', 1);
GO

-- 2. Bàn ăn mẫu
INSERT INTO dining_tables (name, capacity, status) VALUES
(N'Bàn 01', 4, 'EMPTY'),
(N'Bàn 02', 4, 'EMPTY'),
(N'Bàn 03', 4, 'EMPTY'),
(N'Bàn 04', 6, 'EMPTY'),
(N'Bàn 05', 6, 'EMPTY'),
(N'Bàn 06', 2, 'EMPTY'),
(N'Bàn 07', 2, 'EMPTY'),
(N'Bàn 08', 8, 'EMPTY'),
(N'Bàn VIP 1', 10, 'EMPTY'),
(N'Bàn VIP 2', 12, 'EMPTY');
GO

-- 3. Danh mục thực đơn
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

-- 4. 50 Món ăn Việt Nam phong phú
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

-- Món nước
INSERT INTO menu_items (category_id, name, price, is_available) VALUES
(@CatNuoc, N'Phở bò tái nạm', 55000, 1),
(@CatNuoc, N'Phở gà ta lá chanh', 50000, 1),
(@CatNuoc, N'Hủ tiếu Nam Vang', 55000, 1),
(@CatNuoc, N'Hủ tiếu mực', 55000, 1),
(@CatNuoc, N'Bánh canh cua', 60000, 1);

-- Món bún
INSERT INTO menu_items (category_id, name, price, is_available) VALUES
(@CatBun, N'Bún chả Hà Nội', 50000, 1),
(@CatBun, N'Bún bò Huế đặc biệt', 60000, 1),
(@CatBun, N'Bún riêu cua đồng', 45000, 1),
(@CatBun, N'Bún thịt nướng chả giò', 50000, 1),
(@CatBun, N'Bún cá Nha Trang', 45000, 1),
(@CatBun, N'Bún đậu mắm tôm thập cẩm', 65000, 1);

-- Món mì
INSERT INTO menu_items (category_id, name, price, is_available) VALUES
(@CatMi, N'Mì Quảng gà trứng', 50000, 1),
(@CatMi, N'Cao lầu Hội An', 55000, 1),
(@CatMi, N'Mì xào giòn hải sản', 65000, 1),
(@CatMi, N'Mì vịt tiềm', 80000, 1);

-- Cơm
INSERT INTO menu_items (category_id, name, price, is_available) VALUES
(@CatCom, N'Cơm tấm sườn bì chả', 55000, 1),
(@CatCom, N'Cơm gà xối mỡ giòn rụm', 55000, 1),
(@CatCom, N'Cơm rang dưa bò', 50000, 1),
(@CatCom, N'Cơm chiên hải sản Dương Châu', 60000, 1),
(@CatCom, N'Cơm niêu cá kho tộ', 70000, 1),
(@CatCom, N'Cơm sườn nướng mật ong', 60000, 1);

-- Bánh mì
INSERT INTO menu_items (category_id, name, price, is_available) VALUES
(@CatBanhMi, N'Bánh mì thịt nướng pate', 35000, 1),
(@CatBanhMi, N'Bánh mì xíu mại trứng muối', 40000, 1),
(@CatBanhMi, N'Bánh mì chảo đặc biệt', 50000, 1);

-- Món bánh
INSERT INTO menu_items (category_id, name, price, is_available) VALUES
(@CatBanh, N'Bánh xèo tôm thịt miền Tây', 50000, 1),
(@CatBanh, N'Bánh cuốn nóng chả lụa', 40000, 1),
(@CatBanh, N'Bánh khọt Vũng Tàu', 45000, 1),
(@CatBanh, N'Bánh bèo tôm cháy chén', 40000, 1);

-- Món ăn vặt
INSERT INTO menu_items (category_id, name, price, is_available) VALUES
(@CatAnVat, N'Nem nướng Nha Trang cuốn', 55000, 1),
(@CatAnVat, N'Gỏi cuốn tôm thịt (4 cuốn)', 40000, 1),
(@CatAnVat, N'Chả giò hải sản giòn rụm', 45000, 1),
(@CatAnVat, N'Khoai tây chiên lắc phô mai', 35000, 1);

-- Món gỏi
INSERT INTO menu_items (category_id, name, price, is_available) VALUES
(@CatGoi, N'Gỏi gà xé phay bắp cải', 60000, 1),
(@CatGoi, N'Gỏi ngó sen tôm thịt', 65000, 1),
(@CatGoi, N'Gỏi bò bóp thấu', 70000, 1);

-- Món mặn
INSERT INTO menu_items (category_id, name, price, is_available) VALUES
(@CatMan, N'Cá kho tộ đậm đà', 70000, 1),
(@CatMan, N'Thịt kho tàu trứng cút', 65000, 1),
(@CatMan, N'Sườn xào chua ngọt', 75000, 1),
(@CatMan, N'Gà ta rang muối giòn', 80000, 1),
(@CatMan, N'Bò lúc lắc khoai tây', 90000, 1);

-- Hải sản
INSERT INTO menu_items (category_id, name, price, is_available) VALUES
(@CatHaiSan, N'Tôm sú rang me', 95000, 1),
(@CatHaiSan, N'Mực tươi xào cần tỏi', 90000, 1),
(@CatHaiSan, N'Cá chẽm chiên sốt chua ngọt', 110000, 1);

-- Lẩu
INSERT INTO menu_items (category_id, name, price, is_available) VALUES
(@CatLau, N'Lẩu Thái tomyum hải sản', 260000, 1),
(@CatLau, N'Lẩu gà lá é đặc sản', 240000, 1),
(@CatLau, N'Lẩu riêu cua bắp bò sườn sụn', 280000, 1);

-- Tráng miệng & Đồ uống
INSERT INTO menu_items (category_id, name, price, is_available) VALUES
(@CatTrangMieng, N'Chè khúc bạch hạnh nhân', 35000, 1),
(@CatTrangMieng, N'Sữa chua mít trân châu', 30000, 1),
(@CatTrangMieng, N'Kem dừa Tràng Tiền', 30000, 1),
(@CatDoUong, N'Trà sen vàng macchiato', 35000, 1),
(@CatDoUong, N'Trà đào cam sả', 35000, 1),
(@CatDoUong, N'Cà phê sữa đá pha phin', 25000, 1),
(@CatDoUong, N'Nước suối khoáng', 15000, 1);
GO

-- 5. Nguyên vật liệu kho ban đầu
INSERT INTO ingredients (name, unit, stock_qty, min_qty, is_active) VALUES
(N'Thịt bò tươi',     N'kg',   50.000, 10.000, 1),
(N'Thịt gà ta',       N'kg',   40.000,  8.000, 1),
(N'Thịt heo sườn',    N'kg',   45.000, 10.000, 1),
(N'Bún tươi',         N'kg',   80.000, 15.000, 1),
(N'Bánh phở tươi',    N'kg',   60.000, 15.000, 1),
(N'Gạo thơm ST25',    N'kg',  100.000, 20.000, 1),
(N'Tôm sú tươi',      N'kg',   25.000,  5.000, 1),
(N'Mực ống tươi',     N'kg',   20.000,  5.000, 1),
(N'Rau sống tổng hợp',N'kg',   30.000,  5.000, 1),
(N'Nước dùng phở/bún',N'lít', 120.000, 25.000, 1),
(N'Trứng gà',         N'quả', 200.000, 30.000, 1);
GO

-- 6. Định lượng công thức mẫu (Recipes) cho các món thông dụng
-- Phở bò tái nạm: 0.15 kg Bò, 0.2 kg Bánh phở, 0.4 lít Nước dùng, 0.05 kg Rau sống
DECLARE @PhoBo INT = (SELECT id FROM menu_items WHERE name = N'Phở bò tái nạm');
DECLARE @IngBo INT = (SELECT id FROM ingredients WHERE name = N'Thịt bò tươi');
DECLARE @IngBanhPho INT = (SELECT id FROM ingredients WHERE name = N'Bánh phở tươi');
DECLARE @IngNuocDung INT = (SELECT id FROM ingredients WHERE name = N'Nước dùng phở/bún');
DECLARE @IngRau INT = (SELECT id FROM ingredients WHERE name = N'Rau sống tổng hợp');

IF @PhoBo IS NOT NULL
BEGIN
    INSERT INTO recipes (menu_item_id, ingredient_id, qty_per_serving) VALUES
    (@PhoBo, @IngBo, 0.150),
    (@PhoBo, @IngBanhPho, 0.200),
    (@PhoBo, @IngNuocDung, 0.400),
    (@PhoBo, @IngRau, 0.050);
END

-- Phở gà: 0.15 kg Gà, 0.2 kg Bánh phở, 0.4 lít Nước dùng
DECLARE @PhoGa INT = (SELECT id FROM menu_items WHERE name = N'Phở gà ta lá chanh');
DECLARE @IngGa INT = (SELECT id FROM ingredients WHERE name = N'Thịt gà ta');
IF @PhoGa IS NOT NULL
BEGIN
    INSERT INTO recipes (menu_item_id, ingredient_id, qty_per_serving) VALUES
    (@PhoGa, @IngGa, 0.150),
    (@PhoGa, @IngBanhPho, 0.200),
    (@PhoGa, @IngNuocDung, 0.400);
END

-- Cơm tấm sườn: 0.18 kg Sườn, 0.15 kg Gạo ST25, 1 quả Trứng gà
DECLARE @ComTam INT = (SELECT id FROM menu_items WHERE name = N'Cơm tấm sườn bì chả');
DECLARE @IngSuon INT = (SELECT id FROM ingredients WHERE name = N'Thịt heo sườn');
DECLARE @IngGao INT = (SELECT id FROM ingredients WHERE name = N'Gạo thơm ST25');
DECLARE @IngTrung INT = (SELECT id FROM ingredients WHERE name = N'Trứng gà');
IF @ComTam IS NOT NULL
BEGIN
    INSERT INTO recipes (menu_item_id, ingredient_id, qty_per_serving) VALUES
    (@ComTam, @IngSuon, 0.180),
    (@ComTam, @IngGao, 0.150),
    (@ComTam, @IngTrung, 1.000);
END

-- Bún bò Huế đặc biệt: 0.12 kg Bò, 0.2 kg Bún tươi, 0.4 lít Nước dùng
DECLARE @BunBo INT = (SELECT id FROM menu_items WHERE name = N'Bún bò Huế đặc biệt');
DECLARE @IngBun INT = (SELECT id FROM ingredients WHERE name = N'Bún tươi');
IF @BunBo IS NOT NULL
BEGIN
    INSERT INTO recipes (menu_item_id, ingredient_id, qty_per_serving) VALUES
    (@BunBo, @IngBo, 0.120),
    (@BunBo, @IngBun, 0.200),
    (@BunBo, @IngNuocDung, 0.400),
    (@BunBo, @IngRau, 0.050);
END
GO
