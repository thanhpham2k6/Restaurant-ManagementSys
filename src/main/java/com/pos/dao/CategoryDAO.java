package com.pos.dao;

import com.pos.model.Category;
import com.pos.model.CategoryType;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {
    private static final Logger log = LogManager.getLogger(CategoryDAO.class);

    private Category mapRow(ResultSet rs) throws SQLException {
        return new Category(
            rs.getInt("id"),
            com.pos.utils.DatabaseEncodingFixer.fixMojibake(rs.getString("name")),
            CategoryType.valueOf(rs.getString("type"))
        );
    }

    public List<Category> findAll() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM categories ORDER BY name";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { log.error("findAll categories error", e); }
        return list;
    }

    public int insert(Category cat) {
        String sql = "INSERT INTO categories (name, type) VALUES (?,?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cat.getName());
            ps.setString(2, cat.getType().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) { log.error("insert category error", e); }
        return -1;
    }

    public void update(Category cat) {
        String sql = "UPDATE categories SET name=?, type=? WHERE id=?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cat.getName());
            ps.setString(2, cat.getType().name());
            ps.setInt(3, cat.getId());
            ps.executeUpdate();
        } catch (SQLException e) { log.error("update category error", e); }
    }

    public void delete(int id) {
        String sql = "DELETE FROM categories WHERE id=?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("delete category error", e); }
    }
}
