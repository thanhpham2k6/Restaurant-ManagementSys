package com.pos.dao;

import com.pos.model.Customer;
import com.pos.model.MembershipTier;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {
    private static final Logger log = LogManager.getLogger(CustomerDAO.class);

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        c.setAddress(rs.getString("address"));
        c.setLoyaltyPoints(rs.getInt("loyalty_points"));
        c.setTotalSpent(rs.getBigDecimal("total_spent"));
        c.setMembershipTier(MembershipTier.valueOf(rs.getString("membership_tier")));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) c.setCreatedAt(ts.toLocalDateTime());
        return c;
    }

    public List<Customer> findAll() {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers ORDER BY total_spent DESC";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { log.error("findAll customers error", e); }
        return list;
    }

    public Customer findById(int id) {
        String sql = "SELECT * FROM customers WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { log.error("findById customer error", e); }
        return null;
    }

    public Customer findByPhone(String phone) {
        String sql = "SELECT * FROM customers WHERE phone = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone != null ? phone.trim() : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { log.error("findByPhone customer error", e); }
        return null;
    }

    public List<Customer> search(String keyword) {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers WHERE name LIKE ? OR phone LIKE ? ORDER BY name";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { log.error("search customer error", e); }
        return list;
    }

    public int insert(Customer customer) {
        String sql = "INSERT INTO customers (name, phone, email, address, loyalty_points, total_spent, membership_tier) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customer.getName());
            ps.setString(2, customer.getPhone());
            ps.setString(3, customer.getEmail());
            ps.setString(4, customer.getAddress());
            ps.setInt(5, customer.getLoyaltyPoints());
            ps.setBigDecimal(6, customer.getTotalSpent() != null ? customer.getTotalSpent() : BigDecimal.ZERO);
            ps.setString(7, customer.getMembershipTier() != null ? customer.getMembershipTier().name() : MembershipTier.STANDARD.name());
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    customer.setId(gk.getInt(1));
                    return customer.getId();
                }
            }
        } catch (SQLException e) { log.error("insert customer error", e); }
        return -1;
    }

    public void update(Customer customer) {
        String sql = "UPDATE customers SET name = ?, phone = ?, email = ?, address = ?, " +
                     "loyalty_points = ?, total_spent = ?, membership_tier = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, customer.getName());
            ps.setString(2, customer.getPhone());
            ps.setString(3, customer.getEmail());
            ps.setString(4, customer.getAddress());
            ps.setInt(5, customer.getLoyaltyPoints());
            ps.setBigDecimal(6, customer.getTotalSpent());
            ps.setString(7, customer.getMembershipTier().name());
            ps.setInt(8, customer.getId());
            ps.executeUpdate();
        } catch (SQLException e) { log.error("update customer error", e); }
    }

    public void addSpentAndPoints(int customerId, BigDecimal spentAmount, int pointsEarned, int pointsUsed) {
        String sql = "UPDATE customers SET total_spent = total_spent + ?, " +
                     "loyalty_points = loyalty_points + ? - ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, spentAmount);
            ps.setInt(2, pointsEarned);
            ps.setInt(3, pointsUsed);
            ps.setInt(4, customerId);
            ps.executeUpdate();

            // Auto-update tier
            Customer updated = findById(customerId);
            if (updated != null) {
                MembershipTier newTier = MembershipTier.fromTotalSpent(updated.getTotalSpent().longValue());
                if (newTier != updated.getMembershipTier()) {
                    updateTier(customerId, newTier);
                }
            }
        } catch (SQLException e) { log.error("addSpentAndPoints error", e); }
    }

    private void updateTier(int customerId, MembershipTier tier) {
        String sql = "UPDATE customers SET membership_tier = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tier.name());
            ps.setInt(2, customerId);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("updateTier error", e); }
    }

    public void delete(int id) {
        String sql = "DELETE FROM customers WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("delete customer error", e); }
    }
}
