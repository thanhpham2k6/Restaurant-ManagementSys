package view.chart;

import java.awt.BorderLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

public final class RevenueChartRenderer {

    public enum ChartType {
        BAR, LINE, PIE
    }

    public static void show(JPanel host, Map<LocalDate, BigDecimal> revenue, ChartType type, String title) {
        JFreeChart chart = create(revenue, type, title);

        host.removeAll();
        host.setLayout(new BorderLayout());
        host.add(new ChartPanel(chart), BorderLayout.CENTER);
        host.revalidate();
        host.repaint();
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
}