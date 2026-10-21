package com.college.sportsbooking.service;

import com.college.sportsbooking.dao.*;
import java.sql.*;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final DataSource ds;
    private final StatsDao stats;
    private final BookingDao bookingDao;

    public DashboardService(DataSource ds, StatsDao stats, BookingDao bookingDao) {
        this.ds = ds;
        this.stats = stats;
        this.bookingDao = bookingDao;
    }

    // studentId == null -> admin overview
    public Map<String, Object> get(Integer studentId) {
        try (Connection c = ds.getConnection()) {
            bookingDao.markCompleted(c);
            return studentId == null ? stats.adminStats(c) : stats.studentStats(c, studentId);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}