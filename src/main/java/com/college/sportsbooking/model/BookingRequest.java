package com.college.sportsbooking.model;

import java.time.*;

public record BookingRequest(Integer studentId, Integer facilityId, LocalDate bookingDate,
                             LocalTime startTime, LocalTime endTime) {}