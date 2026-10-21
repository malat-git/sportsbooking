package com.college.sportsbooking.service;

import com.college.sportsbooking.dao.*;
import com.college.sportsbooking.exception.BookingException;
import com.college.sportsbooking.model.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    private final DataSource ds;
    private final StudentDao studentDao;
    private final FacilityDao facilityDao;
    private final BookingDao bookingDao;

    public BookingService(DataSource ds, StudentDao studentDao, FacilityDao facilityDao, BookingDao bookingDao) {
        this.ds = ds;
        this.studentDao = studentDao;
        this.facilityDao = facilityDao;
        this.bookingDao = bookingDao;
    }

    @FunctionalInterface
    private interface TxWork<T> { T run(Connection c) throws Exception; }

    // Every operation runs inside a JDBC transaction: commit on success, rollback on error
    private <T> T tx(TxWork<T> work) {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (BookingException e) {
                c.rollback();
                throw e;
            } catch (Exception e) {
                c.rollback();
                throw new RuntimeException(e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }

    // 1. Student Registration
    public Student registerStudent(Student s) {
        if (blank(s.studentName()) || blank(s.email()))
            throw new BookingException("Student name and email are required");
        return tx(c -> {
            if (studentDao.existsByEmail(c, s.email()))
                throw new BookingException("Email already registered");
            int id = studentDao.add(c, s);
            return new Student(id, s.studentName(), s.email(), s.contactInfo());
        });
    }

    public List<Student> getStudents() { return tx(studentDao::findAll); }

    // 2. Sports Facility Management
    public SportsFacility addFacility(SportsFacility f) {
        if (blank(f.facilityName()) || blank(f.sportType()) || f.openingTime() == null || f.closingTime() == null)
            throw new BookingException("Facility name, sport type, opening and closing time are required");
        if (!f.openingTime().isBefore(f.closingTime()))
            throw new BookingException("Opening time must be before closing time");
        String status = f.status() == null ? "AVAILABLE" : f.status().toUpperCase();
        if (!status.equals("AVAILABLE") && !status.equals("MAINTENANCE"))
            throw new BookingException("Status must be AVAILABLE or MAINTENANCE");
        return tx(c -> {
            int id = facilityDao.add(c, f, status);
            return new SportsFacility(id, f.facilityName(), f.sportType(), f.location(),
                    f.openingTime(), f.closingTime(), status);
        });
    }

    public List<SportsFacility> getFacilities() { return tx(facilityDao::findAll); }

    public SportsFacility updateFacilityStatus(int id, String status) {
        String st = status == null ? "" : status.toUpperCase();
        if (!st.equals("AVAILABLE") && !st.equals("MAINTENANCE"))
            throw new BookingException("Status must be AVAILABLE or MAINTENANCE");
        return tx(c -> {
            if (facilityDao.find(c, id, true) == null) throw new BookingException("Facility not found");
            facilityDao.updateStatus(c, id, st);
            return facilityDao.find(c, id, false);
        });
    }

    // 3 + 6. View Available Time Slots (calculated fresh from DB every time = real-time)
    public List<Slot> getSlots(int facilityId, LocalDate date) {
        if (date == null) throw new BookingException("Date is required");
        return tx(c -> {
            SportsFacility f = facilityDao.find(c, facilityId, false);
            if (f == null) throw new BookingException("Facility not found");
            if (!"AVAILABLE".equals(f.status())) throw new BookingException("Facility is under maintenance");
            List<Slot> slots = new ArrayList<>();
            LocalTime t = f.openingTime();
            while (true) {
                LocalTime end = t.plusHours(1);
                if (!end.isAfter(t) || end.isAfter(f.closingTime())) break;
                boolean past = LocalDateTime.of(date, t).isBefore(LocalDateTime.now());
                boolean free = !past && !bookingDao.hasOverlap(c, facilityId, date, t, end);
                slots.add(new Slot(t, end, free));
                t = end;
            }
            return slots;
        });
    }

    public List<Booking> getFacilitySchedule(int facilityId, LocalDate date) {
        return tx(c -> bookingDao.findByFacilityDate(c, facilityId, date));
    }

    // 4 + 5. Book facility + Prevent Double Booking
    public Booking bookFacility(BookingRequest r) {
        if (r.studentId() == null || r.facilityId() == null || r.bookingDate() == null
                || r.startTime() == null || r.endTime() == null)
            throw new BookingException("studentId, facilityId, bookingDate, startTime, endTime are required");
        if (!r.startTime().isBefore(r.endTime()))
            throw new BookingException("Start time must be before end time");

        return tx(c -> {
            if (studentDao.find(c, r.studentId()) == null)
                throw new BookingException("Student not found");
            // Lock the facility row so two people can't book the same slot at the same time
            SportsFacility f = facilityDao.find(c, r.facilityId(), true);
            if (f == null) throw new BookingException("Facility not found");
            if (!"AVAILABLE".equals(f.status()))
                throw new BookingException("Facility is under maintenance");
            if (r.startTime().isBefore(f.openingTime()) || r.endTime().isAfter(f.closingTime()))
                throw new BookingException("Booking must be within operating hours "
                        + f.openingTime() + " - " + f.closingTime());
            if (LocalDateTime.of(r.bookingDate(), r.startTime()).isBefore(LocalDateTime.now()))
                throw new BookingException("Cannot book a past time slot");
            if (bookingDao.hasOverlap(c, r.facilityId(), r.bookingDate(), r.startTime(), r.endTime()))
                throw new BookingException("Double booking! This slot overlaps with an existing booking");
            return bookingDao.insert(c, r.studentId(), r.facilityId(), r.bookingDate(), r.startTime(), r.endTime());
        });
    }

        // 7. Cancel booking (ownerStudentId = null means admin, can cancel any booking)
    public Booking cancelBooking(int bookingId) { return cancelOwnBooking(bookingId, null); }

    public Booking cancelOwnBooking(int bookingId, Integer ownerStudentId) {
        return tx(c -> {
            Booking b = bookingDao.find(c, bookingId, true);
            if (b == null) throw new BookingException("Booking not found");
            if (ownerStudentId != null && !ownerStudentId.equals(b.studentId()))
                throw new BookingException("You can cancel only your own bookings");
            if (!"CONFIRMED".equals(b.status()))
                throw new BookingException("Only CONFIRMED bookings can be cancelled (current status: " + b.status() + ")");
            if (LocalDateTime.of(b.bookingDate(), b.startTime()).isBefore(LocalDateTime.now()))
                throw new BookingException("Cannot cancel a booking that has already started");
            bookingDao.updateStatus(c, bookingId, "CANCELLED");
            return bookingDao.find(c, bookingId, false);
        });
    }

    // 7 + 8. View bookings / history (status = null -> all, CONFIRMED -> upcoming, COMPLETED -> completed)
    public List<Booking> getHistory(int studentId, String status) {
        return tx(c -> {
            if (studentDao.find(c, studentId) == null) throw new BookingException("Student not found");
            bookingDao.markCompleted(c);
            return bookingDao.findByStudent(c, studentId, status == null ? null : status.toUpperCase());
        });
    }
}