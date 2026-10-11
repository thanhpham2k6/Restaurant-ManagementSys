package com.nhom13.restaurant.managementsys.dao;

import com.nhom13.restaurant.managementsys.database.Database;
import com.nhom13.restaurant.managementsys.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM Users ORDER BY role, full_name";
        try (Connection c = Database.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next())
                users.add(new User(rs.getInt("user_id"), rs.getString("username"),
                    rs.getString("password_hash"), rs.getString("full_name"),
                    rs.getString("role"), rs.getTimestamp("created_at")));
        } catch (SQLException e) { e.printStackTrace(); }
        return users;
    }

    public void addUser(User user) {
        String sql = "INSERT INTO Users (username, password_hash, full_name, role) VALUES (?,?,?,?)";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, user.getUsername());
            p.setString(2, user.getPasswordHash());
            p.setString(3, user.getFullName());
            p.setString(4, user.getRole());
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void updateUser(User user) {
        // Cập nhật họ tên, vai trò (không đổi username)
        String sql = "UPDATE Users SET full_name=?, role=? WHERE user_id=?";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, user.getFullName());
            p.setString(2, user.getRole());
            p.setInt(3, user.getUserId());
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void resetPassword(int userId, String newPasswordHash) {
        String sql = "UPDATE Users SET password_hash=? WHERE user_id=?";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, newPasswordHash);
            p.setInt(2, userId);
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void deleteUser(int userId) {
        String sql = "DELETE FROM Users WHERE user_id=?";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, userId);
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
