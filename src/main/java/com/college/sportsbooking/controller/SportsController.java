package com.college.sportsbooking.controller;

import com.college.sportsbooking.model.*;
import com.college.sportsbooking.service.BookingService;
import com.college.sportsbooking.service.DashboardService;
import java.time.LocalDate;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SportsController {

    private final BookingService service;
    private final DashboardService dashboard;

    public SportsController(BookingService service, DashboardService dashboard) {
        this.service = service;
        this.dashboard = dashboard;
    }

    private boolean isAdmin(AuthSession s) { return "ADMIN".equals(s.role()); }

    private void adminOnly(AuthSession s) {
        if (!isAdmin(s)) throw new SecurityException("Admin access required");
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@RequestAttribute("session") AuthSession s) {
        return dashboard.get(isAdmin(s) ? null : s.studentId());
    }

    @GetMapping("/students")
    public List<Student> students(@RequestAttribute("session") AuthSession s) {
        adminOnly(s);
        return service.getStudents();
    }

    @PostMapping("/facilities")
    public SportsFacility addFacility(@RequestBody SportsFacility f, @RequestAttribute("session") AuthSession s) {
        adminOnly(s);
        return service.addFacility(f);
    }

    @GetMapping("/facilities")
    public List<SportsFacility> facilities() { return service.getFacilities(); }

    @PatchMapping("/facilities/{id}/status")
    public SportsFacility updateStatus(@PathVariable("id") int id, @RequestBody Map<String, String> body,
                                       @RequestAttribute("session") AuthSession s) {
        adminOnly(s);
        return service.updateFacilityStatus(id, body.get("status"));
    }

    @GetMapping("/facilities/{id}/slots")
    public List<Slot> slots(@PathVariable("id") int id,
                            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getSlots(id, date);
    }

    @GetMapping("/facilities/{id}/bookings")
    public List<Booking> schedule(@PathVariable("id") int id,
                                  @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  @RequestAttribute("session") AuthSession s) {
        adminOnly(s);
        return service.getFacilitySchedule(id, date);
    }

    @PostMapping("/bookings")
    public Booking book(@RequestBody BookingRequest r, @RequestAttribute("session") AuthSession s) {
        Integer studentId = isAdmin(s) ? r.studentId() : s.studentId();   // students can book only for themselves
        return service.bookFacility(new BookingRequest(studentId, r.facilityId(), r.bookingDate(), r.startTime(), r.endTime()));
    }

    @PutMapping("/bookings/{id}/cancel")
    public Booking cancel(@PathVariable("id") int id, @RequestAttribute("session") AuthSession s) {
        return service.cancelOwnBooking(id, isAdmin(s) ? null : s.studentId());
    }

    @GetMapping("/students/{id}/bookings")
    public List<Booking> history(@PathVariable("id") int id,
                                 @RequestParam(value = "status", required = false) String status,
                                 @RequestAttribute("session") AuthSession s) {
        if (!isAdmin(s) && !Integer.valueOf(id).equals(s.studentId()))
            throw new SecurityException("You can view only your own bookings");
        return service.getHistory(id, status);
    }
}