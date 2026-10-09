-- Thêm danh mục mới (bỏ qua nếu trùng tên)
IF NOT EXISTS (SELECT 1 FROM Categories WHERE category_name = N'Món nước') INSERT INTO Categories (category_name) VALUES (N'Món nước');
IF NOT EXISTS (SELECT 1 FROM Categories WHERE category_name = N'Món bún') INSERT INTO Categories (category_name) VALUES (N'Món bún');
IF NOT EXISTS (SELECT 1 FROM Categories WHERE category_name = N'Món mì') INSERT INTO Categories (category_name) VALUES (N'Món mì');
IF NOT EXISTS (SELECT 1 FROM Categories WHERE category_name = N'Cơm') INSERT INTO Categories (category_name) VALUES (N'Cơm');
IF NOT EXISTS (SELECT 1 FROM Categories WHERE category_name = N'Bánh mì') INSERT INTO Categories (category_name) VALUES (N'Bánh mì');
IF NOT EXISTS (SELECT 1 FROM Categories WHERE category_name = N'Món bánh') INSERT INTO Categories (category_name) VALUES (N'Món bánh');
IF NOT EXISTS (SELECT 1 FROM Categories WHERE category_name = N'Món ăn vặt') INSERT INTO Categories (category_name) VALUES (N'Món ăn vặt');
IF NOT EXISTS (SELECT 1 FROM Categories WHERE category_name = N'Món gỏi') INSERT INTO Categories (category_name) VALUES (N'Món gỏi');
IF NOT EXISTS (SELECT 1 FROM Categories WHERE category_name = N'Món mặn') INSERT INTO Categories (category_name) VALUES (N'Món mặn');
IF NOT EXISTS (SELECT 1 FROM Categories WHERE category_name = N'Hải sản') INSERT INTO Categories (category_name) VALUES (N'Hải sản');
IF NOT EXISTS (SELECT 1 FROM Categories WHERE category_name = N'Lẩu') INSERT INTO Categories (category_name) VALUES (N'Lẩu');
IF NOT EXISTS (SELECT 1 FROM Categories WHERE category_name = N'Tráng miệng') INSERT INTO Categories (category_name) VALUES (N'Tráng miệng');

-- Biến lưu category_id
DECLARE @CatID INT;

-- 1. Phở bò (Món nước)
SELECT @CatID = category_id FROM Categories WHERE category_name = N'Món nước';
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Phở bò', 45000, 1, 100);

-- 2. Phở gà (Món nước)
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Phở gà', 40000, 1, 100);

-- 10. Hủ tiếu (Món nước)
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Hủ tiếu', 45000, 1, 100);

-- Món bún
SELECT @CatID = category_id FROM Categories WHERE category_name = N'Món bún';
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Bún chả', 45000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Bún bò Huế', 50000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Bún riêu', 40000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Bún thịt nướng', 45000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Bún cá', 40000, 1, 100);

-- Món mì
SELECT @CatID = category_id FROM Categories WHERE category_name = N'Món mì';
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Mì Quảng', 45000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Cao lầu', 50000, 1, 100);

-- Cơm
SELECT @CatID = category_id FROM Categories WHERE category_name = N'Cơm';
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Cơm tấm sườn', 50000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Cơm gà xối mỡ', 50000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Cơm rang dưa bò', 45000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Cơm chiên hải sản', 55000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Cơm niêu', 60000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Cơm sườn nướng', 55000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Cơm gà Hội An', 50000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Cơm cá kho', 45000, 1, 100);

-- Bánh mì
SELECT @CatID = category_id FROM Categories WHERE category_name = N'Bánh mì';
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Bánh mì thịt', 30000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Bánh mì xíu mại', 35000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Bánh mì chảo', 45000, 1, 100);

-- Món bánh
SELECT @CatID = category_id FROM Categories WHERE category_name = N'Món bánh';
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Bánh xèo', 40000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Bánh cuốn', 35000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Bánh khọt', 40000, 1, 100);

-- Món ăn vặt
SELECT @CatID = category_id FROM Categories WHERE category_name = N'Món ăn vặt';
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Nem nướng', 45000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Gỏi cuốn', 30000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Chả giò', 35000, 1, 100);

-- Món gỏi
SELECT @CatID = category_id FROM Categories WHERE category_name = N'Món gỏi';
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Gỏi gà', 50000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Gỏi ngó sen', 50000, 1, 100);

-- Món mặn
SELECT @CatID = category_id FROM Categories WHERE category_name = N'Món mặn';
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Cá kho tộ', 60000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Thịt kho trứng', 55000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Sườn xào chua ngọt', 65000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Gà rang muối', 70000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Gà nướng mật ong', 75000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Vịt quay', 80000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Bò lúc lắc', 75000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Bò xào rau củ', 65000, 1, 100);

-- Hải sản
SELECT @CatID = category_id FROM Categories WHERE category_name = N'Hải sản';
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Tôm rang me', 70000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Mực xào cần tỏi', 75000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Cá chiên xù', 65000, 1, 100);

-- Lẩu
SELECT @CatID = category_id FROM Categories WHERE category_name = N'Lẩu';
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Lẩu thái', 180000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Lẩu gà lá é', 200000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Lẩu bò', 220000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Lẩu hải sản', 250000, 1, 100);

-- Tráng miệng
SELECT @CatID = category_id FROM Categories WHERE category_name = N'Tráng miệng';
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Chè khúc bạch', 30000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Chè đậu đen', 20000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Sữa chua mít', 25000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Kem dừa', 30000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Trái cây thập cẩm', 35000, 1, 100);
INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (@CatID, N'Chè thái', 30000, 1, 100);
