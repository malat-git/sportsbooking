package com.college.sportsbooking.dao;

import com.college.sportsbooking.model.Student;
import java.sql.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class StudentDao {

    private Student map(ResultSet rs) throws SQLException {
        return new Student(rs.getInt("student_id"), rs.getString("student_name"),
                rs.getString("email"), rs.getString("contact_info"));
    }

    public int add(Connection c, Student s) throws SQLException {
        String sql = "INSERT INTO students(student_name,email,contact_info) VALUES(?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.studentName());
            ps.setString(2, s.email());
            ps.setString(3, s.contactInfo());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean existsByEmail(Connection c, String email) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM students WHERE email=?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public Student find(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM students WHERE student_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Student> findAll(Connection c) throws SQLException {
        List<Student> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM students");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }
}