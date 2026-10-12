-- Script nạp dữ liệu doanh thu mẫu 7 ngày gần nhất và trạng thái bàn cho Restaurant Management System
USE RestaurantManagementSys;
GO

-- 1. Cập nhật lại trạng thái bàn trống trước
UPDATE dining_tables SET status = 'EMPTY';
UPDATE orders SET status = 'CANCELLED' WHERE status = 'OPEN';

-- 2. Thêm một số công thức nguyên liệu cho các món để hiển thị số suất phong phú
-- menu_items:
-- 1: Phở bò đặc biệt, 2: Phở tái nạm, 3: Bún bò Huế, 4: Bún riêu cua, 5: Cơm tấm sườn bì chả
-- ingredients:
-- 1: Thịt bò (kg), 2: Xương bò (kg), 3: Bánh phở (kg), 4: Hành lá (kg), 5: Gạo tấm (kg), 6: Sườn heo (kg)

-- Bổ sung công thức cho Phở bò đặc biệt (id 1): 0.15kg bò, 0.2kg bánh phở
MERGE recipes AS target USING (SELECT 1 as menu_item_id, 1 as ingredient_id) AS source ON target.menu_item_id=source.menu_item_id AND target.ingredient_id=source.ingredient_id
WHEN MATCHED THEN UPDATE SET qty_per_serving=0.15
WHEN NOT MATCHED THEN INSERT (menu_item_id, ingredient_id, qty_per_serving) VALUES (1, 1, 0.15);

MERGE recipes AS target USING (SELECT 1 as menu_item_id, 3 as ingredient_id) AS source ON target.menu_item_id=source.menu_item_id AND target.ingredient_id=source.ingredient_id
WHEN MATCHED THEN UPDATE SET qty_per_serving=0.20
WHEN NOT MATCHED THEN INSERT (menu_item_id, ingredient_id, qty_per_serving) VALUES (1, 3, 0.20);

-- Bổ sung công thức cho Cơm tấm sườn bì chả (id 5): 0.15kg sườn, 0.2kg gạo tấm
MERGE recipes AS target USING (SELECT 5 as menu_item_id, 6 as ingredient_id) AS source ON target.menu_item_id=source.menu_item_id AND target.ingredient_id=source.ingredient_id
WHEN MATCHED THEN UPDATE SET qty_per_serving=0.15
WHEN NOT MATCHED THEN INSERT (menu_item_id, ingredient_id, qty_per_serving) VALUES (5, 6, 0.15);

MERGE recipes AS target USING (SELECT 5 as menu_item_id, 5 as ingredient_id) AS source ON target.menu_item_id=source.menu_item_id AND target.ingredient_id=source.ingredient_id
WHEN MATCHED THEN UPDATE SET qty_per_serving=0.20
WHEN NOT MATCHED THEN INSERT (menu_item_id, ingredient_id, qty_per_serving) VALUES (5, 5, 0.20);

-- Cập nhật tồn kho dồi dào để các món có số suất thực tế
UPDATE ingredients SET stock_qty = 35.0 WHERE id = 1; -- Thịt bò -> ~233 suất
UPDATE ingredients SET stock_qty = 25.0 WHERE id = 3; -- Bánh phở -> ~125 suất
UPDATE ingredients SET stock_qty = 15.0 WHERE id = 6; -- Sườn heo -> ~100 suất
UPDATE ingredients SET stock_qty = 40.0 WHERE id = 5; -- Gạo tấm -> ~200 suất
UPDATE ingredients SET stock_qty = 1.2, min_qty = 3.0 WHERE id = 4; -- Hành lá cảnh báo sắp hết (1.2kg < 3kg)
GO

-- 3. Tạo hàm hỗ trợ chèn đơn hàng hoàn chỉnh
DECLARE @orderId INT;
DECLARE @now DATETIME2 = GETDATE();

-- === NGÀY -6 ===
-- Đơn 1 (-6 ngày, lúc 11:30)
INSERT INTO orders (table_id, user_id, status, discount_type, discount_amount, vat_rate, total_amount, payment_method, created_at, paid_at)
VALUES (1, 2, 'PAID', 'NONE', 0, 0.10, 242000, 'CASH', DATEADD(minute, 710, DATEADD(day, -6, CAST(CAST(@now AS date) AS datetime2))), DATEADD(minute, 755, DATEADD(day, -6, CAST(CAST(@now AS date) AS datetime2))));
SET @orderId = SCOPE_IDENTITY();
INSERT INTO order_details (order_id, menu_item_id, status, quantity, unit_price, note, sent_to_kitchen) VALUES
(@orderId, 1, 'DONE', 2, 75000, N'Ít bánh phở', 1),
(@orderId, 5, 'DONE', 1, 55000, NULL, 1),
(@orderId, 10, 'DONE', 2, 15000, NULL, 1);

-- Đơn 2 (-6 ngày, lúc 19:15)
INSERT INTO orders (table_id, user_id, status, discount_type, discount_amount, vat_rate, total_amount, payment_method, created_at, paid_at)
VALUES (3, 2, 'PAID', 'VIP', 62000, 0.10, 613800, 'TRANSFER', DATEADD(minute, 1150, DATEADD(day, -6, CAST(CAST(@now AS date) AS datetime2))), DATEADD(minute, 1210, DATEADD(day, -6, CAST(CAST(@now AS date) AS datetime2))));
SET @orderId = SCOPE_IDENTITY();
INSERT INTO order_details (order_id, menu_item_id, status, quantity, unit_price, note, sent_to_kitchen) VALUES
(@orderId, 6, 'DONE', 2, 180000, NULL, 1),
(@orderId, 7, 'DONE', 1, 220000, NULL, 1),
(@orderId, 11, 'DONE', 4, 10000, NULL, 1);

-- === NGÀY -5 ===
-- Đơn 3 (-5 ngày, lúc 12:00)
INSERT INTO orders (table_id, user_id, status, discount_type, discount_amount, vat_rate, total_amount, payment_method, created_at, paid_at)
VALUES (2, 2, 'PAID', 'NONE', 0, 0.10, 495000, 'TRANSFER', DATEADD(minute, 720, DATEADD(day, -5, CAST(CAST(@now AS date) AS datetime2))), DATEADD(minute, 770, DATEADD(day, -5, CAST(CAST(@now AS date) AS datetime2))));
SET @orderId = SCOPE_IDENTITY();
INSERT INTO order_details (order_id, menu_item_id, status, quantity, unit_price, note, sent_to_kitchen) VALUES
(@orderId, 1, 'DONE', 3, 75000, NULL, 1),
(@orderId, 5, 'DONE', 3, 55000, NULL, 1),
(@orderId, 12, 'DONE', 3, 20000, NULL, 1);

-- Đơn 4 (-5 ngày, lúc 18:30)
INSERT INTO orders (table_id, user_id, status, discount_type, discount_amount, vat_rate, total_amount, payment_method, created_at, paid_at)
VALUES (4, 1, 'PAID', 'NONE', 0, 0.10, 880000, 'CASH', DATEADD(minute, 1110, DATEADD(day, -5, CAST(CAST(@now AS date) AS datetime2))), DATEADD(minute, 1180, DATEADD(day, -5, CAST(CAST(@now AS date) AS datetime2))));
SET @orderId = SCOPE_IDENTITY();
INSERT INTO order_details (order_id, menu_item_id, status, quantity, unit_price, note, sent_to_kitchen) VALUES
(@orderId, 7, 'DONE', 2, 220000, NULL, 1),
(@orderId, 6, 'DONE', 2, 180000, NULL, 1);

-- === NGÀY -4 ===
-- Đơn 5 (-4 ngày)
INSERT INTO orders (table_id, user_id, status, discount_type, discount_amount, vat_rate, total_amount, payment_method, created_at, paid_at)
VALUES (5, 2, 'PAID', 'NONE', 0, 0.10, 1430000, 'TRANSFER', DATEADD(minute, 740, DATEADD(day, -4, CAST(CAST(@now AS date) AS datetime2))), DATEADD(minute, 800, DATEADD(day, -4, CAST(CAST(@now AS date) AS datetime2))));
SET @orderId = SCOPE_IDENTITY();
INSERT INTO order_details (order_id, menu_item_id, status, quantity, unit_price, note, sent_to_kitchen) VALUES
(@orderId, 8, 'DONE', 2, 350000, NULL, 1),
(@orderId, 7, 'DONE', 2, 220000, NULL, 1),
(@orderId, 10, 'DONE', 6, 15000, NULL, 1),
(@orderId, 11, 'DONE', 7, 10000, NULL, 1);

-- Đơn 6 (-4 ngày, tối)
INSERT INTO orders (table_id, user_id, status, discount_type, discount_amount, vat_rate, total_amount, payment_method, created_at, paid_at)
VALUES (1, 2, 'PAID', 'VIP', 75000, 0.10, 742500, 'CASH', DATEADD(minute, 1160, DATEADD(day, -4, CAST(CAST(@now AS date) AS datetime2))), DATEADD(minute, 1220, DATEADD(day, -4, CAST(CAST(@now AS date) AS datetime2))));
SET @orderId = SCOPE_IDENTITY();
INSERT INTO order_details (order_id, menu_item_id, status, quantity, unit_price, note, sent_to_kitchen) VALUES
(@orderId, 1, 'DONE', 6, 75000, NULL, 1),
(@orderId, 5, 'DONE', 4, 55000, NULL, 1),
(@orderId, 10, 'DONE', 4, 15000, NULL, 1);

-- === NGÀY -3 ===
-- Đơn 7 (-3 ngày)
INSERT INTO orders (table_id, user_id, status, discount_type, discount_amount, vat_rate, total_amount, payment_method, created_at, paid_at)
VALUES (2, 2, 'PAID', 'NONE', 0, 0.10, 1100000, 'TRANSFER', DATEADD(minute, 730, DATEADD(day, -3, CAST(CAST(@now AS date) AS datetime2))), DATEADD(minute, 790, DATEADD(day, -3, CAST(CAST(@now AS date) AS datetime2))));
SET @orderId = SCOPE_IDENTITY();
INSERT INTO order_details (order_id, menu_item_id, status, quantity, unit_price, note, sent_to_kitchen) VALUES
(@orderId, 8, 'DONE', 2, 350000, NULL, 1),
(@orderId, 6, 'DONE', 1, 180000, NULL, 1),
(@orderId, 12, 'DONE', 6, 20000, NULL, 1);

-- Đơn 8 (-3 ngày, tối)
INSERT INTO orders (table_id, user_id, status, discount_type, discount_amount, vat_rate, total_amount, payment_method, created_at, paid_at)
VALUES (6, 1, 'PAID', 'NONE', 0, 0.10, 935000, 'CASH', DATEADD(minute, 1140, DATEADD(day, -3, CAST(CAST(@now AS date) AS datetime2))), DATEADD(minute, 1200, DATEADD(day, -3, CAST(CAST(@now AS date) AS datetime2))));
SET @orderId = SCOPE_IDENTITY();
INSERT INTO order_details (order_id, menu_item_id, status, quantity, unit_price, note, sent_to_kitchen) VALUES
(@orderId, 1, 'DONE', 4, 75000, NULL, 1),
(@orderId, 7, 'DONE', 2, 220000, NULL, 1),
(@orderId, 10, 'DONE', 5, 15000, NULL, 1),
(@orderId, 11, 'DONE', 4, 10000, NULL, 1);

-- === NGÀY -2 ===
-- Đơn 9 (-2 ngày)
INSERT INTO orders (table_id, user_id, status, discount_type, discount_amount, vat_rate, total_amount, payment_method, created_at, paid_at)
VALUES (3, 2, 'PAID', 'NONE', 0, 0.10, 1650000, 'TRANSFER', DATEADD(minute, 750, DATEADD(day, -2, CAST(CAST(@now AS date) AS datetime2))), DATEADD(minute, 810, DATEADD(day, -2, CAST(CAST(@now AS date) AS datetime2))));
SET @orderId = SCOPE_IDENTITY();
INSERT INTO order_details (order_id, menu_item_id, status, quantity, unit_price, note, sent_to_kitchen) VALUES
(@orderId, 8, 'DONE', 3, 350000, NULL, 1),
(@orderId, 6, 'DONE', 2, 180000, NULL, 1),
(@orderId, 12, 'DONE', 4, 20000, NULL, 1);

-- Đơn 10 (-2 ngày, tối)
INSERT INTO orders (table_id, user_id, status, discount_type, discount_amount, vat_rate, total_amount, payment_method, created_at, paid_at)
VALUES (7, 2, 'PAID', 'HOLIDAY', 55000, 0.10, 1149500, 'TRANSFER', DATEADD(minute, 1170, DATEADD(day, -2, CAST(CAST(@now AS date) AS datetime2))), DATEADD(minute, 1230, DATEADD(day, -2, CAST(CAST(@now AS date) AS datetime2))));
SET @orderId = SCOPE_IDENTITY();
INSERT INTO order_details (order_id, menu_item_id, status, quantity, unit_price, note, sent_to_kitchen) VALUES
(@orderId, 7, 'DONE', 3, 220000, NULL, 1),
(@orderId, 1, 'DONE', 4, 75000, NULL, 1),
(@orderId, 10, 'DONE', 6, 15000, NULL, 1);

-- === NGÀY -1 (Hôm qua) ===
-- Đơn 11 (Hôm qua, trưa)
INSERT INTO orders (table_id, user_id, status, discount_type, discount_amount, vat_rate, total_amount, payment_method, created_at, paid_at)
VALUES (4, 2, 'PAID', 'NONE', 0, 0.10, 1375000, 'CASH', DATEADD(minute, 725, DATEADD(day, -1, CAST(CAST(@now AS date) AS datetime2))), DATEADD(minute, 785, DATEADD(day, -1, CAST(CAST(@now AS date) AS datetime2))));
SET @orderId = SCOPE_IDENTITY();
INSERT INTO order_details (order_id, menu_item_id, status, quantity, unit_price, note, sent_to_kitchen) VALUES
(@orderId, 1, 'DONE', 5, 75000, NULL, 1),
(@orderId, 8, 'DONE', 2, 350000, NULL, 1),
(@orderId, 10, 'DONE', 7, 15000, NULL, 1),
(@orderId, 11, 'DONE', 7, 10000, NULL, 1);

-- Đơn 12 (Hôm qua, tối)
INSERT INTO orders (table_id, user_id, status, discount_type, discount_amount, vat_rate, total_amount, payment_method, created_at, paid_at)
VALUES (8, 1, 'PAID', 'VIP', 120000, 0.10, 1188000, 'TRANSFER', DATEADD(minute, 1155, DATEADD(day, -1, CAST(CAST(@now AS date) AS datetime2))), DATEADD(minute, 1225, DATEADD(day, -1, CAST(CAST(@now AS date) AS datetime2))));
SET @orderId = SCOPE_IDENTITY();
INSERT INTO order_details (order_id, menu_item_id, status, quantity, unit_price, note, sent_to_kitchen) VALUES
(@orderId, 7, 'DONE', 3, 220000, NULL, 1),
(@orderId, 6, 'DONE', 2, 180000, NULL, 1),
(@orderId, 12, 'DONE', 5, 20000, NULL, 1);

-- === HÔM NAY (Day 0) ===
-- Đơn 13 (Hôm nay, trưa)
INSERT INTO orders (table_id, user_id, status, discount_type, discount_amount, vat_rate, total_amount, payment_method, created_at, paid_at)
VALUES (1, 2, 'PAID', 'NONE', 0, 0.10, 825000, 'CASH', DATEADD(minute, 690, CAST(CAST(@now AS date) AS datetime2)), DATEADD(minute, 740, CAST(CAST(@now AS date) AS datetime2)));
SET @orderId = SCOPE_IDENTITY();
INSERT INTO order_details (order_id, menu_item_id, status, quantity, unit_price, note, sent_to_kitchen) VALUES
(@orderId, 1, 'DONE', 4, 75000, NULL, 1),
(@orderId, 5, 'DONE', 4, 55000, NULL, 1),
(@orderId, 10, 'DONE', 5, 15000, NULL, 1);

-- Đơn 14 (Hôm nay, chiều)
INSERT INTO orders (table_id, user_id, status, discount_type, discount_amount, vat_rate, total_amount, payment_method, created_at, paid_at)
VALUES (3, 2, 'PAID', 'NONE', 0, 0.10, 1265000, 'TRANSFER', DATEADD(minute, 860, CAST(CAST(@now AS date) AS datetime2)), DATEADD(minute, 920, CAST(CAST(@now AS date) AS datetime2)));
SET @orderId = SCOPE_IDENTITY();
INSERT INTO order_details (order_id, menu_item_id, status, quantity, unit_price, note, sent_to_kitchen) VALUES
(@orderId, 8, 'DONE', 2, 350000, NULL, 1),
(@orderId, 6, 'DONE', 2, 180000, NULL, 1),
(@orderId, 12, 'DONE', 3, 20000, NULL, 1),
(@orderId, 11, 'DONE', 3, 10000, NULL, 1);

-- Đơn 15 (Hôm nay, tối vừa thanh toán)
INSERT INTO orders (table_id, user_id, status, discount_type, discount_amount, vat_rate, total_amount, payment_method, created_at, paid_at)
VALUES (5, 1, 'PAID', 'VIP', 65000, 0.10, 643500, 'TRANSFER', DATEADD(minute, -45, @now), DATEADD(minute, -5, @now));
SET @orderId = SCOPE_IDENTITY();
INSERT INTO order_details (order_id, menu_item_id, status, quantity, unit_price, note, sent_to_kitchen) VALUES
(@orderId, 7, 'DONE', 2, 220000, NULL, 1),
(@orderId, 1, 'DONE', 2, 75000, NULL, 1),
(@orderId, 10, 'DONE', 4, 15000, NULL, 1);

-- 4. Tạo 1 bàn đang có khách (Bàn 02) có đơn OPEN để thử nghiệm Thu ngân & Bếp
UPDATE dining_tables SET status = 'OCCUPIED' WHERE id = 2;

INSERT INTO orders (table_id, user_id, status, discount_type, discount_amount, vat_rate, total_amount, payment_method, created_at, paid_at)
VALUES (2, 2, 'OPEN', 'NONE', 0, 0.10, 0, NULL, DATEADD(minute, -20, @now), NULL);
SET @orderId = SCOPE_IDENTITY();

-- 1 món đã gửi bếp (bếp đã nhận đang nấu)
INSERT INTO order_details (order_id, menu_item_id, status, quantity, unit_price, note, sent_to_kitchen)
VALUES (@orderId, 1, 'COOKING', 2, 75000, N'Ít cay', 1);

-- 1 món vừa thêm vào hoá đơn NHƯNG CHƯA GỬI BẾP (sent_to_kitchen = 0)
INSERT INTO order_details (order_id, menu_item_id, status, quantity, unit_price, note, sent_to_kitchen)
VALUES (@orderId, 5, 'PENDING', 1, 55000, N'Thêm chả', 0);

PRINT N'Da tao xong du lieu mau 7 ngay va ban dang phuc vu!';
GO
