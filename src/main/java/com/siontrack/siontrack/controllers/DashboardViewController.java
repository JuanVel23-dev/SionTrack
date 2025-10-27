package com.siontrack.siontrack.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/web") // Se mantiene en la sección /web
public class DashboardViewController {

    /**
     * Este método manejará las peticiones a:
     * http://localhost:8081/web/
     * y
     * http://localhost:8081/web/dashboard
     */
    @GetMapping({"/", "/dashboard"})
    public String mostrarDashboard() {
        // Esto le dice a Spring Boot que busque el archivo:
        // /resources/templates/dashboard.html
        return "dashboard";
    }
}