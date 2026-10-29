package com.pos.view;

import com.pos.dao.VoucherDAO;
import com.pos.model.DiscountType;
import com.pos.model.User;
import com.pos.model.Voucher;
import com.pos.utils.CurrencyFormat;
import com.pos.utils.TableUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Phân hệ Quản lý Khuyến mãi động (Vouchers / Promocodes).
 */
public class VoucherPanel extends JPanel {
    private final VoucherDAO voucherDAO;
    private final User currentUser;

    private JTable voucherTable;
    private DefaultTableModel tableModel;
    private List<Voucher> currentList;

    public VoucherPanel(User currentUser) {
        this.currentUser = currentUser;
        this.voucherDAO = new VoucherDAO();
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

        JLabel title = new JLabel("QUẢN LÝ MÃ KHUYẾN MÃI & GIẢM GIÁ");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(33, 33, 33));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        JButton btnAdd = createButton("+ Thêm Voucher", new Color(46, 125, 50));
        JButton btnToggle = createButton("Bật/Tắt hiệu lực", new Color(230, 81, 0));
        JButton btnDelete = createButton("Xóa", new Color(211, 47, 47));
        JButton btnRefresh = createButton("Làm mới", new Color(120, 144, 156));

        btnAdd.addActionListener(e -> showAddDialog());
        btnToggle.addActionListener(e -> toggleActiveSelected());
        btnDelete.addActionListener(e -> deleteSelected());
        btnRefresh.addActionListener(e -> loadDataAsync());

        btnPanel.add(btnAdd);
        btnPanel.add(btnToggle);
        btnPanel.add(btnDelete);
        btnPanel.add(btnRefresh);

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(btnPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // ==================== TABLE ====================
        String[] cols = {"ID", "Mã Voucher", "Loại giảm giá", "Mức giảm", "Đơn tối thiểu", "Giảm tối đa", "Bắt đầu", "Kết thúc", "Đã dùng / Giới hạn", "Trạng thái"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        voucherTable = new JTable(tableModel);
        voucherTable.setRowHeight(32);
        voucherTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        voucherTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        voucherTable.getTableHeader().setBackground(new Color(238, 242, 246));
        voucherTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableUtils.applySmartSorting(voucherTable);

        JScrollPane scroll = new JScrollPane(voucherTable);
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
        new SwingWorker<List<Voucher>, Void>() {
            @Override
            protected List<Voucher> doInBackground() {
                return voucherDAO.findAll();
            }

            @Override
            protected void done() {
                try {
                    currentList = get();
                    tableModel.setRowCount(0);
                    for (Voucher v : currentList) {
                        String valStr = v.getDiscountType() == DiscountType.PERCENT
                                ? v.getDiscountVal() + " %"
                                : CurrencyFormat.format(v.getDiscountVal());
                        String maxStr = v.getMaxDiscountAmount() != null
                                ? CurrencyFormat.format(v.getMaxDiscountAmount())
                                : "Không giới hạn";
                        String statusStr = v.isActive() ? "Đang áp dụng" : "Tạm ngưng";

                        tableModel.addRow(new Object[]{
                            v.getId(),
                            v.getCode(),
                            v.getDiscountType().getDisplayName(),
                            valStr,
                            CurrencyFormat.format(v.getMinOrderAmount()),
                            maxStr,
                            v.getStartDate(),
                            v.getEndDate(),
                            v.getUsedCount() + " / " + v.getUsageLimit(),
                            statusStr
                        });
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private Voucher getSelected() {
        int viewRow = voucherTable.getSelectedRow();
        if (viewRow < 0 || currentList == null) {
            return null;
        }
        int r = voucherTable.convertRowIndexToModel(viewRow);
        if (r >= 0 && r < currentList.size()) {
            return currentList.get(r);
        }
        return null;
    }

    private void showAddDialog() {
        JTextField txtCode = new JTextField();
        JComboBox<DiscountType> cbType = new JComboBox<>(new DiscountType[]{DiscountType.PERCENT, DiscountType.FIXED});
        JTextField txtVal = new JTextField("10");
        JTextField txtMinOrder = new JTextField("100000");
        JTextField txtMaxDisc = new JTextField("50000");
        JTextField txtStart = new JTextField(LocalDate.now().toString());
        JTextField txtEnd = new JTextField(LocalDate.now().plusMonths(1).toString());
        JTextField txtLimit = new JTextField("100");

        JPanel p = new JPanel(new GridLayout(8, 2, 8, 8));
        p.add(new JLabel("Mã Code (viết liền không dấu):")); p.add(txtCode);
        p.add(new JLabel("Loại giảm giá:")); p.add(cbType);
        p.add(new JLabel("Mức giảm (% hoặc số tiền VND):")); p.add(txtVal);
        p.add(new JLabel("Giá trị đơn tối thiểu:")); p.add(txtMinOrder);
        p.add(new JLabel("Giảm tối đa (để trống nếu không giới hạn):")); p.add(txtMaxDisc);
        p.add(new JLabel("Ngày bắt đầu (yyyy-MM-dd):")); p.add(txtStart);
        p.add(new JLabel("Ngày kết thúc (yyyy-MM-dd):")); p.add(txtEnd);
        p.add(new JLabel("Giới hạn lượt dùng:")); p.add(txtLimit);

        int res = JOptionPane.showConfirmDialog(this, p, "Tạo mã Voucher mới", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                Voucher v = new Voucher();
                v.setCode(txtCode.getText().trim().toUpperCase());
                v.setDiscountType((DiscountType) cbType.getSelectedItem());
                v.setDiscountVal(new BigDecimal(txtVal.getText().trim()));
                v.setMinOrderAmount(new BigDecimal(txtMinOrder.getText().trim()));
                String maxS = txtMaxDisc.getText().trim();
                if (!maxS.isBlank()) v.setMaxDiscountAmount(new BigDecimal(maxS));
                v.setStartDate(LocalDate.parse(txtStart.getText().trim()));
                v.setEndDate(LocalDate.parse(txtEnd.getText().trim()));
                v.setUsageLimit(Integer.parseInt(txtLimit.getText().trim()));
                v.setActive(true);

                voucherDAO.insert(v);
                JOptionPane.showMessageDialog(this, "Tạo Voucher thành công!");
                loadDataAsync();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void toggleActiveSelected() {
        Voucher v = getSelected();
        if (v == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn Voucher cần bật/tắt.");
            return;
        }
        v.setActive(!v.isActive());
        voucherDAO.update(v);
        loadDataAsync();
    }

    private void deleteSelected() {
        Voucher v = getSelected();
        if (v == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn Voucher cần xóa.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa Voucher " + v.getCode() + "?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            voucherDAO.delete(v.getId());
            loadDataAsync();
        }
    }
}
