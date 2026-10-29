package com.pos.view;

import com.pos.controller.OrderObserver;
import com.pos.controller.ReportController;
import com.pos.dao.TableDAO;
import com.pos.dao.UserDAO;
import com.pos.model.*;
import com.pos.utils.CurrencyFormat;
import com.pos.utils.DateUtils;
import com.pos.utils.TableUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

public class ReportPanel extends JPanel implements OrderObserver {
    private final User currentUser;
    private final ReportController reportController = new ReportController();
    private final TableDAO tableDAO = new TableDAO();
    private final UserDAO userDAO = new UserDAO();

    private ChartPanel chartPanel;
    private DefaultTableModel orderTableModel;
    private JTable orderTable;
    private List<Order> currentLoadedOrders;

    // Recent Invoice Files Table
    private DefaultTableModel invoiceFileModel;
    private JTable invoiceFileTable;
    private List<File> currentInvoiceFiles = new ArrayList<>();

    public ReportPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(0, 10));
        setBackground(new Color(245, 247, 250));
        setBorder(new EmptyBorder(12, 16, 14, 16));
        initUI();
        loadChart();
        loadRecentOrders();
        loadRecentInvoiceFiles();
    }

    private void initUI() {
        // Tiêu đề trang
        JLabel title = new JLabel("BÁO CÁO THỐNG KÊ DOANH THU & LỊCH SỬ BÁN HÀNG");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(30, 41, 59));
        title.setBorder(new EmptyBorder(0, 4, 6, 0));
        add(title, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildChartPanel(), buildBottomTabbedPane());
        split.setDividerLocation(300);
        split.setResizeWeight(0.42);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildChartPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            "Doanh thu 7 ngày gần nhất", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 13), new Color(30, 41, 59)));

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        JFreeChart chart = ChartFactory.createBarChart(
            "Doanh thu 7 ngày gần nhất", "Ngày", "Doanh thu (VNĐ)", dataset);
        styleChart(chart);

        chartPanel = new ChartPanel(chart);
        chartPanel.setBackground(Color.WHITE);
        chartPanel.setPreferredSize(new Dimension(0, 250));
        panel.add(chartPanel, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        bottomBar.setBackground(Color.WHITE);
        JButton btnRefresh = new JButton("Làm mới biểu đồ");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.setBackground(new Color(241, 245, 249));
        btnRefresh.setForeground(new Color(30, 41, 59));
        btnRefresh.setFocusPainted(false);
        btnRefresh.addActionListener(e -> loadChart());
        bottomBar.add(btnRefresh);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    private void styleChart(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);

        TextTitle title = chart.getTitle();
        if (title != null) {
            title.setPaint(new Color(30, 41, 59));
            title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        }

        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(Color.WHITE);
            chart.getLegend().setItemFont(new Font("Segoe UI", Font.PLAIN, 12));
            chart.getLegend().setItemPaint(new Color(71, 85, 105));
        }

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(248, 250, 252));
        plot.setOutlinePaint(new Color(226, 232, 240));
        plot.setRangeGridlinePaint(new Color(226, 232, 240));
        plot.setDomainGridlinePaint(new Color(241, 245, 249));

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setLabelFont(new Font("Segoe UI", Font.BOLD, 12));
        domainAxis.setLabelPaint(new Color(71, 85, 105));
        domainAxis.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
        domainAxis.setTickLabelPaint(new Color(71, 85, 105));
        domainAxis.setAxisLinePaint(new Color(203, 213, 225));

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setLabelFont(new Font("Segoe UI", Font.BOLD, 12));
        rangeAxis.setLabelPaint(new Color(71, 85, 105));
        rangeAxis.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
        rangeAxis.setTickLabelPaint(new Color(71, 85, 105));
        rangeAxis.setAxisLinePaint(new Color(203, 213, 225));

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(37, 99, 235)); // Royal blue #2563EB
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);
    }

    private JTabbedPane buildBottomTabbedPane() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.addTab("Lịch sử đơn hàng đã thanh toán", buildOrdersPanel());
        tabs.addTab("Tệp hóa đơn PDF gần đây", buildInvoicesPanel());
        return tabs;
    }

    private JPanel buildOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        orderTableModel = new DefaultTableModel(
            new Object[]{"Mã đơn", "Bàn", "Thu ngân", "Tổng tiền", "Chiết khấu", "Phương thức", "Thời gian thanh toán"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        orderTable = new JTable(orderTableModel);
        orderTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        orderTable.setRowHeight(30);
        orderTable.setShowGrid(true);
        orderTable.setGridColor(new Color(241, 245, 249));
        orderTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        orderTable.getTableHeader().setBackground(new Color(241, 245, 249));
        orderTable.getTableHeader().setForeground(new Color(51, 65, 85));
        orderTable.setSelectionBackground(new Color(237, 242, 247));
        orderTable.setSelectionForeground(new Color(15, 23, 42));
        TableUtils.applySmartSorting(orderTable);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        orderTable.setDefaultRenderer(Object.class, centerRenderer);

        panel.add(new JScrollPane(orderTable), BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        bottomBar.setBackground(Color.WHITE);

        JButton btnExportPdf = new JButton("Xuất lại hóa đơn PDF");
        btnExportPdf.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnExportPdf.setBackground(new Color(241, 245, 249));
        btnExportPdf.setForeground(new Color(30, 41, 59));
        btnExportPdf.setFocusPainted(false);
        btnExportPdf.addActionListener(e -> exportSelectedOrderPdf());

        JButton btnLoad = new JButton("Tải danh sách đơn gần đây");
        btnLoad.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLoad.setBackground(new Color(241, 245, 249));
        btnLoad.setForeground(new Color(30, 41, 59));
        btnLoad.setFocusPainted(false);
        btnLoad.addActionListener(e -> loadRecentOrders());

        bottomBar.add(btnExportPdf);
        bottomBar.add(btnLoad);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildInvoicesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        invoiceFileModel = new DefaultTableModel(
            new Object[]{"Mã đơn", "Tên tệp hóa đơn", "Thời gian xuất file", "Dung lượng", "Đường dẫn file"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        invoiceFileTable = new JTable(invoiceFileModel);
        invoiceFileTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        invoiceFileTable.setRowHeight(30);
        invoiceFileTable.setShowGrid(true);
        invoiceFileTable.setGridColor(new Color(241, 245, 249));
        invoiceFileTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        invoiceFileTable.getTableHeader().setBackground(new Color(241, 245, 249));
        invoiceFileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableUtils.applySmartSorting(invoiceFileTable);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        invoiceFileTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        invoiceFileTable.getColumnModel().getColumn(0).setCellRenderer(center);
        invoiceFileTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        invoiceFileTable.getColumnModel().getColumn(2).setCellRenderer(center);
        invoiceFileTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        invoiceFileTable.getColumnModel().getColumn(3).setCellRenderer(center);

        invoiceFileTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedInvoicePdf();
                }
            }
        });

        panel.add(new JScrollPane(invoiceFileTable), BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        bottomBar.setBackground(Color.WHITE);

        JButton btnOpenPdf = new JButton("📂 Mở xem file PDF");
        btnOpenPdf.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnOpenPdf.setBackground(new Color(37, 99, 235));
        btnOpenPdf.setForeground(Color.WHITE);
        btnOpenPdf.setFocusPainted(false);
        btnOpenPdf.addActionListener(e -> openSelectedInvoicePdf());

        JButton btnOpenFolder = new JButton("📁 Mở thư mục Invoices");
        btnOpenFolder.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnOpenFolder.setBackground(new Color(241, 245, 249));
        btnOpenFolder.setForeground(new Color(30, 41, 59));
        btnOpenFolder.setFocusPainted(false);
        btnOpenFolder.addActionListener(e -> openInvoicesDirectory());

        JButton btnDeleteFile = new JButton("🗑️ Xóa file");
        btnDeleteFile.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDeleteFile.setBackground(new Color(239, 68, 68));
        btnDeleteFile.setForeground(Color.WHITE);
        btnDeleteFile.setFocusPainted(false);
        btnDeleteFile.addActionListener(e -> deleteSelectedInvoiceFile());

        JButton btnRefreshFiles = new JButton("Làm mới danh sách");
        btnRefreshFiles.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefreshFiles.setBackground(new Color(241, 245, 249));
        btnRefreshFiles.setForeground(new Color(30, 41, 59));
        btnRefreshFiles.setFocusPainted(false);
        btnRefreshFiles.addActionListener(e -> loadRecentInvoiceFiles());

        bottomBar.add(btnOpenPdf);
        bottomBar.add(btnOpenFolder);
        bottomBar.add(btnDeleteFile);
        bottomBar.add(btnRefreshFiles);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    private void loadRecentInvoiceFiles() {
        if (invoiceFileModel == null) return;
        invoiceFileModel.setRowCount(0);
        File dir = new File("invoices");
        if (!dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
        if (files == null || files.length == 0) return;

        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        currentInvoiceFiles = Arrays.asList(files);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        for (File f : files) {
            String name = f.getName();
            String orderId = "-";
            if (name.startsWith("invoice_")) {
                String[] parts = name.split("_");
                if (parts.length >= 2) orderId = "#" + parts[1];
            }
            long kb = Math.max(1, f.length() / 1024);
            String modTime = sdf.format(new Date(f.lastModified()));
            invoiceFileModel.addRow(new Object[]{
                orderId,
                name,
                modTime,
                kb + " KB",
                f.getAbsolutePath()
            });
        }
    }

    private void openSelectedInvoicePdf() {
        int viewRow = invoiceFileTable.getSelectedRow();
        if (viewRow < 0 || currentInvoiceFiles == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 tệp hóa đơn trong danh sách!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int row = invoiceFileTable.convertRowIndexToModel(viewRow);
        if (row < 0 || row >= currentInvoiceFiles.size()) {
            return;
        }
        File file = currentInvoiceFiles.get(row);
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
            } else {
                JOptionPane.showMessageDialog(this, "Hệ điều hành không hỗ trợ mở tự động. Đường dẫn tệp tin:\n" + file.getAbsolutePath());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi khi mở file: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openInvoicesDirectory() {
        File dir = new File("invoices");
        if (!dir.exists()) dir.mkdirs();
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(dir);
            } else {
                JOptionPane.showMessageDialog(this, "Thư mục hóa đơn: " + dir.getAbsolutePath());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi mở thư mục: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedInvoiceFile() {
        int viewRow = invoiceFileTable.getSelectedRow();
        if (viewRow < 0 || currentInvoiceFiles == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 tệp hóa đơn cần xóa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int row = invoiceFileTable.convertRowIndexToModel(viewRow);
        if (row < 0 || row >= currentInvoiceFiles.size()) {
            return;
        }
        File file = currentInvoiceFiles.get(row);
        int opt = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa file hóa đơn: " + file.getName() + "?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            boolean ok = file.delete();
            if (ok) {
                JOptionPane.showMessageDialog(this, "Đã xóa file hóa đơn thành công!");
                loadRecentInvoiceFiles();
            } else {
                JOptionPane.showMessageDialog(this, "Không thể xóa file hóa đơn!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadChart() {
        try {
            Map<String, BigDecimal> data = reportController.revenueChart();
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (Map.Entry<String, BigDecimal> entry : data.entrySet()) {
                dataset.addValue(entry.getValue(), "Doanh thu", entry.getKey());
            }
            JFreeChart chart = ChartFactory.createBarChart(
                "Doanh thu 7 ngày gần nhất", "Ngày", "Doanh thu (VNĐ)", dataset);
            styleChart(chart);
            chartPanel.setChart(chart);
        } catch (Exception ignored) {}
    }

    private void loadRecentOrders() {
        orderTableModel.setRowCount(0);
        try {
            LocalDateTime from = LocalDate.now().minusDays(30).atStartOfDay();
            LocalDateTime to = LocalDate.now().plusDays(1).atStartOfDay();
            currentLoadedOrders = reportController.getOrdersByDateRange(from, to);

            List<DiningTable> tables = tableDAO.findAll();
            List<User> users = userDAO.findAll();

            for (Order o : currentLoadedOrders) {
                String tableName = tables.stream()
                    .filter(t -> o.getTable() != null && t.getId() == o.getTable().getId())
                    .map(DiningTable::getName).findFirst().orElse("Bàn #" + (o.getTable() != null ? o.getTable().getId() : "-"));

                String cashierName = users.stream()
                    .filter(u -> o.getCashier() != null && u.getId() == o.getCashier().getId())
                    .map(User::getFullName).findFirst().orElse("User #" + (o.getCashier() != null ? o.getCashier().getId() : "-"));

                orderTableModel.addRow(new Object[]{
                    "#" + o.getId(),
                    tableName,
                    cashierName,
                    CurrencyFormat.format(o.getTotalAmount()),
                    o.getDiscountType() != null ? o.getDiscountType().name() : "-",
                    o.getPaymentMethod() != null ? o.getPaymentMethod().name() : "-",
                    DateUtils.format(o.getPaidAt())
                });
            }
        } catch (Exception ignored) {}
    }

    private void exportSelectedOrderPdf() {
        int viewRow = orderTable.getSelectedRow();
        if (viewRow < 0 || currentLoadedOrders == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 đơn hàng trong danh sách!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int row = orderTable.convertRowIndexToModel(viewRow);
        if (row < 0 || row >= currentLoadedOrders.size()) {
            return;
        }

        Order order = currentLoadedOrders.get(row);
        com.pos.dao.OrderDAO orderDAO = new com.pos.dao.OrderDAO();
        order.setDetails(orderDAO.findDetailsByOrder(order.getId()));
        if (order.getTable() != null && order.getTable().getName() == null) {
            tableDAO.findAll().stream().filter(t -> t.getId() == order.getTable().getId()).findFirst().ifPresent(order::setTable);
        }
        if (order.getCashier() != null && order.getCashier().getFullName() == null) {
            userDAO.findAll().stream().filter(u -> u.getId() == order.getCashier().getId()).findFirst().ifPresent(order::setCashier);
        }

        String path = reportController.exportInvoice(order);
        if (path != null) {
            JOptionPane.showMessageDialog(this, "Đã xuất hóa đơn PDF thành công:\n" + path, "Thành công", JOptionPane.INFORMATION_MESSAGE);
            loadRecentInvoiceFiles();
        } else {
            JOptionPane.showMessageDialog(this, "Không thể xuất hóa đơn PDF!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void onOrderChanged(OrderEvent event) {
        if (event.getType() == OrderEvent.Type.ORDER_PAID) {
            SwingUtilities.invokeLater(() -> {
                loadChart();
                loadRecentOrders();
                loadRecentInvoiceFiles();
            });
        }
    }
}
