# -*- coding: utf-8 -*-
"""
Chương trình tạo file Word (.docx) Báo cáo Bài tập lớn Hệ thống Quản lý Nhà hàng POS
Quy chuẩn trình bày: Trường Đại học Công nghệ Giao thông Vận tải (UTT)
Khoa Công nghệ Thông tin
"""

import os
import sys
import docx
from docx.shared import Inches, Pt, RGBColor, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import parse_xml, OxmlElement
from docx.oxml.ns import nsdecls, qn

# Thêm thư mục hiện tại vào sys.path để import helper
cur_dir = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, cur_dir)
sys.path.insert(0, os.path.dirname(cur_dir))

from report_builder import (
    set_cell_background, set_cell_margins, set_table_borders,
    format_table_header, format_data_rows,
    add_heading_1, add_heading_2, add_heading_3,
    add_p, add_bullet, add_callout, add_figure_placeholder
)

def create_full_report(output_filename="BAO_CAO_BAI_TAP_LON_NHA_HANG.docx"):
    doc = docx.Document()

    # Thiết lập căn lề chuẩn Đại học: Trái 3.0 cm, Phải 2.0 cm, Trên 2.0 cm, Dưới 2.0 cm
    sections = doc.sections
    for section in sections:
        section.top_margin = Cm(2.0)
        section.bottom_margin = Cm(2.0)
        section.left_margin = Cm(3.0)
        section.right_margin = Cm(2.0)

    # =========================================================================
    # TRANG BÌA (COVER PAGE)
    # =========================================================================
    # Tạo khung viền đôi trang trọng bao quanh trang bìa bằng bảng 1 ô
    cover_table = doc.add_table(rows=1, cols=1)
    cover_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    cover_cell = cover_table.cell(0, 0)
    cover_cell.width = Inches(6.5)
    set_cell_margins(cover_cell, top=280, bottom=280, left=280, right=280)
    set_cell_background(cover_cell, "FFFFFF")
    
    # Border: double outer border
    tcPr = cover_cell._tc.get_or_add_tcPr()
    borders = parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'<w:top w:val="double" w:sz="18" w:space="0" w:color="1E3A8A"/>'
        f'<w:left w:val="double" w:sz="18" w:space="0" w:color="1E3A8A"/>'
        f'<w:bottom w:val="double" w:sz="18" w:space="0" w:color="1E3A8A"/>'
        f'<w:right w:val="double" w:sz="18" w:space="0" w:color="1E3A8A"/>'
        f'</w:tcBorders>'
    )
    tcPr.append(borders)

    cp = cover_cell.paragraphs[0]
    cp.alignment = WD_ALIGN_PARAGRAPH.CENTER
    cp.paragraph_format.space_before = Pt(0)
    cp.paragraph_format.space_after = Pt(2)
    
    r = cp.add_run("TRƯỜNG ĐẠI HỌC CÔNG NGHỆ GIAO THÔNG VẬN TẢI\n")
    r.font.name = "Times New Roman"
    r.font.size = Pt(13)
    r.font.bold = True
    r.font.color.rgb = RGBColor(30, 58, 138)
    
    r = cp.add_run("KHOA CÔNG NGHỆ THÔNG TIN\n")
    r.font.name = "Times New Roman"
    r.font.size = Pt(13)
    r.font.bold = True
    r.font.color.rgb = RGBColor(30, 58, 138)

    r = cp.add_run("o0o\n\n")
    r.font.name = "Times New Roman"
    r.font.size = Pt(12)
    r.font.bold = True

    # Logo placeholder / symbol
    r = cp.add_run("❖❖❖❖❖\n\n\n")
    r.font.name = "Times New Roman"
    r.font.size = Pt(18)
    r.font.bold = True
    r.font.color.rgb = RGBColor(15, 118, 110)

    r = cp.add_run("BÁO CÁO ĐỀ TÀI BÀI TẬP LỚN\n")
    r.font.name = "Times New Roman"
    r.font.size = Pt(16)
    r.font.bold = True
    r.font.color.rgb = RGBColor(15, 23, 42)

    r = cp.add_run("MÔN HỌC: LẬP TRÌNH ỨNG DỤNG JAVA NÂNG CAO\n\n\n")
    r.font.name = "Times New Roman"
    r.font.size = Pt(14)
    r.font.bold = True
    r.font.color.rgb = RGBColor(180, 83, 9)

    r = cp.add_run("TÊN ĐỀ TÀI:\n")
    r.font.name = "Times New Roman"
    r.font.size = Pt(13)
    r.font.bold = True
    r.font.color.rgb = RGBColor(100, 116, 139)

    r = cp.add_run("XÂY DỰNG HỆ THỐNG QUẢN LÝ NHÀ HÀNG TOÀN DIỆN\n")
    r.font.name = "Times New Roman"
    r.font.size = Pt(18)
    r.font.bold = True
    r.font.color.rgb = RGBColor(30, 58, 138)

    r = cp.add_run("(RESTAURANT MANAGEMENT SYSTEM & POS)\n\n\n\n")
    r.font.name = "Times New Roman"
    r.font.size = Pt(13)
    r.font.bold = True
    r.font.italic = True
    r.font.color.rgb = RGBColor(71, 85, 105)

    # Khung thông tin Giảng viên & Sinh viên
    info_p = cover_cell.add_paragraph()
    info_p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    info_p.paragraph_format.left_indent = Inches(0.8)
    info_p.paragraph_format.space_before = Pt(10)
    info_p.paragraph_format.space_after = Pt(2)
    info_p.paragraph_format.line_spacing = 1.3

    r = info_p.add_run("Giảng viên hướng dẫn:\t")
    r.font.name = "Times New Roman"
    r.font.size = Pt(13)
    r.font.bold = True
    r = info_p.add_run("ThS. Nguyễn Thị Lan Anh\n")
    r.font.name = "Times New Roman"
    r.font.size = Pt(13)

    r = info_p.add_run("Lớp học phần:\t\t")
    r.font.name = "Times New Roman"
    r.font.size = Pt(13)
    r.font.bold = True
    r = info_p.add_run("75DCHT21\n")
    r.font.name = "Times New Roman"
    r.font.size = Pt(13)

    r = info_p.add_run("Nhóm sinh viên thực hiện:\t")
    r.font.name = "Times New Roman"
    r.font.size = Pt(13)
    r.font.bold = True
    r = info_p.add_run("Nhóm 13\n")
    r.font.name = "Times New Roman"
    r.font.size = Pt(13)
    r.font.bold = True

    members = [
        "1. Phạm Văn Thanh (Nhóm trưởng)",
        "2. Bùi Văn Thắng",
        "3. Trần Tuấn Thanh",
        "4. Bùi Đức Thịnh"
    ]
    for m in members:
        r_m = info_p.add_run(f"\t\t\t{m}\n")
        r_m.font.name = "Times New Roman"
        r_m.font.size = Pt(12.5)

    date_p = cover_cell.add_paragraph()
    date_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    date_p.paragraph_format.space_before = Pt(50)
    date_p.paragraph_format.space_after = Pt(10)
    r_date = date_p.add_run("Hà Nội, tháng 10 năm 2026")
    r_date.font.name = "Times New Roman"
    r_date.font.size = Pt(13)
    r_date.font.bold = True

    # Ngắt trang sau trang bìa
    doc.add_page_break()

    # =========================================================================
    # MỤC LỤC & DANH MỤC TÀI LIỆU
    # =========================================================================
    h_toc = doc.add_paragraph()
    h_toc.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r_toc = h_toc.add_run("MỤC LỤC TỔNG QUAN")
    r_toc.font.name = "Times New Roman"
    r_toc.font.size = Pt(18)
    r_toc.font.bold = True
    r_toc.font.color.rgb = RGBColor(30, 58, 138)
    h_toc.paragraph_format.space_after = Pt(15)

    toc_items = [
        ("PHẦN I: GIỚI THIỆU", "1"),
        ("1. Lời giới thiệu", "1"),
        ("2. Mục đích đề tài", "2"),
        ("    2.1. Mục đích của việc làm bài tập lớn", "2"),
        ("    2.2. Mục đích cụ thể của bài tập đang làm", "2"),
        ("3. Cấu trúc chương trình (12 Module theo từng Tab chức năng)", "3"),
        ("4. Bảng phân công nhiệm vụ các thành viên", "5"),
        ("PHẦN II: NỘI DUNG VÀ THỰC HIỆN DỰ ÁN", "6"),
        ("1. Cơ sở lý thuyết và các Mẫu thiết kế phần mềm", "6"),
        ("    1.1. Kiến trúc phân tầng MVC", "6"),
        ("    1.2. Các mẫu thiết kế (Design Patterns) áp dụng", "7"),
        ("    1.3. Cơ sở dữ liệu quan hệ và bảo toàn tính toàn vẹn (ACID)", "9"),
        ("2. Các công nghệ và kỹ thuật sử dụng", "10"),
        ("3. Nội dung và các bước thực hiện dự án", "12"),
        ("    3.1. Phân tích chức năng và Biểu đồ Use Case", "12"),
        ("    3.2. Thiết kế Cơ sở dữ liệu quan hệ (15 bảng chuẩn 3NF)", "15"),
        ("    3.3. Biểu đồ tuần tự (Sequence Diagram) & Luồng nghiệp vụ Bếp - POS", "19"),
        ("    3.4. Hiện thực chi tiết 12 Module chức năng (Theo từng Tab giao diện)", "21"),
        ("    3.5. Kiểm thử phần mềm (Black-box Test Cases)", "28"),
        ("4. Kết quả đạt được", "32"),
        ("5. Những hạn chế của dự án và hướng phát triển", "33"),
        ("    5.1. Những hạn chế của dự án", "33"),
        ("    5.2. Hướng phát triển trong tương lai", "33"),
        ("PHẦN III: TỔNG KẾT, KẾT LUẬN & TÀI LIỆU THAM KHẢO", "35"),
        ("1. Kết luận", "35"),
        ("2. Lời cảm ơn", "35"),
        ("3. Tài liệu tham khảo", "36")
    ]

    toc_table = doc.add_table(rows=len(toc_items), cols=2)
    toc_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(toc_table, color="FFFFFF") # Không kẻ viền bảng mục lục

    for idx, (title, page) in enumerate(toc_items):
        cell_t = toc_table.cell(idx, 0)
        cell_p = toc_table.cell(idx, 1)
        cell_t.width = Inches(5.8)
        cell_p.width = Inches(0.7)
        
        is_part = title.startswith("PHẦN")
        p_t = cell_t.paragraphs[0]
        p_t.paragraph_format.space_before = Pt(3 if is_part else 1)
        p_t.paragraph_format.space_after = Pt(3 if is_part else 1)
        p_t.paragraph_format.line_spacing = 1.2
        r_t = p_t.add_run(title)
        r_t.font.name = "Times New Roman"
        r_t.font.size = Pt(12)
        r_t.font.bold = is_part or title[0].isdigit() and title[1] == "."
        if is_part:
            r_t.font.color.rgb = RGBColor(30, 58, 138)
            
        p_p = cell_p.paragraphs[0]
        p_p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        p_p.paragraph_format.space_before = Pt(3 if is_part else 1)
        p_p.paragraph_format.space_after = Pt(3 if is_part else 1)
        r_p = p_p.add_run(page)
        r_p.font.name = "Times New Roman"
        r_p.font.size = Pt(12)
        r_p.font.bold = is_part

    doc.add_page_break()

    # =========================================================================
    # PHẦN I: GIỚI THIỆU
    # =========================================================================
    add_heading_1(doc, "PHẦN I: GIỚI THIỆU")

    add_heading_2(doc, "1. Lời giới thiệu")
    add_p(doc, 
        "Trong thời đại chuyển đổi số mạnh mẽ hiện nay, ngành dịch vụ ẩm thực (F&B - Food and Beverage) đang phải đối mặt với áp lực cạnh tranh khốc liệt cùng yêu cầu ngày càng cao về tốc độ phục vụ, tính chính xác và hiệu quả quản trị chi phí. Các phương thức vận hành truyền thống sử dụng ghi chép sổ sách, order bằng giấy ghi chú (order slip) hoặc các phần mềm đơn lẻ thường bộc lộ nhiều điểm nghẽn nghiêm trọng:"
    )
    add_bullet(doc, "Dễ thất lạc order, ghi nhận nhầm món giữa nhân viên phục vụ tại bàn và nhà bếp trong các khung giờ cao điểm.", "Thất thoát thông tin order: ")
    add_bullet(doc, "Thời gian chuyển giao thông tin chậm trễ, khiến khách hàng phải chờ đợi lâu, làm giảm trải nghiệm dịch vụ.", "Tốc độ phục vụ chậm: ")
    add_bullet(doc, "Khó kiểm soát định lượng tồn kho nguyên vật liệu, dẫn đến tình trạng thất thoát hoặc hư hỏng thực phẩm mà không rõ nguyên nhân.", "Hao hụt kho không kiểm soát: ")
    add_bullet(doc, "Báo cáo doanh thu cuối ngày tốn nhiều công sức đối soát tiền mặt, tiền chuyển khoản, voucher và chênh lệch ca làm việc.", "Khó khăn trong đối soát tài chính: ")

    add_p(doc, 
        "Xuất phát từ thực tế đó, nhóm sinh viên chúng em đã nghiên cứu và phát triển đề tài \"Hệ Thống Quản Lý Nhà Hàng Toàn Diện (Restaurant Management System & POS)\". Ứng dụng được xây dựng trên nền tảng Java Swing hiện đại (kết hợp bộ giao diện FlatLaf Look & Feel) kết hợp cơ sở dữ liệu quan hệ mạnh mẽ Microsoft SQL Server, áp dụng các mẫu thiết kế hướng đối tượng chuẩn mực (MVC, Singleton, Observer, Strategy, Factory) nhằm cung cấp một giải pháp quản trị khép kín, từ khâu đón tiếp khách, gọi món tại bàn, truyền đơn tự động xuống màn hình bếp KDS, quản lý kho nguyên liệu theo quy trình kiểm kê thực tế, lập sổ quỹ thu - chi, phân ca chấm công nhân viên cho đến thanh toán xuất hóa đơn nhiệt và báo cáo doanh thu đa chiều."
    )

    add_heading_2(doc, "2. Mục đích đề tài")
    add_heading_3(doc, "2.1. Mục đích của việc làm bài tập lớn")
    add_bullet(doc, "Áp dụng toàn diện các kiến thức nền tảng của môn học Lập trình Java Nâng cao, bao gồm: Lập trình hướng đối tượng (OOP), Quản lý biệt lệ (Exception Handling), Đa luồng (Multithreading), Collections Framework, JDBC, Luồng dữ liệu (I/O Streams) và xây dựng giao diện người dùng đồ họa (GUI).", "Củng cố và vận dụng kiến thức chuyên sâu: ")
    add_bullet(doc, "Nắm vững quy trình phát triển phần mềm chuẩn mực từ khâu khảo sát nghiệp vụ thực tế, mô hình hóa hệ thống (Use Case, ERD, Sequence, Activity Diagram), phân tích thiết kế kiến trúc phân tầng (MVC Layer Architecture), viết mã nguồn sạch (Clean Code), kiểm thử đơn vị đến đóng gói và triển khai ứng dụng.", "Rèn luyện kỹ năng kỹ thuật phần mềm: ")
    add_bullet(doc, "Thực hành phân chia module chức năng, quản lý mã nguồn bằng hệ thống kiểm soát phiên bản (Git/GitHub), phối hợp giải quyết xung đột và tích hợp hệ thống thành một khối thống nhất.", "Nâng cao kỹ năng làm việc nhóm: ")

    add_heading_3(doc, "2.2. Mục đích cụ thể của bài tập đang làm")
    add_p(doc, "Xây dựng một hệ thống phần mềm quản lý nhà hàng đạt tiêu chuẩn công nghiệp với các mục tiêu cụ thể:")
    add_bullet(doc, "Hỗ trợ đầy đủ các hình thức phục vụ Tại bàn (Dine-in) và Mang về (Takeaway). Giao diện trực quan theo sơ đồ bàn ăn động, hỗ trợ chuyển bàn, gộp bàn tức thì; tìm kiếm món ăn nhanh qua danh mục và thẻ ảnh minh họa.", "1. Tối ưu hóa quy trình bán hàng tại quầy (POS): ")
    add_bullet(doc, "Loại bỏ hoàn toàn giấy in order tại bếp bằng màn hình hiển thị trực quan. Đầu bếp tiếp nhận, chuyển trạng thái nấu và báo hoàn tất theo thời gian thực (Real-time Observer Pattern).", "2. Số hóa truyền thông Bếp - Thu ngân (KDS): ")
    add_bullet(doc, "Tuyệt đối ngăn chặn các rủi ro gian lận hoặc sai sót vận hành: Chặn thanh toán khi món chưa được gửi sang bếp; cảnh báo khi bếp chưa nấu xong; khóa thao tác xóa/giảm số lượng với các món đang nấu hoặc đã nấu xong.", "3. Thắt chặt kiểm soát luồng nghiệp vụ thanh toán: ")
    add_bullet(doc, "Quản lý danh mục nguyên vật liệu, theo dõi lịch sử nhập/xuất kho; hỗ trợ người kiểm kê thực hiện đối soát tồn kho thực tế song song với dữ liệu hao hụt định lượng lý thuyết của hệ thống.", "4. Quản lý kho nguyên vật liệu và kiểm kê khoa học: ")
    add_bullet(doc, "Tích hợp Sổ quỹ Thu - Chi nội bộ độc lập; quản lý đóng/mở ca thu ngân có đối soát tiền mặt thực tế; chấm công vào/ra và xếp lịch làm việc cho nhân viên theo từng ca; tích điểm khách hàng thân thiết và áp dụng mã Voucher giảm giá tự động.", "5. Minh bạch tài chính & Vận hành nhân sự: ")

    add_heading_2(doc, "3. Cấu trúc chương trình (12 Module theo từng Tab chức năng)")
    add_p(doc, 
        "Hệ thống được thiết kế theo cấu trúc bảng điều khiển hiện đại (Modern Dashboard), trong đó mỗi Module chức năng được tổ chức độc lập tương ứng với từng Tab trên thanh Menu điều hướng (Sidebar), hỗ trợ phân quyền hiển thị theo Role người dùng (Manager, Cashier, Kitchen):"
    )

    modules_summary = [
        ("Màn hình Đăng nhập (LoginForm)", "Xác thực tài khoản với mật khẩu mã hóa SHA-256; nhận diện và phân chia 3 vai trò làm việc: MANAGER (Quản lý), CASHIER (Thu ngân), KITCHEN (Bếp trưởng)."),
        ("Tab 1: Tổng quan kinh doanh (Dashboard)", "Theo dõi các chỉ số KPI thời gian thực: Doanh thu trong ngày, số đơn hàng hoàn tất, số khách đang dùng bữa, cảnh báo kho, biểu đồ doanh thu theo khung giờ và danh sách đơn hàng gần nhất."),
        ("Tab 2: Thu ngân - Điểm bán hàng (POS)", "Điểm bán hàng tại bàn (Dine-in) và mang về (Takeaway); gọi món nhanh qua lưới thẻ ảnh; theo dõi tiến độ món ăn trên hóa đơn theo 4 màu; cơ chế an toàn tuyệt đối chặn thanh toán khi bếp chưa nấu xong; tự động in hóa đơn nhiệt ESC/POS và xuất file PDF chuẩn 80mm."),
        ("Tab 3: Sơ đồ bàn & Đặt bàn (Tables & Reservations)", "Trực quan hóa hiện trạng các bàn ăn theo màu sắc (Trống, Đang có khách, Đã đặt trước); hỗ trợ nghiệp vụ Chuyển bàn, Gộp bàn và tiếp nhận đặt hẹn trước đa kênh (Hotline, Zalo, MXH, Trực tiếp, Website)."),
        ("Tab 4: Màn hình Bếp & Bar KDS (Kitchen Display System)", "Điều phối thứ tự chế biến món theo thời gian thực (Observer Pattern) với quy trình 3 bước nghiêm ngặt: 1. Nhận món -> 2. Nấu món -> 3. Nấu xong; hỗ trợ nhận nhanh tất cả món chờ và từ chối món hết nguyên liệu."),
        ("Tab 5: Quản lý Thực đơn & Món ăn (Menu Management)", "Thiết lập danh mục, món ăn, giá bán và trạng thái kinh doanh; tích hợp trình quản lý ảnh DishImageManager hỗ trợ xem trước ảnh (preview) và tự động đồng bộ cache."),
        ("Tab 6: Khách hàng & Hội viên (Customer CRM)", "Quản lý hồ sơ khách hàng, phân hạng thẻ thành viên (Standard, Silver, Gold, Diamond) và tích lũy điểm thưởng tự động khi thanh toán."),
        ("Tab 7: Khuyến mãi & Voucher (Promotions & Vouchers)", "Thiết lập chương trình ưu đãi, mã giảm giá theo % hoặc tiền mặt cố định, quy định giá trị đơn tối thiểu và giới hạn số lượt áp dụng."),
        ("Tab 8: Quản lý Kho & Hàng hóa (Inventory Management)", "Quản lý danh mục nguyên vật liệu; tab hợp nhất \"Nhập - Xuất & Lịch Sử\"; hỗ trợ kiểm kê kho thực tế (Stocktake) phân biệt rõ giữa định lượng lý thuyết của máy và số liệu thực tế do con người kiểm đếm; quản lý thông tin nhà cung cấp."),
        ("Tab 9: Sổ quỹ Thu - Chi (Cashbook)", "Quản lý dòng tiền vào/ra ngoài doanh thu bán hàng (chi nhập hàng, chi lương, chi tiền điện/nước, thu nợ, thu tiền cọc); thống kê tồn quỹ thực tế (Dành riêng cho Quản lý)."),
        ("Tab 10: Ca làm việc & Chấm công (Shift & Attendance)", "Quản lý mở/đóng ca có đối soát tiền mặt thực tế và tính chênh lệch két tiền; lập lịch phân ca làm việc (Shift Scheduling) với bộ chọn ngày linh hoạt; chấm công Check-in / Check-out cho nhân viên."),
        ("Tab 11: Báo cáo doanh thu & Phân tích (Reports & Analytics)", "Thống kê và trực quan hóa doanh thu theo khoảng thời gian, phân tích cơ cấu doanh thu theo phương thức thanh toán và top món ăn bán chạy nhất (Dành riêng cho Quản lý)."),
        ("Tab 12: Quản lý tài khoản & Phân quyền (User Management)", "Quản lý danh sách nhân sự hệ thống, cấp tài khoản, đổi mật khẩu và phân quyền vai trò (Dành riêng cho Quản lý).")
    ]
    for mod_title, mod_desc in modules_summary:
        add_bullet(doc, mod_desc, f"{mod_title}: ")

    add_heading_2(doc, "4. Bảng phân công nhiệm vụ các thành viên")
    add_p(doc, "Dự án được phân chia công việc rõ ràng theo đúng năng lực và chuyên môn của từng thành viên:")

    team_headers = ["STT", "Họ và tên", "Vai trò đảm nhiệm", "Nhiệm vụ và Công việc cụ thể", "Tỷ lệ"]
    team_widths = [0.5, 1.4, 1.3, 2.8, 0.5]
    team_aligns = [WD_ALIGN_PARAGRAPH.CENTER, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.CENTER]
    team_rows = [
        [
            "1",
            "Phạm Văn Thanh",
            "Nhóm trưởng\n(Lead Architect & POS)",
            "- Thiết kế kiến trúc tổng thể phần mềm theo mô hình MVC và các Design Patterns.\n- Phân tích thiết kế CSDL quan hệ (ERD 3NF) trên MS SQL Server.\n- Lập trình Module Bán hàng (POSPanel), luồng thanh toán an toàn, in hóa đơn nhiệt ESC/POS và PDF iText.\n- Xây dựng kịch bản cài đặt tự động setup.bat và run_app.bat.",
            "100%"
        ],
        [
            "2",
            "Bùi Văn Thắng",
            "Thành viên\n(Backend & Kitchen KDS)",
            "- Lập trình Module Màn hình Bếp KDS (KitchenPanel, KitchenController) và đồng bộ thời gian thực (OrderObserver).\n- Lập trình tầng DAO (OrderDAO, TableDAO, UserDAO).\n- Xây dựng nghiệp vụ Chuyển bàn, Gộp bàn và quản lý trạng thái bàn ăn (TableController).\n- Viết tài liệu đặc tả và kiểm thử tích hợp luồng gọi món.",
            "100%"
        ],
        [
            "3",
            "Trần Tuấn Thanh",
            "Thành viên\n(Inventory & Cashbook)",
            "- Phát triển Module Quản lý Kho & Nhập - Xuất hàng (InventoryPanel, StockController).\n- Xây dựng nghiệp vụ Kiểm kê kho thực tế (Stocktake) và đối soát hao hụt định lượng.\n- Lập trình Module Sổ quỹ Thu - Chi (CashbookPanel, CashTransactionDAO).\n- Quản lý danh mục Nhà cung cấp và dữ liệu đối tác.",
            "100%"
        ],
        [
            "4",
            "Bùi Đức Thịnh",
            "Thành viên\n(HRM, Dashboard & Menu)",
            "- Xây dựng Module Quản lý Ca làm việc & Chấm công (ShiftPanel, ShiftController).\n- Thiết kế bộ chọn ngày xếp ca linh hoạt và tính toán chênh lệch két tiền.\n- Phát triển Dashboard tổng quan, kết xuất biểu đồ doanh thu (JFreeChart).\n- Thu thập dữ liệu món ăn, chuẩn hóa hình ảnh (DishImageManager) và kiểm thử tổng thể.",
            "100%"
        ]
    ]

    t_team = doc.add_table(rows=1, cols=len(team_headers))
    t_team.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(t_team)
    format_table_header(t_team, team_widths, team_headers, "1E3A8A")
    format_data_rows(t_team, team_widths, team_rows, team_aligns)

    add_p(doc, "").paragraph_format.space_after = Pt(8)

    # Ngắt trang sang Phần II
    doc.add_page_break()

    # =========================================================================
    # PHẦN II: NỘI DUNG VÀ THỰC HIỆN DỰ ÁN
    # =========================================================================
    add_heading_1(doc, "PHẦN II: NỘI DUNG VÀ HIỆN THỰC DỰ ÁN")

    add_heading_2(doc, "1. Cơ sở lý thuyết và các Mẫu thiết kế phần mềm")
    add_heading_3(doc, "1.1. Kiến trúc phân tầng MVC (Model - View - Controller)")
    add_p(doc, 
        "Hệ thống tuân thủ chặt chẽ nguyên lý phân tách mối quan tâm (Separation of Concerns) của kiến trúc MVC tiêu chuẩn:"
    )
    add_bullet(doc, "Đại diện cho các thực thể dữ liệu nghiệp vụ của nhà hàng: User, Role, DiningTable, MenuItem, Category, Order, OrderDetail, InventoryItem, StockMovement, Supplier, CashTransaction, Shift, EmployeeAttendance, Customer, Voucher. Các lớp Model thuần túy chứa thuộc tính, constructor, getter/setter và các phương thức tính toán nội tại (như tính subtotal, chiết khấu voucher).", "Model (com.pos.model): ")
    add_bullet(doc, "Chịu trách nhiệm trực quan hóa dữ liệu và tiếp nhận tương tác từ người dùng. Tất cả các view được kế thừa từ JPanel hoặc JFrame của Java Swing, sử dụng Look & Feel FlatLaf để tạo giao diện phẳng hiện đại, thân thiện và đáp ứng nhanh.", "View (com.pos.view): ")
    add_bullet(doc, "Đóng vai trò cầu nối điều phối. Controller tiếp nhận sự kiện từ View, thực hiện các quy tắc kiểm tra logic nghiệp vụ (business validation), phối hợp với tầng DAO để truy xuất/cập nhật dữ liệu CSDL và gửi thông báo cập nhật lại View.", "Controller (com.pos.controller): ")
    add_bullet(doc, "Tách biệt hoàn toàn các câu lệnh SQL khỏi logic nghiệp vụ. Tầng DAO chịu trách nhiệm thực thi các truy vấn CRUD an toàn qua PreparedStatement, ánh xạ ResultSet thành các đối tượng Model tương ứng.", "Tầng truy xuất CSDL (com.pos.dao): ")

    add_figure_placeholder(doc, "2.1", "Sơ đồ Kiến trúc phân tầng MVC kết hợp DAO & Database trong hệ thống POS")

    add_heading_3(doc, "1.2. Các mẫu thiết kế (Design Patterns) áp dụng")
    
    add_callout(doc, "1. Singleton Pattern - Quản lý kết nối CSDL duy nhất",
        "Áp dụng tại lớp DatabaseConnection (com.pos.utils):\n"
        "• Mục đích: Đảm bảo toàn bộ ứng dụng chỉ tồn tại duy nhất một đối tượng quản lý kết nối CSDL, tránh việc mở quá nhiều kết nối dư thừa gây cạn kiệt tài nguyên hệ thống.\n"
        "• Cài đặt: Khởi tạo biến private static DatabaseConnection instance, hàm khởi tạo private đọc cấu hình từ file db.properties và cung cấp phương thức truy cập toàn cục public static synchronized DatabaseConnection getInstance()."
    )

    add_callout(doc, "2. Observer Pattern - Đồng bộ hóa thời gian thực giữa Bếp KDS và Thu ngân POS",
        "Áp dụng trong cơ chế đồng bộ trạng thái món ăn giữa quầy Thu ngân và Nhà bếp:\n"
        "• Giao diện Observer: OrderObserver định nghĩa phương thức onOrderChanged(OrderEvent event).\n"
        "• Đối tượng Subject: OrderController duy trì danh sách các Observer đăng ký (List<OrderObserver>).\n"
        "• Luồng hoạt động: Khi thu ngân bấm \"Gửi Bếp\" hoặc sửa đổi món, OrderController kích hoạt sự kiện notifyObservers(). Màn hình KitchenPanel tự động lắng nghe và nạp lại danh sách món chờ nấu ngay lập tức mà không cần nhân viên bếp phải bấm phím F5 thủ công. Ngược lại, khi đầu bếp bấm \"Nấu xong\", quầy thu ngân lập tức nhận được thông báo để đổi màu trạng thái trên hóa đơn sang xanh lá."
    )

    add_callout(doc, "3. Strategy Pattern - Linh hoạt trong cơ chế giảm giá đơn hàng",
        "Áp dụng cho cơ chế tính toán chiết khấu trên hóa đơn thanh toán:\n"
        "• Giao diện: DiscountStrategy định nghĩa hàm BigDecimal apply(BigDecimal originalTotal).\n"
        "• Các chiến lược cụ thể:\n"
        "   + NoDiscount: Không áp dụng giảm giá (trả về 0 đ).\n"
        "   + VoucherDiscountStrategy: Giảm giá dựa trên mã Voucher (theo % hoặc trừ tiền cố định, có xét điều kiện đơn tối thiểu).\n"
        "   + LoyaltyPointsDiscount: Trừ trực tiếp tiền bằng điểm thưởng tích lũy của khách hàng thân thiết.\n"
        "• Lợi ích: Cho phép nhà hàng dễ dàng bổ sung các chương trình khuyến mãi mới (ví dụ: Giảm giá sinh nhật, Flash sale giờ vàng) trong tương lai mà không phải can thiệp vào mã nguồn thanh toán cốt lõi (tuân thủ nguyên lý Open/Closed Principle)."
    )

    add_callout(doc, "4. Factory Pattern - Khởi tạo phương thức thanh toán",
        "Áp dụng trong việc phân bổ luồng thanh toán tại quầy POS:\n"
        "• Lớp PaymentFactory tiếp nhận tham số phương thức (CASH, TRANSFER) và sinh ra đối tượng xử lý tương ứng (CashPayment, TransferPayment), đảm bảo phân tách rõ ràng giữa tiền mặt két thu ngân và sao kê ngân hàng chuyển khoản."
    )

    add_heading_3(doc, "1.3. Cơ sở dữ liệu quan hệ và bảo toàn tính toàn vẹn (ACID)")
    add_p(doc, 
        "Cơ sở dữ liệu của hệ thống được chuẩn hóa đạt dạng chuẩn 3 (3NF), ngăn chặn hiện tượng dư thừa dữ liệu và dị thường khi cập nhật. Để đảm bảo an toàn tuyệt đối cho số liệu kinh doanh:"
    )
    add_bullet(doc, "Toàn bộ các thao tác thanh toán, trừ kho, chuyển bàn, gộp bàn và kết ca đều được thực thi trong một Database Transaction (conn.setAutoCommit(false), conn.commit(), conn.rollback()). Nếu xảy ra bất kỳ sự cố gián đoạn nào giữa chừng, toàn bộ trạng thái sẽ tự động hoàn nguyên về thời điểm trước giao dịch.", "Tính nguyên tử và nhất quán (ACID Transaction): ")
    add_bullet(doc, "Tại bảng dining_tables, hệ thống áp dụng cột version quản trị đồng thời (Optimistic Concurrency Control). Khi hai thu ngân cùng thao tác đổi trạng thái trên một bàn tại cùng thời điểm, hệ thống sẽ phát hiện phiên bản dữ liệu bị xung đột và đưa ra cảnh báo ngăn chặn ghi đè sai sót.", "Kiểm soát tranh chấp đồng thời (Concurrency Control): ")

    add_heading_2(doc, "2. Các kỹ thuật và công nghệ sử dụng")
    add_p(doc, "Hệ thống được phát triển với ngăn xếp công nghệ tiêu chuẩn, mạnh mẽ và ổn định:")

    tech_headers = ["Nhóm công nghệ", "Công nghệ / Thư viện", "Phiên bản", "Vai trò và Tính năng ứng dụng trong dự án"]
    tech_widths = [1.5, 1.6, 0.8, 2.6]
    tech_aligns = [WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.CENTER, WD_ALIGN_PARAGRAPH.LEFT]
    tech_rows = [
        ["Ngôn ngữ cốt lõi", "Java SE Development Kit (JDK)", "17 LTS", "Sử dụng các tính năng hiện đại: Record classes, Switch expressions mở rộng, Text blocks, Stream API, Concurrent collections."],
        ["Giao diện người dùng", "Java Swing + FlatLaf Look & Feel", "3.4.x", "Khắc phục nhược điểm giao diện thô cứng của Swing truyền thống. Mang lại giao diện phẳng hiện đại, bo góc mềm mại, độ tương phản chuẩn doanh nghiệp."],
        ["Hệ quản trị CSDL", "Microsoft SQL Server", "2019/2022", "Lưu trữ dữ liệu quan hệ mạnh mẽ, hỗ trợ Transaction ACID, thủ tục lưu trữ, ràng buộc toàn vẹn khóa ngoại và chỉ mục tối ưu tìm kiếm."],
        ["Kết nối CSDL", "Microsoft JDBC Driver + HikariCP", "12.x", "Tối ưu hóa kết nối cơ sở dữ liệu qua cơ chế Connection Pooling, tái sử dụng kết nối giúp hệ thống phản hồi cực nhanh."],
        ["Xuất hóa đơn PDF", "iText PDF Library", "5.5.x", "Tự động kết xuất hóa đơn thanh toán thành file PDF khổ nhiệt 80mm (K80) chuẩn siêu thị/nhà hàng, lưu trữ tại thư mục invoices/."],
        ["Mô phỏng in nhiệt", "ESC/POS Printing Simulator", "Tự phát triển", "Gửi trực tiếp tập lệnh điều khiển máy in nhiệt ESC/POS (cắt giấy tự động, mở ngăn kéo đựng tiền) qua cổng COM/USB."],
        ["Biểu đồ phân tích", "JFreeChart", "1.5.x", "Vẽ biểu đồ cột và đường biểu diễn doanh thu theo từng khung giờ trong ngày trên Dashboard, trực quan hóa giờ cao điểm."],
        ["Nhật ký kiểm toán", "Apache Log4j2", "2.20.x", "Ghi log có hệ thống toàn bộ hoạt động đăng nhập, giao dịch thanh toán và biệt lệ hệ thống ra file logs/restaurant-pos.log."],
        ["Tự động hóa DevOps", "Windows Batch (.bat)", "Mã hóa UTF-8", "Kịch bản setup.bat (tự động phát hiện SQL Server và cài đặt CSDL) và run_app.bat (khởi chạy 1-click không cần IDE)."]
    ]

    t_tech = doc.add_table(rows=1, cols=len(tech_headers))
    t_tech.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(t_tech)
    format_table_header(t_tech, tech_widths, tech_headers, "0F766E")
    format_data_rows(t_tech, tech_widths, tech_rows, tech_aligns)

    add_p(doc, "").paragraph_format.space_after = Pt(8)

    add_heading_2(doc, "3. Nội dung và các bước thực hiện dự án")
    add_p(doc, "Dự án được triển khai bài bản qua 5 giai đoạn nối tiếp theo quy trình phát triển phần mềm chuẩn mực:")

    add_heading_3(doc, "3.1. Phân tích chức năng và Biểu đồ Use Case")
    add_p(doc, 
        "Dựa trên khảo sát thực tế tại các mô hình nhà hàng ẩm thực, hệ thống phân định rõ ràng 3 nhóm tác nhân (Actors) chính với ma trận phân quyền RBAC (Role-Based Access Control):"
    )
    add_bullet(doc, "Toàn quyền quản trị hệ thống: Xem báo cáo doanh thu tài chính, quản lý sổ quỹ thu chi, quản lý kho & kiểm kê hàng hóa, thiết lập thực đơn món ăn, phân ca chấm công nhân sự, quản lý tài khoản người dùng và thiết lập mã giảm giá Voucher.", "1. Quản lý (Manager): ")
    add_bullet(doc, "Phục vụ bán hàng tại quầy: Mở ca bán hàng, nhận order món tại bàn hoặc mang về, chuyển bàn, gộp bàn, gửi order sang bếp, tiếp nhận đặt bàn trước, thu tiền xuất hóa đơn, tích điểm khách hàng và đóng ca bàn giao tiền mặt két.", "2. Thu ngân (Cashier): ")
    add_bullet(doc, "Vận hành chế biến món ăn: Theo dõi màn hình Bếp KDS thời gian thực, tiếp nhận món, chuyển trạng thái nấu, báo hoàn tất chế biến và từ chối món khi gặp sự cố nguyên liệu.", "3. Bếp trưởng (Kitchen): ")

    add_figure_placeholder(doc, "2.2", "Sơ đồ Use Case tổng quát phân quyền theo 3 Actor (Manager, Cashier, Kitchen)")

    add_heading_3(doc, "3.2. Thiết kế Cơ sở dữ liệu quan hệ (15 bảng chuẩn hóa 3NF)")
    add_p(doc, "Hệ thống sở hữu cơ sở dữ liệu quan hệ gồm 15 bảng được thiết kế chuẩn mực, loại bỏ trùng lặp và liên kết chặt chẽ:")

    tables_desc = [
        ("1. users", "id (PK), username, password_hash, full_name, role (CHECK: MANAGER, CASHIER, KITCHEN), is_active", "Quản lý danh sách nhân sự, tài khoản và phân quyền vai trò làm việc."),
        ("2. categories", "id (PK), name, type (CHECK: FOOD, DRINK, OTHER)", "Quản lý các nhóm danh mục thực đơn (Khai vị, Món chính, Lẩu & Nướng, Đồ uống...)."),
        ("3. menu_items", "id (PK), category_id (FK), name, price, unit, image_url, is_available", "Danh mục chi tiết các món ăn, đồ uống kèm giá bán và trạng thái phục vụ."),
        ("4. dining_tables", "id (PK), name, capacity, status (CHECK: AVAILABLE, OCCUPIED, RESERVED), qr_code_token, version", "Quản lý danh sách bàn ăn, sức chứa ghế ngồi, trạng thái phục vụ và mã định danh bàn."),
        ("5. reservations", "id (PK), table_id (FK), customer_id (FK), customer_name, customer_phone, party_size, reservation_time, deposit_amount, status (CHECK: PENDING, CONFIRMED, SEATED, CANCELLED), channel, note", "Quản lý lịch đặt bàn trước đa kênh (Hotline, Zalo, MXH, Trực tiếp, Website), tiền cọc và thời gian khách đến."),
        ("6. customers", "id (PK), full_name, phone (UNIQUE), address, loyalty_points, total_spent, tier (CHECK: STANDARD, SILVER, GOLD, DIAMOND)", "Hồ sơ khách hàng thân thiết, tích điểm thưởng và tự động nâng hạng thẻ thành viên."),
        ("7. vouchers", "id (PK), code (UNIQUE), discount_type (CHECK: PERCENT, FIXED), discount_value, min_order_amount, max_discount, start_date, end_date, usage_limit, used_count, is_active", "Quản lý các chương trình khuyến mãi, mã ưu đãi giảm giá theo % hoặc tiền cố định."),
        ("8. shifts", "id (PK), user_id (FK), start_time, end_time, initial_cash, cash_sales, transfer_sales, actual_cash, difference, status (CHECK: OPEN, CLOSED), note", "Quản lý ca làm việc của thu ngân, kiểm soát tiền mặt đầu ca, doanh thu trong ca và đối soát két tiền khi đóng ca."),
        ("9. orders", "id (PK), shift_id (FK), user_id (FK), table_id (FK), customer_id (FK), order_type (CHECK: DINE_IN, TAKEAWAY), status (CHECK: PENDING, SERVING, PAID, CANCELLED), payment_method, subtotal, discount_amount, total_amount, created_at, completed_at", "Lưu trữ thông tin hóa đơn đơn hàng, liên kết bàn ăn, ca làm việc và tổng tiền."),
        ("10. order_details", "id (PK), order_id (FK), menu_item_id (FK), quantity, unit_price, sent_to_kitchen (BIT), kitchen_status (CHECK: PENDING, ACCEPTED, COOKING, DONE, REJECTED), note", "Chi tiết từng món ăn trong đơn, theo dõi cờ gửi bếp và trạng thái nấu nướng 3 bước của bếp."),
        ("11. ingredients", "id (PK), name, unit, stock_quantity, min_quantity, cost_price", "Quản lý danh mục nguyên vật liệu kho, số lượng tồn kho thực tế và ngưỡng cảnh báo sắp hết."),
        ("12. suppliers", "id (PK), name, contact_phone, address, note", "Thông tin đối tác cung cấp thực phẩm và nguyên vật liệu cho nhà hàng."),
        ("13. stock_movements", "id (PK), ingredient_id (FK), supplier_id (FK), user_id (FK), movement_type (CHECK: IN, OUT, ADJUST), quantity, unit_price, total_cost, reason, created_at", "Lịch sử chi tiết các lần Nhập kho, Xuất kho bếp, Xuất hủy và Điều chỉnh kiểm kê."),
        ("14. cash_transactions", "id (PK), user_id (FK), type (CHECK: IN, OUT), category, amount, description, created_at", "Sổ quỹ quản lý các khoản Thu - Chi tiền mặt nội bộ ngoài bán hàng (nhập hàng, lương, điện nước, đặt cọc)."),
        ("15. employee_attendance", "id (PK), user_id (FK), shift_id (FK), check_in, check_out, status, note", "Bảng theo dõi chấm công vào/ra và xếp lịch làm việc cho từng nhân viên.")
    ]

    t_db_headers = ["STT", "Tên bảng", "Cấu trúc trường chính (PK, FK, CHECK)", "Mô tả nghiệp vụ"]
    t_db_widths = [0.4, 1.4, 2.7, 2.0]
    t_db_aligns = [WD_ALIGN_PARAGRAPH.CENTER, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.LEFT]
    t_db_rows = []
    for idx, (tb_name, cols, desc) in enumerate(tables_desc):
        t_db_rows.append([str(idx + 1), tb_name, cols, desc])

    t_db = doc.add_table(rows=1, cols=len(t_db_headers))
    t_db.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(t_db)
    format_table_header(t_db, t_db_widths, t_db_headers, "1E3A8A")
    format_data_rows(t_db, t_db_widths, t_db_rows, t_db_aligns)

    add_p(doc, "").paragraph_format.space_after = Pt(8)
    add_figure_placeholder(doc, "2.3", "Sơ đồ Quan hệ Thực thể CSDL (ERD - Entity Relationship Diagram) chuẩn 3NF")

    add_heading_3(doc, "3.3. Biểu đồ tuần tự (Sequence Diagram) & Luồng kiểm soát nghiệp vụ Bếp - POS")
    add_p(doc, 
        "Một trong những điểm sáng tạo nổi bật nhất của dự án là việc thiết lập luồng nghiệp vụ kiểm soát 2 chiều chặt chẽ giữa Quầy Thu ngân và Màn hình Bếp trưởng, giải quyết dứt điểm các sai sót thường gặp trong thực tế:"
    )

    add_callout(doc, "Luồng kiểm soát Thanh toán An toàn Tuyệt đối (POS Checkout Policy)",
        "1. Chặn thanh toán khi chưa gửi bếp: Khi thu ngân bấm THANH TOÁN (F9), hệ thống duyệt qua danh sách chi tiết đơn. Nếu còn bất kỳ món nào có cờ sent_to_kitchen = 0, lệnh thanh toán bị dừng ngay lập tức và hiển thị cảnh báo đỏ yêu cầu gửi bếp trước.\n"
        "2. Bắt buộc Bếp phải báo NẤU XONG mới được thanh toán: Hệ thống kiểm tra trạng thái nấu của tất cả các món. Nếu tồn tại món chưa ở trạng thái DONE hoặc REJECTED (tức là còn PENDING, ACCEPTED, hoặc COOKING), hệ thống KHÓA HOÀN TOÀN nút thanh toán và yêu cầu thu ngân chờ bếp hoàn tất. Điều này đảm bảo khách hàng luôn được phục vụ đủ món trước khi xuất tiền.\n"
        "3. Khóa giảm/xóa món đang nấu: Thu ngân không thể bấm nút xóa hoặc giảm số lượng của món ăn đã chuyển sang trạng thái Đang nấu hoặc Nấu xong, ngăn chặn thất thoát nguyên liệu của bếp."
    )

    add_callout(doc, "Quy trình Chế biến 3 Bước Nghiêm ngặt tại Bếp (Enforced Kitchen Workflow)",
        "Đầu bếp trên màn hình KitchenPanel phải thao tác đúng trình tự nghiệp vụ 3 bước:\n"
        "• Bước 1: \"1. Nhận món\" -> Chuyển từ PENDING (Chờ nhận) sang ACCEPTED (Đã nhận).\n"
        "• Bước 2: \"2. Nấu món\" -> Chuyển từ ACCEPTED sang COOKING (Đang nấu chảo/bếp). Nếu món chưa được nhận mà đầu bếp bấm nấu, hệ thống sẽ bật cảnh báo ngăn chặn.\n"
        "• Bước 3: \"3. Nấu xong\" -> Chuyển từ COOKING sang DONE (Hoàn tất). Trạng thái lập tức bắn tín hiệu Observer sang hóa đơn quầy POS chuyển thành màu xanh lá."
    )

    add_figure_placeholder(doc, "2.4", "Biểu đồ Tuần tự (Sequence Diagram) quy trình Gọi món - Chế biến KDS 3 bước - Thanh toán POS")

    add_heading_3(doc, "3.4. Hiện thực chi tiết 12 Module chức năng (Theo từng Tab trong phần mềm)")
    add_p(doc, "Chi tiết 12 phân hệ nghiệp vụ hoàn chỉnh được tổ chức theo từng Tab giao diện:")

    tabs_detail = [
        ("Tab 1: Tổng quan kinh doanh (DashboardPanel)",
         "Cung cấp bức tranh toàn cảnh về hoạt động nhà hàng trong ngày. Bảng KPI hiển thị Doanh thu hôm nay, Số đơn hoàn thành, Số khách đang dùng bữa và Cảnh báo mặt hàng sắp hết kho. Biểu đồ JFreeChart biểu diễn trực quan doanh thu theo các khung giờ (sáng, trưa, tối) giúp quản lý phân bổ nhân sự hợp lý vào giờ cao điểm. Bên dưới là bảng các giao dịch đơn hàng phát sinh gần nhất.",
         "2.5", "Bảng điều khiển kinh doanh tổng quan DashboardPanel"),
        
        ("Tab 2: Điểm bán hàng & Thu ngân (POSPanel)",
         "Trung tâm điều hành bán hàng tại quầy. Lưới thẻ món ăn có hình ảnh sắc nét, lọc nhanh theo danh mục và tìm kiếm theo tên. Sơ đồ bàn tương tác giúp chọn bàn phục vụ nhanh chóng. Bảng hóa đơn tạm thời hiển thị chi tiết tiến độ món ăn qua 4 trạng thái màu sắc riêng biệt (Đỏ: Chưa gửi bếp, Xanh dương: Chờ nấu, Cam: Đang nấu, Xanh lá: Đã nấu xong). Tích hợp cơ chế an toàn chặn thanh toán khi bếp chưa xong, áp dụng mã voucher, tích điểm khách hàng và tự động in phiếu nhiệt ESC/POS kết hợp xuất file PDF khổ 80mm.",
         "2.6", "Giao diện Điểm bán hàng & Thu ngân POSPanel"),

        ("Tab 3: Sơ đồ bàn & Đặt bàn trước (TableReservationPanel)",
         "Gồm 2 màn hình con: Sơ đồ bàn (Floor Plan) trực quan hóa các bàn ăn theo màu sắc (Xanh: Trống, Đỏ: Đang có khách, Vàng cam: Đã đặt trước). Cung cấp các tác vụ Chuyển bàn, Gộp bàn cực kỳ tiện lợi; Tab Đặt bàn (Reservations) tiếp nhận đặt hẹn đa kênh (Hotline, Zalo, MXH, Trực tiếp, Website), lưu thông tin cọc và tự động chuyển trạng thái bàn ăn sang RESERVED.",
         "2.7", "Sơ đồ bàn ăn Floor Plan và Tiếp nhận lịch đặt bàn TableReservationPanel"),

        ("Tab 4: Màn hình Bếp & Bar KDS (KitchenPanel)",
         "Màn hình thông minh dành cho Bếp trưởng và quầy Bar. Header tối màu sang trọng hiển thị đồng hồ thời gian thực và các bộ đếm KPI món cần xử lý. Bảng order hiển thị danh sách món ăn kèm ghi chú khách hàng (vd: Ít cay, không hành). Bắt buộc tuân thủ quy trình 3 bước: 1. Nhận món -> 2. Nấu món -> 3. Nấu xong, tích hợp nút Nhận tất cả món chờ và Từ chối món hết nguyên liệu.",
         "2.8", "Màn hình điều phối chế biến Bếp trưởng KDS KitchenPanel"),

        ("Tab 5: Quản lý Thực đơn & Món ăn (MenuManagementPanel)",
         "Quản lý danh mục món ăn (Khai vị, Món chính, Lẩu & Nướng, Tráng miệng, Đồ uống...). Thiết lập chi tiết từng món ăn: Tên, đơn giá, đơn vị tính, mô tả và trạng thái kinh doanh. Tích hợp trình quản lý hình ảnh DishImageManager có khung xem trước ảnh (preview) và tự động đồng bộ bộ nhớ đệm cache.",
         "2.9", "Quản lý Danh mục và Thực đơn món ăn MenuManagementPanel"),

        ("Tab 6: Khách hàng & Hội viên CRM (CustomerPanel)",
         "Hồ sơ lưu trữ thông tin khách hàng: Họ tên, số điện thoại, địa chỉ, tổng chi tiêu tích lũy. Tự động xếp hạng thẻ thành viên theo 4 cấp bậc: Standard, Silver, Gold, Diamond. Tích lũy 1 điểm thưởng cho mỗi 1.000 đ thanh toán, hỗ trợ trừ tiền trực tiếp trên hóa đơn tại quầy POS.",
         "2.10", "Quản lý Khách hàng thân thiết và Hạng thẻ CRM CustomerPanel"),

        ("Tab 7: Khuyến mãi & Voucher (VoucherPanel)",
         "Thiết lập các chương trình ưu đãi, mã giảm giá cho nhà hàng. Hỗ trợ 2 phương thức: Giảm theo phần trăm (%) hoặc Giảm số tiền cố định (VNĐ). Thiết lập giá trị đơn hàng tối thiểu, mức giảm tối đa, thời hạn hiệu lực và số lượt áp dụng tối đa.",
         "2.11", "Quản lý Chương trình khuyến mãi và Mã giảm giá VoucherPanel"),

        ("Tab 8: Quản lý Kho & Hàng hóa (InventoryPanel)",
         "Quản lý danh mục nguyên vật liệu kho; tab hợp nhất \"Nhập - Xuất & Lịch Sử\" giúp thực hiện phiếu nhập kho từ nhà cung cấp, phiếu xuất kho cho bếp và xem lịch sử biến động. Đặc biệt, quy trình Kiểm kê kho (Stocktake) hỗ trợ đối soát giữa định lượng tiêu hao lý thuyết của máy và số liệu thực tế do con người kiểm đếm, quản lý thông tin nhà cung cấp.",
         "2.12", "Quản lý Kho nguyên vật liệu và Kiểm kê thực tế InventoryPanel"),

        ("Tab 9: Sổ quỹ Thu - Chi tài chính (CashbookPanel)",
         "Quản lý các dòng tiền vào/ra ngoài hoạt động bán hàng: Lập Phiếu Thu (thu cọc tiệc, thu hồi công nợ) và Lập Phiếu Chi (nhập thực phẩm ngoài chợ, chi lương nhân viên, điện, nước, internet). Thống kê tồn quỹ tiền mặt hiện tại. Phân quyền bảo mật chỉ cho phép tài khoản MANAGER truy cập.",
         "2.13", "Sổ quỹ quản lý Thu - Chi tài chính nội bộ CashbookPanel"),

        ("Tab 10: Ca làm việc & Chấm công (ShiftPanel)",
         "Quản lý ca bán hàng thu ngân: Mở ca nhập tiền mặt ban đầu, theo dõi doanh thu tiền mặt / chuyển khoản trong ca, đóng ca đếm két đối soát chênh lệch Thừa/Thiếu. Phân ca làm việc (Shift Scheduling) với bộ chọn ngày linh hoạt và chấm công Check-in / Check-out cho nhân viên.",
         "2.14", "Quản lý Ca làm việc, Phân ca và Chấm công ShiftPanel"),

        ("Tab 11: Báo cáo doanh thu & Thống kê (ReportPanel)",
         "Thống kê tổng hợp doanh thu theo khoảng thời gian tùy chọn (Hôm nay, Tuần này, Tháng này, hoặc khoảng ngày tùy chọn). Phân tích cơ cấu thanh toán Tiền mặt vs Chuyển khoản, danh sách Top món ăn bán chạy nhất (Best-sellers) và xuất báo cáo dữ liệu.",
         "2.15", "Báo cáo phân tích doanh thu và Món bán chạy ReportPanel"),

        ("Tab 12: Quản trị tài khoản & Phân quyền (UserManagementPanel)",
         "Quản lý danh sách tài khoản nhân sự: Thêm nhân viên mới, khóa/mở khóa tài khoản, cấp lại mật khẩu. Phân quyền truy cập hệ thống theo 3 vai trò: Quản lý (MANAGER), Thu ngân (CASHIER), Nhà bếp (KITCHEN). Mật khẩu được mã hóa an toàn bằng thuật toán SHA-256.",
         "2.16", "Quản trị tài khoản nhân viên và Phân quyền RBAC UserManagementPanel")
    ]

    for title, desc, fig_num, fig_cap in tabs_detail:
        add_heading_3(doc, title)
        add_p(doc, desc)
        add_figure_placeholder(doc, fig_num, fig_cap)

    # Đóng gói và DevOps
    add_heading_3(doc, "Bộ kịch bản Tự động hóa Triển khai (DevOps Automation)")
    add_bullet(doc, "Tự động phát hiện phiên bản SQL Server trên máy (localhost, .\\SQLEXPRESS), tự động cấu hình chuỗi kết nối trong db.properties, thực thi file setup_database.sql tạo 15 bảng và nạp toàn bộ dữ liệu mẫu.", "Kịch bản setup.bat: ")
    add_bullet(doc, "Khởi chạy ứng dụng tức thì chỉ với 1 cú nhấp đúp chuột, tự động nhận diện Java 17 Runtime và kích hoạt chế độ UTF-8 hiển thị tiếng Việt chuẩn mực.", "Kịch bản run_app.bat: ")
    add_bullet(doc, "Chứa file sao lưu chuẩn RestaurantManagementSys.bak, script khởi tạo CSDL setup_database.sql và hướng dẫn chi tiết HUONG_DAN_CAI_DAT_DATABASE.md.", "Thư mục database/: ")

    add_heading_3(doc, "3.5. Kiểm thử phần mềm (Black-box Test Cases)")
    add_p(doc, "Hệ thống đã trải qua quá trình kiểm thử chức năng hộp đen nghiêm ngặt với 12 kịch bản then chốt:")

    tc_headers = ["Mã TC", "Chức năng kiểm thử", "Tiền điều kiện", "Các bước thực hiện", "Kết quả mong đợi", "Đánh giá"]
    tc_widths = [0.6, 1.3, 1.2, 1.7, 1.3, 0.4]
    tc_aligns = [WD_ALIGN_PARAGRAPH.CENTER, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.CENTER]
    tc_rows = [
        ["TC-01", "Đăng nhập hệ thống", "Tài khoản active trong DB", "Nhập đúng username/password và chọn quyền", "Đăng nhập thành công, mở đúng giao diện theo Role", "PASS"],
        ["TC-02", "Đăng nhập sai mật khẩu", "Tài khoản active", "Nhập sai mật khẩu 3 lần", "Báo lỗi mật khẩu không chính xác, từ chối đăng nhập", "PASS"],
        ["TC-03", "Gọi món tại bàn (POS)", "Bàn ở trạng thái Trống", "Chọn bàn ăn, bấm chọn món ăn từ lưới thẻ ảnh", "Món hiển thị trên hóa đơn, cờ 'Chưa gửi bếp' (màu đỏ)", "PASS"],
        ["TC-04", "Gửi món sang Bếp", "Đơn có món chưa gửi bếp", "Bấm nút 'Gửi Bếp'", "Đổi trạng thái thành 'Chờ nấu' (xanh dương), Bếp nhận được", "PASS"],
        ["TC-05", "Chặn thanh toán chưa gửi bếp", "Đơn còn món chưa gửi bếp", "Bấm 'THANH TOÁN (F9)'", "Chặn thanh toán, hiện popup cảnh báo còn món chưa gửi", "PASS"],
        ["TC-06", "Chặn thanh toán khi bếp chưa nấu xong", "Món đang ở bước COOKING", "Bấm 'THANH TOÁN (F9)'", "Khóa hoàn toàn thanh toán, yêu cầu đợi bếp báo nấu xong", "PASS"],
        ["TC-07", "Quy trình Bếp 3 bước", "Món ở trạng thái PENDING", "Bấm lần lượt: 1. Nhận món -> 2. Nấu món -> 3. Nấu xong", "Chuyển đúng trạng thái, cấm nhảy cóc bước, POS nhận DONE", "PASS"],
        ["TC-08", "Thanh toán & Xuất hóa đơn", "Tất cả món đã DONE", "Bấm 'THANH TOÁN', chọn Tiền mặt / Chuyển khoản", "Đơn chuyển PAID, giải phóng bàn, in bill PDF nhiệt K80", "PASS"],
        ["TC-09", "Chuyển bàn ăn", "Bàn A có khách, Bàn B trống", "Bấm 'Chuyển bàn', chọn Bàn A chuyển sang Bàn B", "Đơn hàng dời sang Bàn B, Bàn A trở về trạng thái Trống", "PASS"],
        ["TC-10", "Gộp bàn ăn", "Bàn 1 và Bàn 2 đều có khách", "Bấm 'Gộp bàn', gộp Bàn 1 vào Bàn 2", "Gộp toàn bộ món ăn về Bàn 2, cập nhật tổng tiền", "PASS"],
        ["TC-11", "Tiếp nhận đặt bàn trước", "Bàn hẹn còn trống", "Nhập tên khách, SĐT, giờ đến, chọn kênh Hotline/Zalo", "Lưu lịch đặt bàn, hiện trên bảng, bàn đổi màu RESERVED", "PASS"],
        ["TC-12", "Đóng ca & Đối soát két", "Ca làm việc đang OPEN", "Đếm tiền két nhập vào hệ thống, bấm 'Đóng ca'", "Tính chính xác số tiền chênh lệch (Thừa/Thiếu), in Z-Report", "PASS"]
    ]

    t_tc = doc.add_table(rows=1, cols=len(tc_headers))
    t_tc.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(t_tc)
    format_table_header(t_tc, tc_widths, tc_headers, "1E3A8A")
    format_data_rows(t_tc, tc_widths, tc_rows, tc_aligns)

    add_p(doc, "").paragraph_format.space_after = Pt(8)

    add_heading_2(doc, "4. Kết quả đạt được")
    add_p(doc, "Hệ thống đã hoàn thiện 100% các mục tiêu đề ra và vận hành ổn định trên môi trường thực tế:")
    add_bullet(doc, "Hoàn thiện trọn vẹn 12 Module chức năng tương ứng với 12 Tab trên thanh Menu điều hướng, bao quát toàn bộ các khâu nghiệp vụ của một nhà hàng quy mô từ vừa đến lớn.", "Hoàn thiện đầy đủ chức năng: ")
    add_bullet(doc, "Giao diện phẳng FlatLaf hiện đại, trực quan, hỗ trợ thẻ ảnh món ăn, phối màu trạng thái bàn và tiến độ món rõ ràng, nâng cao trải nghiệm người dùng.", "Giao diện thẩm mỹ và trực quan: ")
    add_bullet(doc, "Đồng bộ thời gian thực mượt mà giữa Quầy thu ngân và Màn hình bếp qua Observer Pattern; ngăn chặn triệt để mọi tình trạng thu tiền trước khi đồ ăn được nấu xong.", "Độ tin cậy và an toàn nghiệp vụ cao: ")
    add_bullet(doc, "Cơ sở dữ liệu 15 bảng chuẩn 3NF, toàn bộ giao dịch được bọc trong Transaction ACID, mã hóa mật khẩu bảo mật và hệ thống log kiểm toán đầy đủ.", "Kiến trúc chuẩn mực và toàn vẹn dữ liệu: ")
    add_bullet(doc, "Triển khai một chạm với các tệp kịch bản tự động hóa, dễ dàng mang sang máy tính khác cài đặt và chạy thử nghiệm mà không gặp trở ngại.", "Khả năng đóng gói và triển khai thuận tiện: ")

    add_heading_2(doc, "5. Những hạn chế của dự án và hướng phát triển")
    add_heading_3(doc, "5.1. Những hạn chế của dự án")
    add_bullet(doc, "Ứng dụng hiện tại là Desktop Application chạy trên hệ điều hành Windows qua Java Swing, chưa hỗ trợ trực tiếp trên nền tảng di động (Android/iOS) để nhân viên phục vụ có thể cầm máy tính bảng order ngay tại bàn ăn.", "Môi trường hoạt động: ")
    add_bullet(doc, "Phương thức thanh toán chuyển khoản hiện tại mới dừng ở mức xác nhận qua hệ thống và hiển thị mã QR tĩnh, chưa tích hợp Webhook ngân hàng (như VietQR Pro, VNPay, MoMo) để tự động nhận diện biến động số dư và hoàn tất đơn hàng mà không cần thu ngân kiểm tra màn hình điện thoại.", "Cổng thanh toán tự động: ")
    add_bullet(doc, "Đã hoàn thiện việc xuất file PDF chuẩn hóa đơn nhiệt và mô phỏng tập lệnh ESC/POS, tuy nhiên việc in ra máy in nhiệt thực tế vẫn phụ thuộc vào việc cài đặt Driver máy in cục bộ của hệ điều hành.", "Cơ chế in ấn vật lý: ")

    add_heading_3(doc, "5.2. Hướng phát triển trong tương lai")
    add_bullet(doc, "Xây dựng ứng dụng di động dành cho nhân viên phục vụ (Order tại bàn) và ứng dụng quét mã QR trên bàn để khách hàng tự gọi món (Self-ordering).", "Phát triển ứng dụng Mobile Client: ")
    add_bullet(doc, "Tách phần nghiệp vụ Backend thành các dịch vụ độc lập viết bằng Spring Boot, lưu trữ CSDL trên đám mây (Cloud Database) để phục vụ chuỗi nhà hàng (Multi-branch Chain Management) với hệ thống báo cáo tập trung.", "Chuyển đổi kiến trúc sang RESTful API / Microservices: ")
    add_bullet(doc, "Kết nối API VietQR động (Dynamic QR Code) có sinh nội dung chuyển khoản tự động và lắng nghe Webhook ngân hàng để tự động đóng hóa đơn ngay khi khách chuyển khoản thành công.", "Tích hợp cổng thanh toán trực tuyến thông minh: ")
    add_bullet(doc, "Phân tích dữ liệu lịch sử bán hàng để dự báo nhu cầu tiêu thụ món ăn theo ngày trong tuần và thời tiết, từ đó tự động gợi ý số lượng nguyên liệu cần nhập kho mỗi ngày, giúp nhà hàng tối ưu hóa chi phí lưu kho và hạn chế tối đa thực phẩm hết hạn.", "Ứng dụng Trí tuệ nhân tạo (AI & Machine Learning): ")

    # Ngắt trang sang Phần III
    doc.add_page_break()

    # =========================================================================
    # PHẦN III: TỔNG KẾT, KẾT LUẬN & TÀI LIỆU THAM KHẢO
    # =========================================================================
    add_heading_1(doc, "PHẦN III: TỔNG KẾT, KẾT LUẬN & TÀI LIỆU THAM KHẢO")

    add_heading_2(doc, "1. Kết luận")
    add_p(doc, 
        "Sau thời gian nghiên cứu nghiêm túc và nỗ lực phát triển, nhóm 13 chúng em đã hoàn thành xuất sắc đề tài \"Hệ Thống Quản Lý Nhà Hàng Toàn Diện (Restaurant Management System & POS)\". Dự án không chỉ là minh chứng rõ nét cho việc làm chủ các kiến thức lập trình Java nâng cao, kiến trúc hướng đối tượng, thiết kế CSDL quan hệ và các mẫu thiết kế phần mềm kinh điển, mà còn mang lại một sản phẩm phần mềm có tính ứng dụng thực tiễn cao, giải quyết triệt để các bài toán hóc búa trong công tác quản lý và vận hành nhà hàng hiện đại."
    )
    add_p(doc, 
        "Thông qua quá trình thực hiện bài tập lớn, các thành viên trong nhóm đã được rèn luyện tác phong làm việc khoa học, nâng cao kỹ năng tư duy phân tích hệ thống, kỹ năng giải quyết sự cố phát sinh và tinh thần phối hợp đồng đội hiệu quả."
    )

    add_heading_2(doc, "2. Lời cảm ơn")
    add_p(doc, 
        "Nhóm chúng em xin bày tỏ lòng biết ơn chân thành và sâu sắc nhất tới cô ThS. Nguyễn Thị Lan Anh – Giảng viên hướng dẫn môn học Lập trình ứng dụng Java nâng cao. Cô đã luôn tận tình chỉ bảo, truyền đạt những kiến thức chuyên môn quý báu, định hướng giải pháp và đưa ra những lời nhận xét xác đáng để nhóm có thể hoàn thiện đồ án này một cách tốt nhất."
    )
    add_p(doc, 
        "Chúng em cũng xin trân trọng cảm ơn các thầy cô giáo trong Khoa Công nghệ Thông tin – Trường Đại học Công nghệ Giao thông Vận tải đã tạo điều kiện học tập thuận lợi và trang bị cho chúng em nền tảng kiến thức vững chắc trong suốt quá trình học tập tại trường."
    )

    add_heading_2(doc, "3. Tài liệu tham khảo")
    add_p(doc, "Tài liệu và nguồn tham khảo chính được sử dụng trong quá trình nghiên cứu và thực hiện đề tài:")

    refs = [
        "[1] Herbert Schildt, \"Java: The Complete Reference, Twelfth Edition\", McGraw Hill, 2021.",
        "[2] Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides, \"Design Patterns: Elements of Reusable Object-Oriented Software\", Addison-Wesley Professional, 1994.",
        "[3] Robert C. Martin, \"Clean Architecture: A Craftsman's Guide to Software Structure and Design\", Prentice Hall, 2017.",
        "[4] Oracle Corporation, \"Java SE 17 Documentation & API Specification\", https://docs.oracle.com/en/java/javase/17/.",
        "[5] FormDev Software GmbH, \"FlatLaf - Flat Look and Feel for Swing\", https://www.formdev.com/flatlaf/.",
        "[6] Microsoft Corporation, \"Microsoft SQL Server Technical Documentation\", https://learn.microsoft.com/en-us/sql/sql-server/.",
        "[7] iText Group NV, \"iText 5 - Building High-Performance PDF Workflows\", https://github.com/itext/itextpdf.",
        "[8] Brett Wooldridge, \"HikariCP: A high-performance JDBC connection pool\", https://github.com/brettwooldridge/HikariCP."
    ]
    for r in refs:
        p_ref = doc.add_paragraph()
        p_ref.paragraph_format.space_before = Pt(2)
        p_ref.paragraph_format.space_after = Pt(4)
        p_ref.paragraph_format.line_spacing = 1.3
        p_ref.paragraph_format.left_indent = Inches(0.4)
        p_ref.paragraph_format.first_line_indent = Inches(-0.4)
        run_r = p_ref.add_run(r)
        run_r.font.name = "Times New Roman"
        run_r.font.size = Pt(12)
        run_r.font.color.rgb = RGBColor(30, 41, 59)

    doc.save(output_filename)
    print(f"SUCCESS: Report saved to: {output_filename}")

if __name__ == "__main__":
    create_full_report()
