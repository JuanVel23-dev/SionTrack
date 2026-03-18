package com.siontrack.siontrack.controllers;

import com.siontrack.siontrack.DTO.Response.AlertaStockDTO;
import com.siontrack.siontrack.repository.ClienteRepository;
import com.siontrack.siontrack.repository.ProveedoresRepository;
import com.siontrack.siontrack.repository.ProductosRepository;
import com.siontrack.siontrack.services.ProductosServicios;
import com.siontrack.siontrack.services.ServiciosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/web")
public class DashboardViewController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProveedoresRepository proveedorRepository;

    @Autowired
    private ProductosRepository productoRepository;

    @Autowired
    private ProductosServicios productosServicios;

    @Autowired
    private ServiciosService serviciosService;

    @GetMapping({"/", "/dashboard"})
    public String mostrarDashboard(Model model) {

        long totalClientes = clienteRepository.count();
        long totalProveedores = proveedorRepository.count();
        long totalProductos = productoRepository.count();
        long totalServicios = serviciosService.obtenerTodos().size();

        // Alertas de stock (nuevo sistema v2)
        List<AlertaStockDTO> alertasStock = productosServicios.obtenerAlertasStock();

        // Conteos por nivel para los chips del header
        long countAgotado = alertasStock.stream()
                .filter(a -> "AGOTADO".equals(a.getNivelAlerta())).count();
        long countCritico = alertasStock.stream()
                .filter(a -> "CRITICO".equals(a.getNivelAlerta())).count();
        long countBajo = alertasStock.stream()
                .filter(a -> "BAJO".equals(a.getNivelAlerta())).count();
        long countAdvertencia = alertasStock.stream()
                .filter(a -> "ADVERTENCIA".equals(a.getNivelAlerta())).count();
        long countPopulares = alertasStock.stream()
                .filter(AlertaStockDTO::isEsPopular).count();

        model.addAttribute("totalClientes", totalClientes);
        model.addAttribute("totalProveedores", totalProveedores);
        model.addAttribute("totalProductos", totalProductos);
        model.addAttribute("totalServicios", totalServicios);
        model.addAttribute("alertasStock", alertasStock);
        model.addAttribute("totalAlertas", alertasStock.size());
        model.addAttribute("countAgotado", countAgotado);
        model.addAttribute("countCritico", countCritico);
        model.addAttribute("countBajo", countBajo);
        model.addAttribute("countAdvertencia", countAdvertencia);
        model.addAttribute("countPopulares", countPopulares);

        return "dashboard";
    }
}