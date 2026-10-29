package com.pos.view;

import com.pos.controller.ShiftController;
import com.pos.dao.UserDAO;
import com.pos.model.Shift;
import com.pos.model.ShiftStatus;
import com.pos.model.User;
import com.pos.utils.CurrencyFormat;
import com.pos.utils.DateUtils;
import com.pos.utils.PrinterService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Phân hệ Quản lý Ca làm việc (Shift Management & Timetable).
 * Hỗ trợ Vận hành ca trực tiếp, Bàn giao két tiền, Báo cáo Z-Report và Thời khóa biểu phân ca (Calendar).
 */
public class ShiftPanel extends JPanel {
    private final ShiftController shiftController;
    private final User currentUser;
    private final UserDAO userDAO = new UserDAO();

    // Tab 1 UI
    private JLabel lblStatus;
    private JLabel lblCashierName;
    private JLabel lblCashSales;
    private JLabel lblTransferSales;
    private JLabel lblTotalRevenue;

    private JButton btnOpenShift;
    private JButton btnCloseShift;
    private JButton btnPrintZReport;
    private JButton btnRefresh;

    private JTable shiftTable;
    private DefaultTableModel tableModel;

    // Tab 2 Timetable UI
    private LocalDate currentWeekMonday;
    private JLabel lblWeekRange;
    private JPanel timetableGrid;
    private List<Shift> cachedAllShifts = new ArrayList<>();

    public ShiftPanel(User currentUser) {
        this.currentUser = currentUser;
        this.shiftController = new ShiftController();
        this.currentWeekMonday = LocalDate.now().with(DayOfWeek.MONDAY);
        initComponents();
        loadDataAsync();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabbedPane.addTab("Vận hành ca & Bàn giao", createLiveShiftPanel());
        tabbedPane.addTab("Thời khóa biểu phân ca (Lịch tuần)", createTimetablePanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==================== TAB 1: LIVE SHIFT & CASH DRAWER ====================
    private JPanel createLiveShiftPanel() {
        JPanel main = new JPanel(new BorderLayout(15, 15));
        main.setBorder(new EmptyBorder(15, 15, 15, 15));
        main.setOpaque(false);

        // TOP BANNER & ACTION BAR
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 2, 2));
        titlePanel.setOpaque(false);
        JLabel title = new JLabel("QUẢN LÝ CA LÀM VIỆC & KÉT TIỀN");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(30, 41, 59));
        JLabel lblSub = new JLabel("Theo dõi thời gian làm việc & Doanh thu theo ca của thu ngân");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(new Color(100, 116, 139));
        titlePanel.add(title);
        titlePanel.add(lblSub);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        btnOpenShift = createStyledButton("Mở ca mới", new Color(16, 185, 129));
        btnCloseShift = createStyledButton("Đóng ca (Kết ca)", new Color(239, 68, 68));
        btnPrintZReport = createStyledButton("In Z-Report ca", new Color(59, 130, 246));
        btnRefresh = createStyledButton("Làm mới", new Color(100, 116, 139));

        btnOpenShift.addActionListener(e -> showOpenShiftDialog());
        btnCloseShift.addActionListener(e -> showCloseShiftDialog());
        btnPrintZReport.addActionListener(e -> printCurrentZReport());
        btnRefresh.addActionListener(e -> loadDataAsync());

        btnPanel.add(btnOpenShift);
        btnPanel.add(btnCloseShift);
        btnPanel.add(btnPrintZReport);
        btnPanel.add(btnRefresh);

        topPanel.add(titlePanel, BorderLayout.WEST);
        topPanel.add(btnPanel, BorderLayout.EAST);

        // KPI CARDS
        JPanel cardsPanel = new JPanel(new GridLayout(1, 5, 12, 12));
        cardsPanel.setOpaque(false);
        cardsPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        lblStatus = new JLabel("Chưa mở ca", SwingConstants.CENTER);
        lblCashierName = new JLabel(currentUser.getFullName(), SwingConstants.CENTER);
        lblCashSales = new JLabel("0 đ", SwingConstants.CENTER);
        lblTransferSales = new JLabel("0 đ", SwingConstants.CENTER);
        lblTotalRevenue = new JLabel("0 đ", SwingConstants.CENTER);

        cardsPanel.add(createCard("Trạng thái ca", lblStatus, new Color(59, 130, 246)));
        cardsPanel.add(createCard("Thu ngân phụ trách", lblCashierName, new Color(100, 116, 139)));
        cardsPanel.add(createCard("Doanh thu Tiền mặt", lblCashSales, new Color(16, 185, 129)));
        cardsPanel.add(createCard("Doanh thu Chuyển khoản", lblTransferSales, new Color(139, 92, 246)));
        cardsPanel.add(createCard("TỔNG DOANH THU CA", lblTotalRevenue, new Color(225, 29, 72)));

        JPanel headerWrapper = new JPanel(new BorderLayout(5, 8));
        headerWrapper.setOpaque(false);
        headerWrapper.add(topPanel, BorderLayout.NORTH);
        headerWrapper.add(cardsPanel, BorderLayout.SOUTH);
        main.add(headerWrapper, BorderLayout.NORTH);

        // TABLE OF SHIFTS
        String[] cols = {"ID", "Thu ngân", "Bắt đầu ca", "Kết thúc ca", "Tiền mặt", "Chuyển khoản", "Tổng doanh thu", "Trạng thái", "Ghi chú"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        shiftTable = new JTable(tableModel);
        shiftTable.setRowHeight(34);
        shiftTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        shiftTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        shiftTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        shiftTable.setAutoCreateRowSorter(true);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        shiftTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        shiftTable.getColumnModel().getColumn(0).setCellRenderer(center);
        shiftTable.getColumnModel().getColumn(7).setPreferredWidth(100);
        shiftTable.getColumnModel().getColumn(7).setCellRenderer(center);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        shiftTable.getColumnModel().getColumn(4).setCellRenderer(right);
        shiftTable.getColumnModel().getColumn(5).setCellRenderer(right);
        shiftTable.getColumnModel().getColumn(6).setCellRenderer(right);

        JScrollPane scrollPane = new JScrollPane(shiftTable);
        scrollPane.setBorder(new CompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            new EmptyBorder(0, 0, 0, 0)
        ));
        main.add(scrollPane, BorderLayout.CENTER);
        return main;
    }

    // ==================== TAB 2: TIMETABLE / CALENDAR VIEW ====================
    private JPanel createTimetablePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setOpaque(false);

        // Control bar
        JPanel controlBar = new JPanel(new BorderLayout());
        controlBar.setOpaque(false);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        navPanel.setOpaque(false);

        JButton btnPrevWeek = createStyledButton("◀ Tuần trước", new Color(100, 116, 139));
        JButton btnThisWeek = createStyledButton("Tuần này", new Color(59, 130, 246));
        JButton btnNextWeek = createStyledButton("Tuần sau ▶", new Color(100, 116, 139));

        lblWeekRange = new JLabel();
        lblWeekRange.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblWeekRange.setForeground(new Color(30, 41, 59));
        lblWeekRange.setBorder(new EmptyBorder(0, 10, 0, 10));

        btnPrevWeek.addActionListener(e -> {
            currentWeekMonday = currentWeekMonday.minusWeeks(1);
            renderTimetable();
        });
        btnThisWeek.addActionListener(e -> {
            currentWeekMonday = LocalDate.now().with(DayOfWeek.MONDAY);
            renderTimetable();
        });
        btnNextWeek.addActionListener(e -> {
            currentWeekMonday = currentWeekMonday.plusWeeks(1);
            renderTimetable();
        });

        navPanel.add(btnPrevWeek);
        navPanel.add(btnThisWeek);
        navPanel.add(btnNextWeek);
        navPanel.add(lblWeekRange);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);
        JButton btnSchedule = createStyledButton("+ Xếp ca làm việc", new Color(16, 185, 129));
        JButton btnRefreshCal = createStyledButton("Làm mới", new Color(120, 144, 156));

        btnSchedule.addActionListener(e -> showScheduleShiftDialog());
        btnRefreshCal.addActionListener(e -> loadDataAsync());

        rightPanel.add(btnSchedule);
        rightPanel.add(btnRefreshCal);

        controlBar.add(navPanel, BorderLayout.WEST);
        controlBar.add(rightPanel, BorderLayout.EAST);
        panel.add(controlBar, BorderLayout.NORTH);

        // 7-day calendar grid
        timetableGrid = new JPanel(new GridLayout(1, 7, 8, 8));
        timetableGrid.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(timetableGrid);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void renderTimetable() {
        if (timetableGrid == null) return;
        timetableGrid.removeAll();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter shortDtf = DateTimeFormatter.ofPattern("dd/MM");
        LocalDate sunday = currentWeekMonday.plusDays(6);
        lblWeekRange.setText("Thời khóa biểu tuần: " + currentWeekMonday.format(dtf) + "  đến  " + sunday.format(dtf));

        String[] dayNames = {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"};
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 7; i++) {
            LocalDate day = currentWeekMonday.plusDays(i);
            boolean isToday = day.equals(today);

            JPanel dayCol = new JPanel(new BorderLayout(0, 6));
            dayCol.setBackground(Color.WHITE);
            dayCol.setBorder(new CompoundBorder(
                new LineBorder(isToday ? new Color(37, 99, 235) : new Color(226, 232, 240), isToday ? 2 : 1, true),
                new EmptyBorder(8, 8, 8, 8)
            ));

            // Header for Day
            JPanel colHeader = new JPanel(new GridLayout(2, 1));
            colHeader.setOpaque(false);
            JLabel lblDayName = new JLabel(dayNames[i], SwingConstants.CENTER);
            lblDayName.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblDayName.setForeground(isToday ? new Color(37, 99, 235) : new Color(30, 41, 59));

            JLabel lblDayDate = new JLabel(day.format(shortDtf) + (isToday ? " (Hôm nay)" : ""), SwingConstants.CENTER);
            lblDayDate.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblDayDate.setForeground(isToday ? new Color(37, 99, 235) : new Color(100, 116, 139));

            colHeader.add(lblDayName);
            colHeader.add(lblDayDate);
            dayCol.add(colHeader, BorderLayout.NORTH);

            // Container for shift cards
            JPanel cardContainer = new JPanel();
            cardContainer.setLayout(new BoxLayout(cardContainer, BoxLayout.Y_AXIS));
            cardContainer.setOpaque(false);

            List<Shift> dayShifts = new ArrayList<>();
            for (Shift s : cachedAllShifts) {
                if (s.getStartTime() != null && s.getStartTime().toLocalDate().equals(day)) {
                    dayShifts.add(s);
                }
            }

            if (dayShifts.isEmpty()) {
                JLabel emptyLbl = new JLabel("<html><center><br><i>Chưa có ca</i></center></html>", SwingConstants.CENTER);
                emptyLbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
                emptyLbl.setForeground(new Color(160, 174, 192));
                emptyLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
                cardContainer.add(emptyLbl);
            } else {
                for (Shift s : dayShifts) {
                    cardContainer.add(createShiftCard(s));
                    cardContainer.add(Box.createVerticalStrut(6));
                }
            }

            JScrollPane cardScroll = new JScrollPane(cardContainer);
            cardScroll.setBorder(BorderFactory.createEmptyBorder());
            cardScroll.setOpaque(false);
            cardScroll.getViewport().setOpaque(false);
            dayCol.add(cardScroll, BorderLayout.CENTER);

            timetableGrid.add(dayCol);
        }

        timetableGrid.revalidate();
        timetableGrid.repaint();
    }

    private JPanel createShiftCard(Shift s) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));
        card.setBackground(s.isOpen() ? new Color(240, 253, 244) : new Color(248, 250, 252));
        card.setBorder(new CompoundBorder(
            new LineBorder(s.isOpen() ? new Color(16, 185, 129) : new Color(226, 232, 240), 1, true),
            new EmptyBorder(6, 6, 6, 6)
        ));

        String timeStr = s.getStartTime() != null ? s.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "";
        String slotName = "Ca Sáng";
        if (s.getStartTime() != null) {
            int h = s.getStartTime().getHour();
            if (h >= 14 && h < 22) slotName = "Ca Chiều";
            else if (h >= 22 || h < 6) slotName = "Ca Đêm";
        }

        JLabel lblTitle = new JLabel("Ca #" + s.getId() + " - " + slotName);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(s.isOpen() ? new Color(22, 101, 52) : new Color(30, 41, 59));

        JLabel lblStaff = new JLabel("👤 " + (s.getUserFullName() != null ? s.getUserFullName() : "Thu ngân"));
        lblStaff.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblStaff.setForeground(new Color(71, 85, 105));

        BigDecimal tot = s.getCashSales().add(s.getTransferSales());
        JLabel lblRev = new JLabel("💰 " + CurrencyFormat.format(tot));
        lblRev.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblRev.setForeground(new Color(225, 29, 72));

        JLabel lblBadge = new JLabel(s.isOpen() ? "[ĐANG MỞ]" : "[ĐÃ ĐÓNG]");
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblBadge.setForeground(s.isOpen() ? new Color(16, 185, 129) : new Color(100, 116, 139));

        JPanel centerP = new JPanel(new GridLayout(2, 1, 2, 2));
        centerP.setOpaque(false);
        centerP.add(lblStaff);
        centerP.add(lblRev);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(centerP, BorderLayout.CENTER);
        card.add(lblBadge, BorderLayout.SOUTH);

        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                showZReportDialog(s);
            }
        });

        return card;
    }

    private void showScheduleShiftDialog() {
        List<User> users = userDAO.findAll();
        JComboBox<User> cbUser = new JComboBox<>(users.toArray(new User[0]));
        JTextField txtDate = new JTextField(LocalDate.now().toString());
        String[] slots = {
            "Ca Sáng (06:00 - 14:00)",
            "Ca Chiều (14:00 - 22:00)",
            "Ca Đêm (22:00 - 06:00)",
            "Tùy chỉnh (Giờ hiện tại)"
        };
        JComboBox<String> cbSlot = new JComboBox<>(slots);
        JTextField txtInitCash = new JTextField("0");
        JTextField txtNote = new JTextField("Phân ca làm việc thu ngân");

        JPanel p = new JPanel(new GridLayout(5, 2, 8, 8));
        p.add(new JLabel("Chọn thu ngân:")); p.add(cbUser);
        p.add(new JLabel("Ngày làm việc (yyyy-MM-dd):")); p.add(txtDate);
        p.add(new JLabel("Khung giờ ca:")); p.add(cbSlot);
        p.add(new JLabel("Tiền lẻ bàn giao (VND):")); p.add(txtInitCash);
        p.add(new JLabel("Ghi chú ca:")); p.add(txtNote);

        int opt = JOptionPane.showConfirmDialog(this, p, "Xếp Lịch Ca Làm Việc", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            try {
                User u = (User) cbUser.getSelectedItem();
                if (u == null) return;
                LocalDate d = LocalDate.parse(txtDate.getText().trim());
                LocalTime t = LocalTime.of(8, 0);
                int slotIdx = cbSlot.getSelectedIndex();
                if (slotIdx == 0) t = LocalTime.of(6, 0);
                else if (slotIdx == 1) t = LocalTime.of(14, 0);
                else if (slotIdx == 2) t = LocalTime.of(22, 0);
                else t = LocalTime.now();

                LocalDateTime start = LocalDateTime.of(d, t);
                BigDecimal initCash = new BigDecimal(txtInitCash.getText().trim());
                shiftController.scheduleShift(u, start, initCash, txtNote.getText().trim());
                JOptionPane.showMessageDialog(this, "Đã xếp ca thành công cho " + u.getFullName() + "!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                loadDataAsync();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi định dạng: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel createCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTitle.setForeground(new Color(100, 116, 139));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        valueLabel.setForeground(accentColor);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ==================== ASYNC ACTIONS & LOADERS ====================
    private void loadDataAsync() {
        new SwingWorker<List<Shift>, Void>() {
            private Shift curShift;

            @Override
            protected List<Shift> doInBackground() {
                curShift = shiftController.getCurrentShift();
                return shiftController.getAllShifts();
            }

            @Override
            protected void done() {
                try {
                    List<Shift> list = get();
                    cachedAllShifts = list;
                    tableModel.setRowCount(0);

                    for (Shift s : list) {
                        BigDecimal tot = s.getCashSales().add(s.getTransferSales());
                        tableModel.addRow(new Object[]{
                            s.getId(),
                            s.getUserFullName(),
                            DateUtils.format(s.getStartTime()),
                            s.getEndTime() != null ? DateUtils.format(s.getEndTime()) : "(Đang diễn ra)",
                            CurrencyFormat.format(s.getCashSales()),
                            CurrencyFormat.format(s.getTransferSales()),
                            CurrencyFormat.format(tot),
                            s.getStatus().getDisplayName(),
                            s.getNote() != null ? s.getNote() : ""
                        });
                    }

                    if (curShift != null && curShift.isOpen()) {
                        lblStatus.setText("ĐANG MỞ (Ca #" + curShift.getId() + ")");
                        lblStatus.setForeground(new Color(16, 185, 129));
                        lblCashierName.setText(curShift.getUserFullName() != null ? curShift.getUserFullName() : currentUser.getFullName());
                        lblCashSales.setText(CurrencyFormat.format(curShift.getCashSales()));
                        lblTransferSales.setText(CurrencyFormat.format(curShift.getTransferSales()));
                        lblTotalRevenue.setText(CurrencyFormat.format(curShift.getCashSales().add(curShift.getTransferSales())));

                        btnOpenShift.setEnabled(false);
                        btnCloseShift.setEnabled(true);
                        btnPrintZReport.setEnabled(true);
                    } else {
                        lblStatus.setText("CHƯA MỞ CA");
                        lblStatus.setForeground(new Color(239, 68, 68));
                        lblCashierName.setText(currentUser.getFullName());
                        lblCashSales.setText("0 đ");
                        lblTransferSales.setText("0 đ");
                        lblTotalRevenue.setText("0 đ");

                        btnOpenShift.setEnabled(true);
                        btnCloseShift.setEnabled(false);
                        btnPrintZReport.setEnabled(true); // Luôn cho phép in Z-Report ca gần nhất
                    }

                    renderTimetable();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void showOpenShiftDialog() {
        JTextField txtNote = new JTextField("Ca làm việc " + currentUser.getFullName());
        JPanel p = new JPanel(new GridLayout(2, 2, 8, 8));

        JLabel lblStaff = new JLabel(currentUser.getFullName() + " (" + currentUser.getUsername() + ")");
        lblStaff.setFont(new Font("Segoe UI", Font.BOLD, 13));

        p.add(new JLabel("Thu ngân mở ca:"));
        p.add(lblStaff);
        p.add(new JLabel("Ghi chú ca:"));
        p.add(txtNote);

        int opt = JOptionPane.showConfirmDialog(this, p, "Xác nhận Mở Ca Làm Việc", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            try {
                shiftController.openShift(currentUser, BigDecimal.ZERO, txtNote.getText().trim());
                JOptionPane.showMessageDialog(this, "Đã mở ca làm việc thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                loadDataAsync();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Lỗi mở ca: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showCloseShiftDialog() {
        Shift cur = shiftController.getCurrentShift();
        if (cur == null || !cur.isOpen()) {
            JOptionPane.showMessageDialog(this, "Hiện tại không có ca làm việc nào đang mở!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal tot = cur.getCashSales().add(cur.getTransferSales());
        JTextField txtNote = new JTextField();

        JLabel lblShiftId = new JLabel("#" + cur.getId());
        lblShiftId.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JLabel lblStaff = new JLabel(cur.getUserFullName() != null ? cur.getUserFullName() : currentUser.getFullName());
        lblStaff.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JLabel lblCash = new JLabel(CurrencyFormat.format(cur.getCashSales()));
        lblCash.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblCash.setForeground(new Color(16, 185, 129));

        JLabel lblTransfer = new JLabel(CurrencyFormat.format(cur.getTransferSales()));
        lblTransfer.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTransfer.setForeground(new Color(139, 92, 246));

        JPanel p = new JPanel(new GridLayout(5, 2, 8, 8));
        p.add(new JLabel("Mã ca:"));
        p.add(lblShiftId);
        p.add(new JLabel("Thu ngân:"));
        p.add(lblStaff);
        p.add(new JLabel("Doanh thu tiền mặt:"));
        p.add(lblCash);
        p.add(new JLabel("Doanh thu chuyển khoản:"));
        p.add(lblTransfer);
        p.add(new JLabel("Ghi chú bàn giao:"));
        p.add(txtNote);

        int opt = JOptionPane.showConfirmDialog(this, p, "Xác nhận Đóng Ca (Tổng doanh thu: " + CurrencyFormat.format(tot) + ")", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            try {
                cur.setEndTime(LocalDateTime.now());
                cur.setStatus(ShiftStatus.CLOSED);
                cur.setActualCash(cur.getCashSales());
                cur.setDifferenceCash(BigDecimal.ZERO);
                cur.setNote(txtNote.getText().trim());

                shiftController.closeShift(cur.getCashSales(), txtNote.getText().trim());
                loadDataAsync();

                // Hiển thị trực tiếp Z-Report và kích hoạt in nhiệt
                showZReportDialog(cur);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Lỗi đóng ca: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void printCurrentZReport() {
        Shift cur = shiftController.getCurrentShift();
        if (cur == null) {
            int viewRow = shiftTable.getSelectedRow();
            if (viewRow < 0 && shiftTable.getRowCount() > 0) {
                viewRow = 0; // Tự động chọn ca gần nhất
                shiftTable.setRowSelectionInterval(0, 0);
            }
            if (viewRow >= 0) {
                int row = shiftTable.convertRowIndexToModel(viewRow);
                int id = (int) tableModel.getValueAt(row, 0);
                cur = cachedAllShifts.stream().filter(s -> s.getId() == id).findFirst().orElse(null);
            }
        }
        if (cur == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một ca làm việc trong danh sách để in Z-Report!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        PrinterService.printZReport(cur);
        showZReportDialog(cur);
    }

    private void showZReportDialog(Shift shift) {
        if (shift == null) return;
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Z-REPORT CA #" + shift.getId(), true);
        dialog.setSize(460, 560);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JTextArea txt = new JTextArea();
        txt.setFont(new Font("Monospaced", Font.PLAIN, 13));
        txt.setEditable(false);
        txt.setMargin(new Insets(15, 15, 15, 15));

        StringBuilder sb = new StringBuilder();
        sb.append("==========================================\n");
        sb.append("          BÁO CÁO ĐÓNG CA (Z-REPORT)       \n");
        sb.append("==========================================\n\n");
        sb.append(String.format("Mã ca:              #%d\n", shift.getId()));
        sb.append(String.format("Thu ngân:           %s\n", shift.getUserFullName() != null ? shift.getUserFullName() : currentUser.getFullName()));
        sb.append(String.format("Giờ mở ca:          %s\n", shift.getStartTime() != null ? DateUtils.format(shift.getStartTime()) : "-"));
        sb.append(String.format("Giờ đóng ca:        %s\n", shift.getEndTime() != null ? DateUtils.format(shift.getEndTime()) : DateUtils.format(LocalDateTime.now())));
        sb.append(String.format("Trạng thái:         %s\n", shift.getStatus() != null ? shift.getStatus().getDisplayName() : "ĐÃ ĐÓNG"));
        sb.append("------------------------------------------\n");
        sb.append(String.format("Tiền lẻ ban đầu:    %s\n", CurrencyFormat.format(shift.getInitialCash())));
        sb.append(String.format("Doanh thu tiền mặt: %s\n", CurrencyFormat.format(shift.getCashSales())));
        sb.append(String.format("Doanh thu CK:       %s\n", CurrencyFormat.format(shift.getTransferSales())));
        BigDecimal tot = shift.getCashSales().add(shift.getTransferSales());
        sb.append(String.format("TỔNG DOANH THU CA:  %s\n", CurrencyFormat.format(tot)));
        sb.append("------------------------------------------\n");
        BigDecimal exp = shift.getInitialCash().add(shift.getCashSales());
        BigDecimal act = shift.getActualCash() != null ? shift.getActualCash() : exp;
        BigDecimal diff = shift.getDifferenceCash() != null ? shift.getDifferenceCash() : BigDecimal.ZERO;
        sb.append(String.format("Tiền mặt lý thuyết: %s\n", CurrencyFormat.format(exp)));
        sb.append(String.format("Tiền mặt thực tế:   %s\n", CurrencyFormat.format(act)));
        sb.append(String.format("Chênh lệch két:     %s\n", CurrencyFormat.format(diff)));
        if (shift.getNote() != null && !shift.getNote().isBlank()) {
            sb.append(String.format("Ghi chú:            %s\n", shift.getNote()));
        }
        sb.append("\n==========================================\n");
        sb.append("      ĐÃ GỬI LỆNH TỚI MÁY IN NHIỆT ESC/POS\n");
        sb.append("==========================================\n");

        txt.setText(sb.toString());
        dialog.add(new JScrollPane(txt), BorderLayout.CENTER);

        JPanel btnP = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnPrint = createStyledButton("🖨️ In lại Z-Report", new Color(59, 130, 246));
        JButton btnClose = createStyledButton("Đóng", new Color(100, 116, 139));

        btnPrint.addActionListener(e -> {
            PrinterService.printZReport(shift);
            JOptionPane.showMessageDialog(dialog, "Đã gửi lại lệnh in Z-Report tới máy in nhiệt ESC/POS!");
        });
        btnClose.addActionListener(e -> dialog.dispose());

        btnP.add(btnPrint);
        btnP.add(btnClose);
        dialog.add(btnP, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
}
