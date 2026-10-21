package com.college.sportsbooking.dao;

import java.sql.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class StatsDao {

    private int count(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public Map<String, Object> adminStats(Connection c) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("students", count(c, "SELECT COUNT(*) FROM students"));
        m.put("facilities", count(c, "SELECT COUNT(*) FROM sports_facilities"));
        m.put("availableFacilities", count(c, "SELECT COUNT(*) FROM sports_facilities WHERE status='AVAILABLE'"));
        m.put("totalBookings", count(c, "SELECT COUNT(*) FROM sports_bookings"));
        m.put("confirmed", count(c, "SELECT COUNT(*) FROM sports_bookings WHERE status='CONFIRMED'"));
        m.put("completed", count(c, "SELECT COUNT(*) FROM sports_bookings WHERE status='COMPLETED'"));
        m.put("cancelled", count(c, "SELECT COUNT(*) FROM sports_bookings WHERE status='CANCELLED'"));
        m.put("today", count(c, "SELECT COUNT(*) FROM sports_bookings WHERE booking_date=CURDATE() AND status<>'CANCELLED'"));

        List<Map<String, Object>> per = new ArrayList<>();
        String sql = "SELECT f.facility_name, COUNT(b.booking_id) AS cnt FROM sports_facilities f "
                + "LEFT JOIN sports_bookings b ON b.facility_id=f.facility_id AND b.status<>'CANCELLED' "
                + "GROUP BY f.facility_id, f.facility_name ORDER BY cnt DESC";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", rs.getString("facility_name"));
                row.put("count", rs.getInt("cnt"));
                per.add(row);
            }
        }
        m.put("perFacility", per);
        m.put("recent", views(c, null, false, 6));
        return m;
    }

    public Map<String, Object> studentStats(Connection c, int studentId) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("upcoming", count(c, "SELECT COUNT(*) FROM sports_bookings WHERE student_id=? AND status='CONFIRMED'", studentId));
        m.put("completed", count(c, "SELECT COUNT(*) FROM sports_bookings WHERE student_id=? AND status='COMPLETED'", studentId));
        m.put("cancelled", count(c, "SELECT COUNT(*) FROM sports_bookings WHERE student_id=? AND status='CANCELLED'", studentId));
        m.put("availableFacilities", count(c, "SELECT COUNT(*) FROM sports_facilities WHERE status='AVAILABLE'"));
        m.put("upcomingList", views(c, studentId, true, 5));
        return m;
    }

    private List<Map<String, Object>> views(Connection c, Integer studentId, boolean upcomingOnly, int limit) throws SQLException {
        String sql = "SELECT b.booking_id, s.student_name, f.facility_name, b.booking_date, b.start_time, b.end_time, b.status "
                + "FROM sports_bookings b JOIN students s ON s.student_id=b.student_id "
                + "JOIN sports_facilities f ON f.facility_id=b.facility_id WHERE 1=1"
                + (studentId != null ? " AND b.student_id=?" : "")
                + (upcomingOnly ? " AND b.status='CONFIRMED' ORDER BY b.booking_date, b.start_time"
                                : " ORDER BY b.booking_time DESC")
                + " LIMIT " + limit;
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (studentId != null) ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("bookingId", rs.getInt("booking_id"));
                    row.put("studentName", rs.getString("student_name"));
                    row.put("facilityName", rs.getString("facility_name"));
                    row.put("bookingDate", rs.getDate("booking_date").toString());
                    row.put("startTime", rs.getTime("start_time").toString());
                    row.put("endTime", rs.getTime("end_time").toString());
                    row.put("status", rs.getString("status"));
                    list.add(row);
                }
            }
        }
        return list;
    }
}