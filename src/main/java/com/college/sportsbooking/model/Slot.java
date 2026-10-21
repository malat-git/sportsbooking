package com.college.sportsbooking.model;

import java.time.LocalTime;

public record Slot(LocalTime startTime, LocalTime endTime, boolean available) {}