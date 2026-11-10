package com.raviranjan.os.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the portfolio UI and Admin OS directly on Port 8080 as well as Port 3000,
 * and handles Cloud Shell Web Preview query strings like /?authuser=0/admin cleanly
 * without exposing any backend configuration or database credentials.
 */
@Controller
public class RootWelcomeController {

    @GetMapping("/")
    public String rootPage(HttpServletRequest request) {
        String qs = request.getQueryString();
        if (qs != null && qs.toLowerCase().contains("/admin")) {
            return "forward:/admin_cms.html";
        }
        return "forward:/index.html";
    }

    @GetMapping({"/admin", "/admin/"})
    public String adminPage() {
        return "forward:/admin_cms.html";
    }
}
