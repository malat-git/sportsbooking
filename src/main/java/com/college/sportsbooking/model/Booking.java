package com.college.sportsbooking.model;

import java.time.*;

public record Booking(Integer bookingId, Integer studentId, Integer facilityId,
                      LocalDate bookingDate, LocalTime startTime, LocalTime endTime,
                      LocalDateTime bookingTime, String status) {}