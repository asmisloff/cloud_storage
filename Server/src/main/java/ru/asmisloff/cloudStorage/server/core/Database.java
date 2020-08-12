package ru.asmisloff.cloudStorage.server.core;

import java.sql.*;

public class Database {

    private static final String DB_PATH = "./users.db";
    private static Connection conn = null;

    public static void connect() {
        String url = "jdbc:sqlite:" + DB_PATH;
        try {
            conn = DriverManager.getConnection(url);
            System.out.println("Connection to SQLite has been established.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
    }

    public static boolean checkLoginAndPassword(String login, String pwd) {
        if (conn == null) return false;

        String req = "SELECT login_fld FROM user_tbl WHERE login_fld = ? AND password_fld = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(req);
            ps.setString(1, login);
            ps.setString(2, pwd);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        connect();
        System.out.println(checkLoginAndPassword("user2", "pwd1"));
    }
}
