package com.college.sportsbooking.config;

import com.college.sportsbooking.model.AuthSession;
import com.college.sportsbooking.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService auth;

    public AuthInterceptor(AuthService auth) { this.auth = auth; }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        AuthSession s = auth.get(req.getHeader("X-Auth-Token"));
        if (s == null) {
            res.setStatus(401);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"Please login to continue\"}");
            return false;
        }
        req.setAttribute("session", s);
        return true;
    }
}