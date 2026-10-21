package com.college.sportsbooking.model;

public record UserAccount(Integer accountId, Integer studentId, String email, String passwordHash, String role) {}