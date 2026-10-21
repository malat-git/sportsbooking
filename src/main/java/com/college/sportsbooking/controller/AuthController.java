package com.college.sportsbooking.controller;

import com.college.sportsbooking.model.*;
import com.college.sportsbooking.service.AuthService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/signup")
    public AuthSession signup(@RequestBody SignupRequest r) { return auth.signup(r); }

    @PostMapping("/login")
    public AuthSession login(@RequestBody LoginRequest r) { return auth.login(r); }

    @PostMapping("/logout")
    public Map<String, String> logout(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        auth.logout(token);
        return Map.of("message", "Logged out");
    }

    @GetMapping("/me")
    public AuthSession me(@RequestAttribute("session") AuthSession s) { return s; }
}