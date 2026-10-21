package com.college.sportsbooking.dao;

import com.college.sportsbooking.model.SportsFacility;
import java.sql.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class FacilityDao {

    private SportsFacility map(ResultSet rs) throws SQLException {
        return new SportsFacility(rs.getInt("facility_id"), rs.getString("facility_name"),
                rs.getString("sport_type"), rs.getString("location"),
                rs.getTime("opening_time").toLocalTime(), rs.getTime("closing_time").toLocalTime(),
                rs.getString("status"));
    }

    public int add(Connection c, SportsFacility f, String status) throws SQLException {
        String sql = "INSERT INTO sports_facilities(facility_name,sport_type,location,opening_time,closing_time,status) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, f.facilityName());
            ps.setString(2, f.sportType());
            ps.setString(3, f.location());
            ps.setTime(4, Time.valueOf(f.openingTime()));
            ps.setTime(5, Time.valueOf(f.closingTime()));
            ps.setString(6, status);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // lock=true -> row lock (SELECT ... FOR UPDATE) used while booking
    public SportsFacility find(Connection c, int id, boolean lock) throws SQLException {
        String sql = "SELECT * FROM sports_facilities WHERE facility_id=?" + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<SportsFacility> findAll(Connection c) throws SQLException {
        List<SportsFacility> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM sports_facilities");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public void updateStatus(Connection c, int id, String status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE sports_facilities SET status=? WHERE facility_id=?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
}