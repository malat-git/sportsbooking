package com.college.sportsbooking.model;

import java.time.LocalTime;

public record SportsFacility(Integer facilityId, String facilityName, String sportType,
                             String location, LocalTime openingTime, LocalTime closingTime,
                             String status) {}