package com.siontrack.siontrack.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/web")
public class DashboardViewController {

    @GetMapping({"/", "/dashboard"})
    public String mostrarDashboard() {
        return "dashboard";
    }
}