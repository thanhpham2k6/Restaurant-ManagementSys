
package com.nhom13.restaurant.managementsys.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=RestaurantManagementSys;encrypt=true;trustServerCertificate=true;";
    private static final String USER = "sa"; 
    private static final String PASS = "12345";
    private static Connection conn;

    public static Connection getConnection() {
        try {
            if (conn == null || conn.isClosed()) {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                
                conn = DriverManager.getConnection(URL, USER, PASS);
                System.out.println(">>> Đã kết nối SQL Server thành công (Localhost)!");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Lỗi: Không tìm thấy thư viện JDBC. Hãy kiểm tra lại file pom.xml");
        } catch (SQLException e) {
            System.err.println("Lỗi kết nối CSDL: " + e.getMessage());
        }
        return conn;
    }
    public static void main(String[] args) {
        System.out.println("Đang test kết nối đến SQL Server...");
        java.sql.Connection testConn = getConnection();
        
        if (testConn != null) {
            System.out.println("Đã kết nối thành công!");
        } else {
            System.out.println("Kết nối thất bại. Hãy kiểm tra lại SQL Server.");
        }
    }
}