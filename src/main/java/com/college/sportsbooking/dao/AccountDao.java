package com.college.sportsbooking.dao;

import com.college.sportsbooking.model.UserAccount;
import java.sql.*;
import org.springframework.stereotype.Repository;

@Repository
public class AccountDao {

    public UserAccount findByEmail(Connection c, String email) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM user_accounts WHERE email=?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                int sid = rs.getInt("student_id");
                Integer studentId = rs.wasNull() ? null : sid;
                return new UserAccount(rs.getInt("account_id"), studentId, rs.getString("email"),
                        rs.getString("password_hash"), rs.getString("role"));
            }
        }
    }

    public void insert(Connection c, Integer studentId, String email, String hash, String role) throws SQLException {
        String sql = "INSERT INTO user_accounts(student_id,email,password_hash,role) VALUES(?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, studentId, Types.INTEGER);
            ps.setString(2, email);
            ps.setString(3, hash);
            ps.setString(4, role);
            ps.executeUpdate();
        }
    }
}