package com.college.sportsbooking.dao;

import com.college.sportsbooking.model.Booking;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class BookingDao {

    private Booking map(ResultSet rs) throws SQLException {
        return new Booking(rs.getInt("booking_id"), rs.getInt("student_id"), rs.getInt("facility_id"),
                rs.getDate("booking_date").toLocalDate(),
                rs.getTime("start_time").toLocalTime(), rs.getTime("end_time").toLocalTime(),
                rs.getTimestamp("booking_time").toLocalDateTime(), rs.getString("status"));
    }

    // Overlap check: existing.start < new.end AND existing.end > new.start
    public boolean hasOverlap(Connection c, int facilityId, LocalDate date, LocalTime start, LocalTime end) throws SQLException {
        String sql = "SELECT COUNT(*) FROM sports_bookings WHERE facility_id=? AND booking_date=? "
                + "AND status='CONFIRMED' AND start_time < ? AND end_time > ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, facilityId);
            ps.setDate(2, java.sql.Date.valueOf(date));
            ps.setTime(3, Time.valueOf(end));
            ps.setTime(4, Time.valueOf(start));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public Booking insert(Connection c, int studentId, int facilityId, LocalDate date,
                          LocalTime start, LocalTime end) throws SQLException {
        String sql = "INSERT INTO sports_bookings(student_id,facility_id,booking_date,start_time,end_time,status) VALUES(?,?,?,?,?,'CONFIRMED')";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, studentId);
            ps.setInt(2, facilityId);
            ps.setDate(3, java.sql.Date.valueOf(date));
            ps.setTime(4, Time.valueOf(start));
            ps.setTime(5, Time.valueOf(end));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return find(c, rs.getInt(1), false);
            }
        }
    }

    public Booking find(Connection c, int id, boolean lock) throws SQLException {
        String sql = "SELECT * FROM sports_bookings WHERE booking_id=?" + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void updateStatus(Connection c, int id, String status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE sports_bookings SET status=? WHERE booking_id=?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    // Finished bookings -> COMPLETED
    public int markCompleted(Connection c) throws SQLException {
        String sql = "UPDATE sports_bookings SET status='COMPLETED' WHERE status='CONFIRMED' AND TIMESTAMP(booking_date,end_time) < NOW()";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    public List<Booking> findByStudent(Connection c, int studentId, String status) throws SQLException {
        String sql = "SELECT * FROM sports_bookings WHERE student_id=?" + (status != null ? " AND status=?" : "")
                + " ORDER BY booking_date DESC, start_time DESC";
        List<Booking> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            if (status != null) ps.setString(2, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<Booking> findByFacilityDate(Connection c, int facilityId, LocalDate date) throws SQLException {
        String sql = "SELECT * FROM sports_bookings WHERE facility_id=? AND booking_date=? ORDER BY start_time";
        List<Booking> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, facilityId);
            ps.setDate(2, java.sql.Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }
}