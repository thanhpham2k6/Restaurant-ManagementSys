package com.pos.controller;

import com.pos.dao.CustomerDAO;
import com.pos.model.Customer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;

public class CustomerController {
    private static final Logger log = LogManager.getLogger(CustomerController.class);
    private final CustomerDAO customerDAO;

    public CustomerController() {
        this.customerDAO = new CustomerDAO();
    }

    public List<Customer> getAllCustomers() {
        return customerDAO.findAll();
    }

    public List<Customer> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return customerDAO.findAll();
        }
        return customerDAO.search(keyword.trim());
    }

    public Customer findByPhone(String phone) {
        return customerDAO.findByPhone(phone);
    }

    public Customer findById(int id) {
        return customerDAO.findById(id);
    }

    public int createCustomer(String name, String phone, String email, String address) throws Exception {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Tên khách hàng không được để trống.");
        if (phone == null || phone.isBlank()) throw new IllegalArgumentException("Số điện thoại không được để trống.");

        Customer existing = customerDAO.findByPhone(phone);
        if (existing != null) {
            throw new IllegalArgumentException("Số điện thoại " + phone + " đã tồn tại trong hệ thống.");
        }

        Customer c = new Customer();
        c.setName(name.trim());
        c.setPhone(phone.trim());
        c.setEmail(email != null && !email.isBlank() ? email.trim() : null);
        c.setAddress(address != null && !address.isBlank() ? address.trim() : null);
        return customerDAO.insert(c);
    }

    public void updateCustomer(Customer customer) {
        if (customer != null) {
            // Tự động tính toán lại hạng thành viên theo điểm tích lũy hiện có (nâng hạng hoặc hạ hạng)
            customer.setMembershipTier(com.pos.model.MembershipTier.fromPoints(customer.getLoyaltyPoints()));
            customerDAO.update(customer);
        }
    }

    public void deleteCustomer(int id) {
        customerDAO.delete(id);
    }

    public void processOrderLoyalty(int customerId, BigDecimal orderTotal, int pointsUsed) {
        // Tích điểm: 1000đ = 1 điểm (0.1%)
        int pointsEarned = orderTotal.divide(new BigDecimal("1000"), 0, java.math.RoundingMode.DOWN).intValue();
        customerDAO.addSpentAndPoints(customerId, orderTotal, pointsEarned, pointsUsed);
    }
}
