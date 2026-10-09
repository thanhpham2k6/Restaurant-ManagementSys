-- ============================================================
--  UPDATE SCRIPT: Thêm bảng Ingredients & cột available_quantity
--  Chạy script này trên SQL Server trước khi chạy ứng dụng
-- ============================================================

-- 1. Thêm cột available_quantity vào MenuItems (nếu chưa có)
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME='MenuItems' AND COLUMN_NAME='available_quantity'
)
ALTER TABLE MenuItems ADD available_quantity INT DEFAULT 0;
GO

-- 2. Tạo bảng Ingredients (Nguyên liệu)
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='Ingredients')
CREATE TABLE Ingredients (
    ingredient_id   INT PRIMARY KEY IDENTITY(1,1),
    ingredient_name NVARCHAR(100) NOT NULL,
    unit            NVARCHAR(30)  NOT NULL,   -- Đơn vị: kg, lít, cái, gói...
    quantity        DECIMAL(10,2) DEFAULT 0,  -- Số lượng tồn kho hiện tại
    min_quantity    DECIMAL(10,2) DEFAULT 0   -- Mức tồn kho tối thiểu (cảnh báo)
);
GO

-- 3. Thêm role Kitchen cho tài khoản test nhà bếp (nếu chưa có)
IF NOT EXISTS (SELECT 1 FROM Users WHERE username = 'kitchen')
INSERT INTO Users (username, password_hash, full_name, role)
VALUES ('kitchen', 'kitchen123', N'Nhà bếp', 'Kitchen');
GO
