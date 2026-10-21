package com.college.sportsbooking.model;

public record AuthSession(String token, Integer studentId, String name, String role) {}