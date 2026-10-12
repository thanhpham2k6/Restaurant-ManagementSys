-- 2. Tạo bảng Users (Tài khoản người dùng)
CREATE TABLE Users (
    user_id INT PRIMARY KEY IDENTITY(1,1),
    username NVARCHAR(50) UNIQUE NOT NULL,
    password_hash NVARCHAR(255) NOT NULL,
    full_name NVARCHAR(100),
    role NVARCHAR(20) DEFAULT 'Staff',
    created_at DATETIME DEFAULT GETDATE()
);
GO

-- 3. Tạo bảng Categories (Danh mục thực đơn)
CREATE TABLE Categories (
    category_id INT PRIMARY KEY IDENTITY(1,1),
    category_name NVARCHAR(100) NOT NULL
);
GO

-- 4. Tạo bảng MenuItems (Danh sách món ăn)
CREATE TABLE MenuItems (
    item_id INT PRIMARY KEY IDENTITY(1,1),
    category_id INT,
    item_name NVARCHAR(100) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    is_available BIT DEFAULT 1,
    FOREIGN KEY (category_id) REFERENCES Categories(category_id)
);
GO

-- 5. Tạo bảng DiningTables (Danh sách bàn ăn)
CREATE TABLE DiningTables (
    table_id INT PRIMARY KEY IDENTITY(1,1),
    table_name NVARCHAR(50) NOT NULL,
    status NVARCHAR(20) DEFAULT 'Empty' -- Trạng thái: Empty (Trống), Occupied (Đang dùng)
);
GO

-- 6. Tạo bảng Orders (Đơn hàng/Hóa đơn)
CREATE TABLE Orders (
    order_id INT PRIMARY KEY IDENTITY(1,1),
    table_id INT,
    user_id INT,
    total_amount DECIMAL(10, 2) DEFAULT 0.00,
    status NVARCHAR(20) DEFAULT 'Pending', -- Trạng thái: Pending (Chưa thanh toán), Paid (Đã thanh toán)
    order_time DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (table_id) REFERENCES DiningTables(table_id),
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);
GO

-- 7. Tạo bảng OrderDetails (Chi tiết đơn hàng)
CREATE TABLE OrderDetails (
    order_detail_id INT PRIMARY KEY IDENTITY(1,1),
    order_id INT,
    item_id INT,
    quantity INT NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES Orders(order_id),
    FOREIGN KEY (item_id) REFERENCES MenuItems(item_id)
);
GO

-- 8. TẠO DỮ LIỆU MẪU ĐỂ TEST ĐĂNG NHẬP
INSERT INTO Users (username, password_hash, full_name, role) 
VALUES ('admin', '12345', N'Quản trị viên hệ thống', 'Admin');

INSERT INTO Users (username, password_hash, full_name, role) 
VALUES ('thangbv', 'thang2k6', N'Bùi Văn Thắng', 'Staff');
GO
