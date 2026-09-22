package com.nhom13.restaurant.managementsys.view;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class StaffMainFrame extends JFrame {
    
    public StaffMainFrame() {
        initComponents();
    }
    
    private void initComponents() {
        setTitle("Restaurant Management System - Staff Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        JLabel welcomeLabel = new JLabel("Welcome Staff to Restaurant Management System", SwingConstants.CENTER);
        welcomeLabel.setFont(new java.awt.Font("Segoe UI", 1, 24));
        getContentPane().add(welcomeLabel);
    }
}
