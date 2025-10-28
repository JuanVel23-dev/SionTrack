package com.siontrack.siontrack.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {
    /**
     * Muestra la página de inicio de sesión personalizada.
     * Spring Security redirigirá a esta URL cuando se necesite autenticación.
     */
    @GetMapping("/login")
    public String showLoginPage() {
        return "login"; // Busca el archivo "login.html" en /resources/templates/
    }
    
    // NO necesitas un @PostMapping("/login"), Spring Security lo hace por ti.
}
