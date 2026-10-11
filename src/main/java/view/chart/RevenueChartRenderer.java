package view.chart;

import java.awt.BorderLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

public final class RevenueChartRenderer {

    private static final String EMPTY_MESSAGE = "Không có dữ liệu trong khoảng thời gian đã chọn";

    private RevenueChartRenderer() { }

    public enum ChartType {
        BAR, LINE, PIE
    }

    public static void show(JPanel host, Map<LocalDate, BigDecimal> revenue, ChartType type, String title) {
        if (revenue == null || revenue.isEmpty()) {
            showMessage(host, EMPTY_MESSAGE);
            return;
        }
        display(host, create(revenue, type, title));
    }

    public static void showTopItems(JPanel host, Map<String, BigDecimal> revenueByItem,
                                    int topN, ChartType type, String title) {
        Map<String, BigDecimal> top = topItems(revenueByItem, topN);
        if (top.isEmpty()) {
            showMessage(host, EMPTY_MESSAGE);
            return;
        }

        JFreeChart chart;
        if (type == ChartType.PIE) {
            DefaultPieDataset<String> data = new DefaultPieDataset<>();
            top.forEach((name, amount) -> data.setValue(name, amount));
            chart = ChartFactory.createPieChart(title, data, true, true, false);
        } else {
            DefaultCategoryDataset data = new DefaultCategoryDataset();
            top.forEach((name, amount) -> data.addValue(amount, "Doanh thu", name));
            chart = (type == ChartType.LINE)
                    ? ChartFactory.createLineChart(title, "Món", "VNĐ", data)
                    : ChartFactory.createBarChart(title, "Món", "VNĐ", data);
        }
        display(host, chart);
    }

    public static Map<String, BigDecimal> topItems(Map<String, BigDecimal> byItem, int topN) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        if (byItem == null || byItem.isEmpty() || topN <= 0) {
            return result;
        }
        List<Map.Entry<String, BigDecimal>> entries = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : byItem.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                entries.add(e);
            }
        }
        entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        BigDecimal other = BigDecimal.ZERO;
        for (int i = 0; i < entries.size(); i++) {
            if (i < topN) {
                result.put(entries.get(i).getKey(), entries.get(i).getValue());
            } else {
                other = other.add(entries.get(i).getValue());
            }
        }
        if (other.signum() > 0) {
            result.merge("Khác", other, BigDecimal::add);
        }
        return result;
    }

    private static JFreeChart create(Map<LocalDate, BigDecimal> revenue, ChartType type, String title) {
        Map<LocalDate, BigDecimal> sortedRevenue = new TreeMap<>(revenue);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        if (type == ChartType.PIE) {
            DefaultPieDataset<String> data = new DefaultPieDataset<>();
            sortedRevenue.forEach((date, amount) ->
                data.setValue(date.format(formatter), amount)
            );
            return ChartFactory.createPieChart(title, data, true, true, false);
        }

        DefaultCategoryDataset data = new DefaultCategoryDataset();
        sortedRevenue.forEach((date, amount) ->
            data.addValue(amount, "Doanh thu", date.format(formatter))
        );

        if (type == ChartType.LINE) {
            return ChartFactory.createLineChart(title, "Ngày", "VNĐ", data);
        }

        return ChartFactory.createBarChart(title, "Ngày", "VNĐ", data);
    }

    private static void display(JPanel host, JFreeChart chart) {
        host.removeAll();
        host.setLayout(new BorderLayout());
        host.add(new ChartPanel(chart), BorderLayout.CENTER);
        host.revalidate();
        host.repaint();
    }

    private static void showMessage(JPanel host, String message) {
        host.removeAll();
        host.setLayout(new BorderLayout());
        host.add(new JLabel(message, SwingConstants.CENTER), BorderLayout.CENTER);
        host.revalidate();
        host.repaint();
    }
}
