package com.nhom13.restaurant.managementsys.dao;

import com.nhom13.restaurant.managementsys.database.Database;
import com.nhom13.restaurant.managementsys.model.Ingredient;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class IngredientDao {

    private Ingredient mapRow(ResultSet rs) throws SQLException {
        return new Ingredient(
            rs.getInt("ingredient_id"),
            rs.getString("ingredient_name"),
            rs.getString("unit"),
            rs.getDouble("quantity"),
            rs.getDouble("min_quantity")
        );
    }

    public List<Ingredient> getAllIngredients() {
        List<Ingredient> list = new ArrayList<>();
        String sql = "SELECT * FROM Ingredients ORDER BY ingredient_name";
        try (Connection c = Database.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void addIngredient(Ingredient ing) {
        String sql = "INSERT INTO Ingredients (ingredient_name, unit, quantity, min_quantity) VALUES (?,?,?,?)";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, ing.getIngredientName());
            p.setString(2, ing.getUnit());
            p.setDouble(3, ing.getQuantity());
            p.setDouble(4, ing.getMinQuantity());
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Cập nhật thông tin nguyên liệu (tên, đơn vị, tồn kho tối thiểu) */
    public void updateIngredient(Ingredient ing) {
        String sql = "UPDATE Ingredients SET ingredient_name=?, unit=?, quantity=?, min_quantity=? WHERE ingredient_id=?";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, ing.getIngredientName());
            p.setString(2, ing.getUnit());
            p.setDouble(3, ing.getQuantity());
            p.setDouble(4, ing.getMinQuantity());
            p.setInt(5, ing.getIngredientId());
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Nhập thêm hàng: cộng delta vào tồn kho */
    public void addStock(int ingredientId, double delta) {
        String sql = "UPDATE Ingredients SET quantity = quantity + ? WHERE ingredient_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setDouble(1, delta);
            p.setInt(2, ingredientId);
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Xuất kho: trừ delta khỏi tồn kho (không để âm) */
    public void useStock(int ingredientId, double delta) {
        String sql = "UPDATE Ingredients SET quantity = CASE WHEN quantity >= ? THEN quantity - ? ELSE 0 END WHERE ingredient_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setDouble(1, delta);
            p.setDouble(2, delta);
            p.setInt(3, ingredientId);
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void deleteIngredient(int id) {
        String sql = "DELETE FROM Ingredients WHERE ingredient_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
