package com.siontrack.siontrack.controllers;

import com.siontrack.siontrack.repository.ClienteRepository;
import com.siontrack.siontrack.repository.ProveedoresRepository;
import com.siontrack.siontrack.repository.ProductosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/web")
public class DashboardViewController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProveedoresRepository proveedorRepository;

    @Autowired
    private ProductosRepository productoRepository;

    // Descomentar cuando exista el repositorio de servicios
    // @Autowired
    // private ServicioRepository servicioRepository;

    @GetMapping({"/", "/dashboard"})
    public String mostrarDashboard(Model model) {
        
        // Obtener conteos de cada entidad
        long totalClientes = clienteRepository.count();
        long totalProveedores = proveedorRepository.count();
        long totalProductos = productoRepository.count();
        long totalServicios = 0; // servicioRepository.count(); cuando exista

        // Pasar las variables al modelo para Thymeleaf
        model.addAttribute("totalClientes", totalClientes);
        model.addAttribute("totalProveedores", totalProveedores);
        model.addAttribute("totalProductos", totalProductos);
        model.addAttribute("totalServicios", totalServicios);

        return "dashboard";
    }
}