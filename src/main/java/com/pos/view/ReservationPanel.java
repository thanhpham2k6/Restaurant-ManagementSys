package com.pos.view;

import com.pos.controller.ReservationController;
import com.pos.dao.TableDAO;
import com.pos.model.DiningTable;
import com.pos.model.Reservation;
import com.pos.model.ReservationStatus;
import com.pos.model.User;
import com.pos.utils.CurrencyFormat;
import com.pos.utils.DateUtils;
import com.pos.utils.QRCodeService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Phân hệ Quản lý Đặt bàn trước (Reservations) & Mã QR Table.
 */
public class ReservationPanel extends JPanel {
    private final ReservationController reservationController;
    private final TableDAO tableDAO;
    private final User currentUser;

    private JTable resTable;
    private DefaultTableModel tableModel;
    private List<Reservation> currentList;

    private JComboBox<String> cbFilterDate;
    private JButton btnAdd;
    private JButton btnConfirm;
    private JButton btnSeatGuest;
    private JButton btnCancel;
    private JButton btnViewQR;
    private JButton btnRefresh;

    public ReservationPanel(User currentUser) {
        this.currentUser = currentUser;
        this.reservationController = new ReservationController();
        this.tableDAO = new TableDAO();
        initComponents();
        loadDataAsync();
    }

    private void initComponents() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(new Color(245, 247, 250));

        // ==================== TOP BAR ====================
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);

        JLabel title = new JLabel("QUẢN LÝ ĐẶT BÀN TRƯỚC (RESERVATIONS) & QR TABLE");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(33, 33, 33));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterPanel.setOpaque(false);

        cbFilterDate = new JComboBox<>(new String[]{"Hôm nay", "Tất cả thời gian"});
        cbFilterDate.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbFilterDate.addActionListener(e -> loadDataAsync());

        btnAdd = createButton("+ Đặt bàn mới", new Color(46, 125, 50));
        btnConfirm = createButton("Xác nhận", new Color(30, 136, 229));
        btnSeatGuest = createButton("Xếp khách (Vào bàn)", new Color(230, 81, 0));
        btnCancel = createButton("Hủy đặt", new Color(211, 47, 47));
        btnViewQR = createButton("Mã QR Bàn", new Color(92, 107, 115));
        btnRefresh = createButton("Làm mới", new Color(120, 144, 156));

        btnAdd.addActionListener(e -> showAddReservationDialog());
        btnConfirm.addActionListener(e -> confirmSelected());
        btnSeatGuest.addActionListener(e -> seatSelected());
        btnCancel.addActionListener(e -> cancelSelected());
        btnViewQR.addActionListener(e -> showTableQRCodeDialog());
        btnRefresh.addActionListener(e -> loadDataAsync());

        filterPanel.add(new JLabel("Lọc:"));
        filterPanel.add(cbFilterDate);
        filterPanel.add(btnAdd);
        filterPanel.add(btnConfirm);
        filterPanel.add(btnSeatGuest);
        filterPanel.add(btnCancel);
        filterPanel.add(btnViewQR);
        filterPanel.add(btnRefresh);

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(filterPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // ==================== TABLE ====================
        String[] cols = {"ID", "Bàn", "Khách hàng", "Số điện thoại", "Giờ đặt", "Số khách", "Tiền cọc", "Trạng thái", "Ghi chú"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        resTable = new JTable(tableModel);
        resTable.setRowHeight(32);
        resTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        resTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        resTable.getTableHeader().setBackground(new Color(238, 242, 246));
        resTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resTable.setAutoCreateRowSorter(true);

        JScrollPane scroll = new JScrollPane(resTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(224, 224, 224)));
        add(scroll, BorderLayout.CENTER);
    }

    private JButton createButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void loadDataAsync() {
        boolean todayOnly = cbFilterDate.getSelectedIndex() == 0;
        new SwingWorker<List<Reservation>, Void>() {
            @Override
            protected List<Reservation> doInBackground() {
                return todayOnly ? reservationController.getTodayReservations() : reservationController.getAllReservations();
            }

            @Override
            protected void done() {
                try {
                    currentList = get();
                    tableModel.setRowCount(0);
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    for (Reservation r : currentList) {
                        tableModel.addRow(new Object[]{
                            r.getId(),
                            r.getTableName() != null ? r.getTableName() : "Bàn #" + r.getTableId(),
                            r.getCustomerName(),
                            r.getCustomerPhone(),
                            r.getReservationTime() != null ? r.getReservationTime().format(dtf) : "",
                            r.getGuestCount() + " người",
                            CurrencyFormat.format(r.getDepositAmount()),
                            r.getStatus().getDisplayName(),
                            r.getNote() != null ? r.getNote() : ""
                        });
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private Reservation getSelectedReservation() {
        int viewRow = resTable.getSelectedRow();
        if (viewRow < 0 || currentList == null) {
            return null;
        }
        int row = resTable.convertRowIndexToModel(viewRow);
        if (row >= 0 && row < currentList.size()) {
            return currentList.get(row);
        }
        return null;
    }

    private void confirmSelected() {
        Reservation r = getSelectedReservation();
        if (r == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn lịch đặt cần xác nhận.");
            return;
        }
        reservationController.confirmReservation(r.getId());
        loadDataAsync();
    }

    private void seatSelected() {
        Reservation r = getSelectedReservation();
        if (r == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn lịch đặt cần xếp khách vào bàn.");
            return;
        }
        reservationController.seatGuest(r.getId(), r.getTableId());
        JOptionPane.showMessageDialog(this, "Khách đã vào " + r.getTableName() + ". Bàn đã chuyển sang trạng thái CÓ KHÁCH!");
        loadDataAsync();
    }

    private void cancelSelected() {
        Reservation r = getSelectedReservation();
        if (r == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn lịch đặt cần hủy.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn hủy đặt bàn này?", "Hủy đặt bàn", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            reservationController.cancelReservation(r.getId(), r.getTableId());
            loadDataAsync();
        }
    }

    private void showAddReservationDialog() {
        List<DiningTable> tables = tableDAO.findAll();
        JComboBox<DiningTable> cbTables = new JComboBox<>(tables.toArray(new DiningTable[0]));
        JTextField txtName = new JTextField();
        JTextField txtPhone = new JTextField();
        JTextField txtGuests = new JTextField("2");
        JTextField txtDeposit = new JTextField("0");
        JTextField txtTime = new JTextField(LocalDateTime.now().plusHours(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        JTextField txtNote = new JTextField();

        JPanel p = new JPanel(new GridLayout(7, 2, 8, 8));
        p.add(new JLabel("Chọn bàn:")); p.add(cbTables);
        p.add(new JLabel("Tên khách hàng:")); p.add(txtName);
        p.add(new JLabel("Số điện thoại:")); p.add(txtPhone);
        p.add(new JLabel("Thời gian (yyyy-MM-dd HH:mm):")); p.add(txtTime);
        p.add(new JLabel("Số lượng khách:")); p.add(txtGuests);
        p.add(new JLabel("Tiền cọc (VND):")); p.add(txtDeposit);
        p.add(new JLabel("Ghi chú:")); p.add(txtNote);

        int res = JOptionPane.showConfirmDialog(this, p, "Tạo phiếu đặt bàn mới", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                DiningTable dt = (DiningTable) cbTables.getSelectedItem();
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime ldt = LocalDateTime.parse(txtTime.getText().trim(), dtf);
                int guests = Integer.parseInt(txtGuests.getText().trim());
                BigDecimal dep = new BigDecimal(txtDeposit.getText().trim());

                reservationController.createReservation(dt.getId(), null, txtName.getText(), txtPhone.getText(),
                        ldt, guests, dep, txtNote.getText());
                JOptionPane.showMessageDialog(this, "Đặt bàn thành công!");
                loadDataAsync();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showTableQRCodeDialog() {
        List<DiningTable> tables = tableDAO.findAll();
        if (tables.isEmpty()) return;

        JComboBox<DiningTable> cb = new JComboBox<>(tables.toArray(new DiningTable[0]));
        JLabel qrLabel = new JLabel("", SwingConstants.CENTER);
        qrLabel.setPreferredSize(new Dimension(220, 220));

        Runnable updateQr = () -> {
            DiningTable t = (DiningTable) cb.getSelectedItem();
            if (t != null) {
                String token = t.getQrCodeToken() != null ? t.getQrCodeToken() : "TABLE-" + t.getId();
                String url = QRCodeService.buildTableMenuUrl("192.168.1.100", 8080, token);
                BufferedImage img = QRCodeService.generateQRCode(url, 200, 200);
                if (img != null) {
                    qrLabel.setIcon(new ImageIcon(img));
                }
            }
        };

        cb.addActionListener(e -> updateQr.run());
        updateQr.run();

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER));
        top.add(new JLabel("Chọn bàn cần tạo mã:"));
        top.add(cb);

        panel.add(top, BorderLayout.NORTH);
        panel.add(qrLabel, BorderLayout.CENTER);
        JLabel note = new JLabel("<html><center>Khách hàng có thể quét mã QR này để xem Menu<br>và đặt món trực tiếp tại bàn.</center></html>", SwingConstants.CENTER);
        panel.add(note, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(this, panel, "Mã QR Đặt món tại bàn", JOptionPane.PLAIN_MESSAGE);
    }
}
