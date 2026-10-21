package com.college.sportsbooking.service;

import com.college.sportsbooking.dao.*;
import com.college.sportsbooking.exception.BookingException;
import com.college.sportsbooking.model.*;
import jakarta.annotation.PostConstruct;
import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final DataSource ds;
    private final StudentDao studentDao;
    private final AccountDao accountDao;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();

    public AuthService(DataSource ds, StudentDao studentDao, AccountDao accountDao) {
        this.ds = ds;
        this.studentDao = studentDao;
        this.accountDao = accountDao;
    }

    // Default admin account (created once at startup)
    @PostConstruct
    void seedAdmin() {
        try (Connection c = ds.getConnection()) {
            if (accountDao.findByEmail(c, "admin@college.edu") == null)
                accountDao.insert(c, null, "admin@college.edu", encoder.encode("admin123"), "ADMIN");
        } catch (SQLException e) {
            throw new RuntimeException("Run the user_accounts table script first: " + e.getMessage(), e);
        }
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }

    private AuthSession startSession(Integer studentId, String name, String role) {
        String token = UUID.randomUUID().toString();
        AuthSession s = new AuthSession(token, studentId, name, role);
        sessions.put(token, s);
        return s;
    }

    public AuthSession signup(SignupRequest r) {
        if (blank(r.studentName()) || blank(r.email()) || blank(r.password()))
            throw new BookingException("Name, email and password are required");
        if (!r.email().contains("@")) throw new BookingException("Enter a valid email address");
        if (r.password().length() < 6) throw new BookingException("Password must be at least 6 characters");
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (studentDao.existsByEmail(c, r.email().trim()))
                    throw new BookingException("Email already registered");
                int id = studentDao.add(c, new Student(null, r.studentName().trim(), r.email().trim(), r.contactInfo()));
                accountDao.insert(c, id, r.email().trim(), encoder.encode(r.password()), "STUDENT");
                c.commit();
                return startSession(id, r.studentName().trim(), "STUDENT");
            } catch (Exception e) {
                c.rollback();
                if (e instanceof BookingException be) throw be;
                throw new RuntimeException(e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public AuthSession login(LoginRequest r) {
        if (blank(r.email()) || blank(r.password())) throw new BookingException("Email and password are required");
        try (Connection c = ds.getConnection()) {
            UserAccount a = accountDao.findByEmail(c, r.email().trim());
            if (a == null || !encoder.matches(r.password(), a.passwordHash()))
                throw new BookingException("Invalid email or password");
            String name = "Administrator";
            if (a.studentId() != null) name = studentDao.find(c, a.studentId()).studentName();
            return startSession(a.studentId(), name, a.role());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public AuthSession get(String token) { return token == null ? null : sessions.get(token); }

    public void logout(String token) { if (token != null) sessions.remove(token); }
}