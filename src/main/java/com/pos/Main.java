package com.pos;

import com.formdev.flatlaf.FlatLightLaf;
import com.pos.view.LoginForm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;

public class Main {
    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        try {
            Font baseFont = new Font("Segoe UI", Font.PLAIN, 13);
            Font boldFont = new Font("Segoe UI", Font.BOLD, 13);
            UIManager.put("defaultFont", baseFont);
            UIManager.put("Button.font", baseFont);
            UIManager.put("Label.font", baseFont);
            UIManager.put("TextField.font", baseFont);
            UIManager.put("TextArea.font", baseFont);
            UIManager.put("Table.font", baseFont);
            UIManager.put("TableHeader.font", boldFont);
            UIManager.put("TitledBorder.font", boldFont);
            UIManager.put("OptionPane.messageFont", baseFont);
            UIManager.put("OptionPane.buttonFont", baseFont);
            FlatLightLaf.setup();
        } catch (Exception e) {
            log.warn("Cannot apply FlatLaf theme", e);
        }

        SwingUtilities.invokeLater(() -> {
            LoginForm loginForm = new LoginForm();
            loginForm.setVisible(true);
        });
        log.info("Hệ thống Restaurant POS đã khởi động");
    }
}
