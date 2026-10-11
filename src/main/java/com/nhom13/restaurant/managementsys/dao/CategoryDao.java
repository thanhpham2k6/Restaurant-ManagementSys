package com.nhom13.restaurant.managementsys.dao;

import com.nhom13.restaurant.managementsys.database.Database;
import com.nhom13.restaurant.managementsys.model.Category;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {

    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM Categories ORDER BY category_name";
        try (Connection c = Database.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next())
                list.add(new Category(rs.getInt("category_id"), rs.getString("category_name")));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void addCategory(Category category) {
        String sql = "INSERT INTO Categories (category_name) VALUES (?)";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, category.getCategoryName());
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void updateCategory(Category category) {
        String sql = "UPDATE Categories SET category_name=? WHERE category_id=?";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, category.getCategoryName());
            p.setInt(2, category.getCategoryId());
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void deleteCategory(int id) {
        // Chỉ xóa được nếu không còn món ăn nào thuộc danh mục này
        String sql = "DELETE FROM Categories WHERE category_id=?";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
