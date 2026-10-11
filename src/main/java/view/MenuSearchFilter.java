package view;

import java.text.Normalizer;
import java.util.Locale;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public final class MenuSearchFilter {
    private final TableRowSorter<DefaultTableModel> sorter;
    private final int searchColumn;

    public MenuSearchFilter(DefaultTableModel model, int searchColumn) {
        this.sorter = new TableRowSorter<>(model);
        this.searchColumn = searchColumn;
    }

    public MenuSearchFilter(DefaultTableModel model) {
        this(model, 1);
    }

    public TableRowSorter<DefaultTableModel> getSorter() {
        return sorter;
    }

    public void bind(JTextField txtSearch) {
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(txtSearch.getText()); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(txtSearch.getText()); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(txtSearch.getText()); }
        });
    }

    public void applyFilter(String keyword) {
        final String key = normalize(keyword);
        if (key.isEmpty()) {
            sorter.setRowFilter(null);   
            return;
        }
        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(RowFilter.Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                Object value = entry.getValue(searchColumn);
                return value != null && normalize(value.toString()).contains(key);
            }
        });
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return t.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }
}
