package com.siontrack.siontrack.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/web")
public class PowerBIViewController {

    @GetMapping("/powerbi")
    public String mostrarPowerBI(Model model) {
        model.addAttribute("pageTitle", "Power BI");
        return "powerbi";
    }
}
