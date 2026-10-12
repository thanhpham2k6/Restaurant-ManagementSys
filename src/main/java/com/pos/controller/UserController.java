package com.pos.controller;

import com.pos.dao.UserDAO;
import com.pos.model.Role;
import com.pos.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

public class UserController {
    private static final Logger log = LogManager.getLogger(UserController.class);
    private final UserDAO userDAO = new UserDAO();

    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    public User registerUser(String username, String password, String fullName, Role role) {
        if (username == null || username.trim().length() < 3) {
            throw new RuntimeException("Tên đăng nhập phải có ít nhất 3 ký tự!");
        }
        if (password == null || password.trim().length() < 4) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 4 ký tự!");
        }
        if (fullName == null || fullName.trim().isBlank()) {
            throw new RuntimeException("Họ và tên không được để trống!");
        }
        if (role == null) {
            throw new RuntimeException("Vui lòng chọn vai trò (Manager / Cashier / Kitchen)!");
        }

        String trimmedUser = username.trim();
        if (userDAO.existsUsername(trimmedUser)) {
            throw new RuntimeException("Tên đăng nhập '" + trimmedUser + "' đã tồn tại trong hệ thống!");
        }

        String hash = BCrypt.hashpw(password.trim(), BCrypt.gensalt(10));
        User newUser = new User(0, trimmedUser, hash, fullName.trim(), role, true);
        int newId = userDAO.insert(newUser);
        if (newId <= 0) {
            throw new RuntimeException("Không thể tạo tài khoản vào CSDL!");
        }
        newUser.setId(newId);
        log.info("Manager đã tạo tài khoản mới: {} [{}]", trimmedUser, role);
        return newUser;
    }

    public void resetPassword(int userId, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 4) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 4 ký tự!");
        }
        String hash = BCrypt.hashpw(newPassword.trim(), BCrypt.gensalt(10));
        userDAO.updatePassword(userId, hash);
        log.info("Đã đổi mật khẩu cho user #{}", userId);
    }

    public int deleteUser(int userId, User currentLoggedInUser) {
        if (currentLoggedInUser != null && currentLoggedInUser.getId() == userId) {
            throw new RuntimeException("Không thể tự xóa tài khoản của chính mình đang đăng nhập!");
        }
        int result = userDAO.delete(userId);
        log.info("Xóa tài khoản user #{}, kết quả: {}", userId, result);
        return result;
    }

    public void toggleStatus(int userId, boolean currentActive, User currentLoggedInUser) {
        if (currentLoggedInUser != null && currentLoggedInUser.getId() == userId && currentActive) {
            throw new RuntimeException("Không thể tự khóa tài khoản của chính mình!");
        }
        if (currentActive) {
            userDAO.deactivate(userId);
            log.info("Đã vô hiệu hóa (khóa) user #{}", userId);
        } else {
            userDAO.activate(userId);
            log.info("Đã kích hoạt lại user #{}", userId);
        }
    }
}
