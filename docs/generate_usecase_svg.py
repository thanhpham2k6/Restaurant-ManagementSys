import os
import sys

svg_content = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1280 940" width="1280" height="940">
  <defs>
    <style>
      .title { font-family: 'Segoe UI', Arial, sans-serif; font-size: 18px; font-weight: bold; fill: #1e293b; }
      .sub-title { font-family: 'Segoe UI', Arial, sans-serif; font-size: 12px; fill: #64748b; }
      .actor-name { font-family: 'Segoe UI', Arial, sans-serif; font-size: 13px; font-weight: bold; fill: #0f172a; }
      .actor-role { font-family: 'Segoe UI', Arial, sans-serif; font-size: 11px; fill: #475569; font-style: italic; }
      .uc-text { font-family: 'Segoe UI', Arial, sans-serif; font-size: 12px; font-weight: 600; fill: #1e3a8a; }
      .uc-sub { font-family: 'Segoe UI', Arial, sans-serif; font-size: 10px; fill: #3b82f6; }
      .rel-label { font-family: 'Segoe UI', Arial, sans-serif; font-size: 10px; font-weight: bold; fill: #d97706; }
      .group-title { font-family: 'Segoe UI', Arial, sans-serif; font-size: 12px; font-weight: bold; fill: #0369a1; text-transform: uppercase; letter-spacing: 0.5px; }
      .line-solid { stroke: #64748b; stroke-width: 1.4; }
      .line-manager { stroke: #94a3b8; stroke-width: 1.2; stroke-dasharray: 3,3; }
      .line-include { stroke: #d97706; stroke-width: 1.4; stroke-dasharray: 4,3; marker-end: url(#arrow-orange); }
      .line-extend { stroke: #059669; stroke-width: 1.4; stroke-dasharray: 4,3; marker-end: url(#arrow-green); }
    </style>
    <marker id="arrow-orange" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
      <path d="M 0 1 L 10 5 L 0 9 z" fill="#d97706" />
    </marker>
    <marker id="arrow-green" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
      <path d="M 0 1 L 10 5 L 0 9 z" fill="#059669" />
    </marker>
    <filter id="drop-shadow" x="-5%" y="-5%" width="110%" height="110%">
      <feDropShadow dx="1" dy="2" stdDeviation="2" flood-color="#000" flood-opacity="0.06" />
    </filter>
  </defs>

  <!-- Background -->
  <rect width="100%" height="100%" fill="#ffffff" />

  <!-- System Boundary -->
  <rect x="230" y="35" width="1010" height="875" rx="16" fill="#ffffff" stroke="#cbd5e1" stroke-width="2" filter="url(#drop-shadow)" />
  <rect x="230" y="35" width="1010" height="45" rx="16" fill="#f1f5f9" />
  <rect x="230" y="65" width="1010" height="15" fill="#f1f5f9" />
  <text x="735" y="64" text-anchor="middle" class="title">HỆ THỐNG QUẢN LÝ NHÀ HÀNG (RESTAURANT MANAGEMENT SYSTEM &amp; POS)</text>

  <!-- Sub-group containers -->
  <!-- Col 1: Bán hàng & Vận hành -->
  <rect x="250" y="95" width="280" height="795" rx="8" fill="#f8fafc" stroke="#e2e8f0" stroke-width="1.2" />
  <text x="390" y="118" text-anchor="middle" class="group-title">1. Nghiệp vụ Bán hàng (POS)</text>

  <!-- Col 2: Quan hệ Include/Extend & Bếp -->
  <rect x="550" y="95" width="340" height="795" rx="8" fill="#f8fafc" stroke="#e2e8f0" stroke-width="1.2" />
  <text x="720" y="118" text-anchor="middle" class="group-title">2. Phân hệ Bếp KDS &amp; Mở rộng</text>

  <!-- Col 3: Phân hệ Quản trị (Manager) -->
  <rect x="910" y="95" width="310" height="795" rx="8" fill="#f8fafc" stroke="#e2e8f0" stroke-width="1.2" />
  <text x="1065" y="118" text-anchor="middle" class="group-title">3. Quản trị &amp; Thống kê (Manager)</text>

  <!-- ==================== ACTOR 1: MANAGER (STICK FIGURE) ==================== -->
  <g transform="translate(110, 160)">
    <circle cx="0" cy="0" r="16" fill="#ffffff" stroke="#1e293b" stroke-width="2.5" />
    <line x1="0" y1="16" x2="0" y2="50" stroke="#1e293b" stroke-width="2.5" />
    <line x1="-24" y1="28" x2="24" y2="28" stroke="#1e293b" stroke-width="2.5" />
    <line x1="0" y1="50" x2="-18" y2="82" stroke="#1e293b" stroke-width="2.5" />
    <line x1="0" y1="50" x2="18" y2="82" stroke="#1e293b" stroke-width="2.5" />
    <text x="0" y="102" text-anchor="middle" class="actor-name">Quản lý</text>
    <text x="0" y="118" text-anchor="middle" class="actor-role">(Manager)</text>
  </g>

  <!-- ==================== ACTOR 2: CASHIER (STICK FIGURE) ==================== -->
  <g transform="translate(110, 480)">
    <circle cx="0" cy="0" r="16" fill="#ffffff" stroke="#1e293b" stroke-width="2.5" />
    <line x1="0" y1="16" x2="0" y2="50" stroke="#1e293b" stroke-width="2.5" />
    <line x1="-24" y1="28" x2="24" y2="28" stroke="#1e293b" stroke-width="2.5" />
    <line x1="0" y1="50" x2="-18" y2="82" stroke="#1e293b" stroke-width="2.5" />
    <line x1="0" y1="50" x2="18" y2="82" stroke="#1e293b" stroke-width="2.5" />
    <text x="0" y="102" text-anchor="middle" class="actor-name">Thu ngân</text>
    <text x="0" y="118" text-anchor="middle" class="actor-role">(Cashier)</text>
  </g>

  <!-- ==================== ACTOR 3: KITCHEN (STICK FIGURE) ==================== -->
  <g transform="translate(110, 770)">
    <circle cx="0" cy="0" r="16" fill="#ffffff" stroke="#1e293b" stroke-width="2.5" />
    <line x1="0" y1="16" x2="0" y2="50" stroke="#1e293b" stroke-width="2.5" />
    <line x1="-24" y1="28" x2="24" y2="28" stroke="#1e293b" stroke-width="2.5" />
    <line x1="0" y1="50" x2="-18" y2="82" stroke="#1e293b" stroke-width="2.5" />
    <line x1="0" y1="50" x2="18" y2="82" stroke="#1e293b" stroke-width="2.5" />
    <text x="0" y="102" text-anchor="middle" class="actor-name">Nhà bếp</text>
    <text x="0" y="118" text-anchor="middle" class="actor-role">(Kitchen)</text>
  </g>

  <!-- ==================== COLUMN 1 USE CASES (OVALS) ==================== -->
  <!-- UC1: Đăng nhập (y=160) -->
  <ellipse cx="390" cy="160" rx="115" ry="24" fill="#eff6ff" stroke="#2563eb" stroke-width="1.8" filter="url(#drop-shadow)" />
  <text x="390" y="164" text-anchor="middle" class="uc-text">Đăng nhập hệ thống &amp; Phân quyền</text>

  <!-- UC2: Sơ đồ bàn & Đặt bàn (y=260) -->
  <ellipse cx="390" cy="260" rx="115" ry="24" fill="#eff6ff" stroke="#2563eb" stroke-width="1.8" filter="url(#drop-shadow)" />
  <text x="390" y="264" text-anchor="middle" class="uc-text">Quản lý Sơ đồ bàn, Chuyển/Gộp bàn</text>

  <!-- UC3: Tiếp nhận Đặt bàn trước (y=360) -->
  <ellipse cx="390" cy="360" rx="115" ry="24" fill="#eff6ff" stroke="#2563eb" stroke-width="1.8" filter="url(#drop-shadow)" />
  <text x="390" y="364" text-anchor="middle" class="uc-text">Tiếp nhận Đặt bàn trước &amp; Tiền cọc</text>

  <!-- UC4: Gọi món POS (y=470) -->
  <ellipse cx="390" cy="470" rx="115" ry="26" fill="#dbeafe" stroke="#1d4ed8" stroke-width="2" filter="url(#drop-shadow)" />
  <text x="390" y="474" text-anchor="middle" class="uc-text">Gọi món tại bàn &amp; Bán mang về (POS)</text>

  <!-- UC5: Thanh toán & Xuất hóa đơn (y=590) -->
  <ellipse cx="390" cy="590" rx="115" ry="26" fill="#dbeafe" stroke="#1d4ed8" stroke-width="2" filter="url(#drop-shadow)" />
  <text x="390" y="594" text-anchor="middle" class="uc-text">Thanh toán &amp; Kiểm soát 2 tầng</text>

  <!-- UC6: Mở ca & Đóng ca (y=710) -->
  <ellipse cx="390" cy="710" rx="115" ry="24" fill="#eff6ff" stroke="#2563eb" stroke-width="1.8" filter="url(#drop-shadow)" />
  <text x="390" y="714" text-anchor="middle" class="uc-text">Mở ca &amp; Đóng ca đối soát két tiền</text>

  <!-- UC7: Chấm công (y=810) -->
  <ellipse cx="390" cy="810" rx="115" ry="24" fill="#eff6ff" stroke="#2563eb" stroke-width="1.8" filter="url(#drop-shadow)" />
  <text x="390" y="814" text-anchor="middle" class="uc-text">Chấm công Check-in / Check-out</text>

  <!-- ==================== COLUMN 2 USE CASES (EXTEND / INCLUDE & KITCHEN) ==================== -->
  <!-- UC8: Gửi yêu cầu sang Bếp (y=410) (include from UC4) -->
  <ellipse cx="720" cy="410" rx="125" ry="23" fill="#fef3c7" stroke="#d97706" stroke-width="1.6" filter="url(#drop-shadow)" />
  <text x="720" y="414" text-anchor="middle" class="uc-text" fill="#92400e">Gửi yêu cầu chế biến sang Bếp</text>

  <!-- UC9: Thêm thông tin khách mới (y=480) (extend to UC4) -->
  <ellipse cx="720" cy="480" rx="125" ry="23" fill="#ecfdf5" stroke="#059669" stroke-width="1.6" filter="url(#drop-shadow)" />
  <text x="720" y="484" text-anchor="middle" class="uc-text" fill="#065f46">Thêm thông tin khách mới (từ SĐT)</text>

  <!-- UC10: Áp dụng Voucher & Tích điểm (y=555) (extend to UC5) -->
  <ellipse cx="720" cy="555" rx="125" ry="23" fill="#ecfdf5" stroke="#059669" stroke-width="1.6" filter="url(#drop-shadow)" />
  <text x="720" y="559" text-anchor="middle" class="uc-text" fill="#065f46">Áp dụng Voucher &amp; Tích điểm thưởng</text>

  <!-- UC11: In hóa đơn nhiệt & Xuất PDF (y=625) (include from UC5) -->
  <ellipse cx="720" cy="625" rx="125" ry="23" fill="#fef3c7" stroke="#d97706" stroke-width="1.6" filter="url(#drop-shadow)" />
  <text x="720" y="629" text-anchor="middle" class="uc-text" fill="#92400e">In bill nhiệt &amp; Xuất file PDF 80mm</text>

  <!-- UC12: Màn hình Bếp KDS (y=745) -->
  <ellipse cx="720" cy="745" rx="125" ry="26" fill="#fee2e2" stroke="#dc2626" stroke-width="2" filter="url(#drop-shadow)" />
  <text x="720" y="749" text-anchor="middle" class="uc-text" fill="#991b1b">Điều phối món Bếp KDS (3 bước)</text>

  <!-- UC13: Từ chối món hết nguyên liệu (y=825) (extend to UC12) -->
  <ellipse cx="720" cy="825" rx="125" ry="23" fill="#ecfdf5" stroke="#059669" stroke-width="1.6" filter="url(#drop-shadow)" />
  <text x="720" y="829" text-anchor="middle" class="uc-text" fill="#065f46">Từ chối món khi hết nguyên liệu</text>

  <!-- ==================== COLUMN 3 USE CASES (MANAGER ADMIN) ==================== -->
  <!-- UC14: Dashboard KPI (y=160) -->
  <ellipse cx="1065" cy="160" rx="120" ry="23" fill="#f3e8ff" stroke="#7e22ce" stroke-width="1.6" filter="url(#drop-shadow)" />
  <text x="1065" y="164" text-anchor="middle" class="uc-text" fill="#581c87">Xem Dashboard &amp; Biểu đồ KPI</text>

  <!-- UC15: Quản lý Thực đơn (y=245) -->
  <ellipse cx="1065" cy="245" rx="120" ry="23" fill="#f3e8ff" stroke="#7e22ce" stroke-width="1.6" filter="url(#drop-shadow)" />
  <text x="1065" y="249" text-anchor="middle" class="uc-text" fill="#581c87">Quản lý Thực đơn &amp; Hình ảnh món</text>

  <!-- UC16: Quản lý Kho & Kiểm kê (y=335) -->
  <ellipse cx="1065" cy="335" rx="120" ry="23" fill="#f3e8ff" stroke="#7e22ce" stroke-width="1.6" filter="url(#drop-shadow)" />
  <text x="1065" y="339" text-anchor="middle" class="uc-text" fill="#581c87">Quản lý Kho &amp; Kiểm kê Stocktake</text>

  <!-- UC17: Quản lý Khách hàng & Hạng thẻ (y=425) -->
  <ellipse cx="1065" cy="425" rx="120" ry="23" fill="#f3e8ff" stroke="#7e22ce" stroke-width="1.6" filter="url(#drop-shadow)" />
  <text x="1065" y="429" text-anchor="middle" class="uc-text" fill="#581c87">Quản lý Khách hàng &amp; Phân hạng thẻ</text>

  <!-- UC18: Quản lý Voucher (y=515) -->
  <ellipse cx="1065" cy="515" rx="120" ry="23" fill="#f3e8ff" stroke="#7e22ce" stroke-width="1.6" filter="url(#drop-shadow)" />
  <text x="1065" y="519" text-anchor="middle" class="uc-text" fill="#581c87">Thiết lập Chương trình Voucher</text>

  <!-- UC19: Sổ quỹ Thu - Chi (y=605) -->
  <ellipse cx="1065" cy="605" rx="120" ry="23" fill="#f3e8ff" stroke="#7e22ce" stroke-width="1.6" filter="url(#drop-shadow)" />
  <text x="1065" y="609" text-anchor="middle" class="uc-text" fill="#581c87">Quản lý Sổ quỹ Thu - Chi tiền mặt</text>

  <!-- UC20: Báo cáo Doanh thu (y=695) -->
  <ellipse cx="1065" cy="695" rx="120" ry="23" fill="#f3e8ff" stroke="#7e22ce" stroke-width="1.6" filter="url(#drop-shadow)" />
  <text x="1065" y="699" text-anchor="middle" class="uc-text" fill="#581c87">Báo cáo Doanh thu &amp; Xu hướng bán</text>

  <!-- UC21: Quản trị Tài khoản (y=785) -->
  <ellipse cx="1065" cy="785" rx="120" ry="23" fill="#f3e8ff" stroke="#7e22ce" stroke-width="1.6" filter="url(#drop-shadow)" />
  <text x="1065" y="789" text-anchor="middle" class="uc-text" fill="#581c87">Quản trị Tài khoản &amp; Phân quyền</text>

  <!-- ==================== ASSOCIATIONS (ACTOR -> USE CASE) ==================== -->
  <!-- Manager lines -->
  <line x1="140" y1="190" x2="275" y2="160" class="line-solid" />
  <line x1="140" y1="190" x2="945" y2="160" class="line-solid" />
  <line x1="140" y1="190" x2="945" y2="245" class="line-solid" />
  <line x1="140" y1="190" x2="945" y2="335" class="line-solid" />
  <line x1="140" y1="190" x2="945" y2="425" class="line-solid" />
  <line x1="140" y1="190" x2="945" y2="515" class="line-solid" />
  <line x1="140" y1="190" x2="945" y2="605" class="line-solid" />
  <line x1="140" y1="190" x2="945" y2="695" class="line-solid" />
  <line x1="140" y1="190" x2="945" y2="785" class="line-solid" />

  <!-- Cashier lines -->
  <line x1="140" y1="510" x2="275" y2="165" class="line-solid" />
  <line x1="140" y1="510" x2="275" y2="260" class="line-solid" />
  <line x1="140" y1="510" x2="275" y2="360" class="line-solid" />
  <line x1="140" y1="510" x2="275" y2="470" class="line-solid" />
  <line x1="140" y1="510" x2="275" y2="590" class="line-solid" />
  <line x1="140" y1="510" x2="275" y2="710" class="line-solid" />
  <line x1="140" y1="510" x2="275" y2="810" class="line-solid" />

  <!-- Kitchen lines -->
  <line x1="140" y1="800" x2="275" y2="170" class="line-solid" />
  <line x1="140" y1="800" x2="595" y2="745" class="line-solid" />
  <line x1="140" y1="800" x2="275" y2="815" class="line-solid" />

  <!-- ==================== INCLUDE & EXTEND RELATIONSHIPS ==================== -->
  <!-- UC4 (Gọi món) -> UC8 (Gửi bếp) : <<include>> -->
  <line x1="475" y1="448" x2="605" y2="420" class="line-include" />
  <text x="535" y="426" class="rel-label">«include»</text>

  <!-- UC9 (Thêm khách) -> UC4 (Gọi món) : <<extend>> -->
  <line x1="600" y1="476" x2="505" y2="470" class="line-extend" />
  <text x="545" y="468" class="rel-label" fill="#059669">«extend»</text>

  <!-- UC10 (Voucher) -> UC5 (Thanh toán) : <<extend>> -->
  <line x1="605" y1="565" x2="495" y2="580" class="line-extend" />
  <text x="545" y="565" class="rel-label" fill="#059669">«extend»</text>

  <!-- UC5 (Thanh toán) -> UC11 (In hóa đơn) : <<include>> -->
  <line x1="485" y1="600" x2="605" y2="618" class="line-include" />
  <text x="535" y="603" class="rel-label">«include»</text>

  <!-- UC13 (Từ chối món) -> UC12 (Bếp KDS) : <<extend>> -->
  <line x1="660" y1="805" x2="695" y2="770" class="line-extend" />
  <text x="678" y="798" class="rel-label" fill="#059669">«extend»</text>
</svg>"""

os.makedirs('docs', exist_ok=True)
svg_path = os.path.abspath('docs/usecase_diagram.svg')
with open(svg_path, 'w', encoding='utf-8') as f:
    f.write(svg_content)

print('SVG written to:', svg_path)
