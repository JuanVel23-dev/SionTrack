package com.siontrack.siontrack.controllers;

import com.siontrack.siontrack.DTO.Response.AlertaStockDTO;
import com.siontrack.siontrack.repository.ClienteRepository;
import com.siontrack.siontrack.repository.ProveedoresRepository;
import com.siontrack.siontrack.repository.ProductosRepository;
import com.siontrack.siontrack.services.ProductosServicios;

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

    @GetMapping({"/", "/dashboard"})
    public String mostrarDashboard(Model model) {

        long totalClientes = clienteRepository.count();
        long totalProveedores = proveedorRepository.count();
        long totalProductos = productoRepository.count();
        long totalServicios = 0;

        // Alertas de stock
        List<AlertaStockDTO> alertasStock = productosServicios.obtenerAlertasStock();

        model.addAttribute("totalClientes", totalClientes);
        model.addAttribute("totalProveedores", totalProveedores);
        model.addAttribute("totalProductos", totalProductos);
        model.addAttribute("totalServicios", totalServicios);
        model.addAttribute("alertasStock", alertasStock);
        model.addAttribute("totalAlertas", alertasStock.size());

        return "dashboard";
    }
}