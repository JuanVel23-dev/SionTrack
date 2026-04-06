package com.siontrack.siontrack.controllers;

import com.siontrack.siontrack.DTO.Response.AlertaStockDTO;
import com.siontrack.siontrack.repository.ClienteRepository;
import com.siontrack.siontrack.repository.ProveedoresRepository;
import com.siontrack.siontrack.repository.ProductosRepository;
import com.siontrack.siontrack.repository.ServiciosRepository;
import com.siontrack.siontrack.services.ProductosServicios;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    private ServiciosRepository serviciosRepository;

    @Autowired
    private ProductosServicios productosServicios;

    @GetMapping({"/", "/dashboard"})
    public String mostrarDashboard(
            @RequestParam(defaultValue = "0") int pageStock,
            Model model) {

        // Conteos rápidos con COUNT(*) — no cargan entidades
        long totalClientes = clienteRepository.count();
        long totalProveedores = proveedorRepository.count();
        long totalProductos = productoRepository.count();
        long totalServicios = serviciosRepository.count();

        // Alertas de stock — una sola llamada, se usa para conteos y paginación
        List<AlertaStockDTO> todasAlertas = productosServicios.obtenerAlertasStock();

        // Paginar manualmente desde la lista ya calculada (50 por página)
        int fromIndex = pageStock * 50;
        int toIndex = Math.min(fromIndex + 50, todasAlertas.size());
        List<AlertaStockDTO> alertasPagina = fromIndex < todasAlertas.size()
                ? todasAlertas.subList(fromIndex, toIndex) : List.of();
        Page<AlertaStockDTO> pageAlertas = new PageImpl<>(alertasPagina,
                PageRequest.of(pageStock, 50), todasAlertas.size());

        // Conteos por nivel para los chips del header
        long countAgotado = todasAlertas.stream()
                .filter(a -> "AGOTADO".equals(a.getNivelAlerta())).count();
        long countCritico = todasAlertas.stream()
                .filter(a -> "CRITICO".equals(a.getNivelAlerta())).count();
        long countBajo = todasAlertas.stream()
                .filter(a -> "BAJO".equals(a.getNivelAlerta())).count();
        long countAdvertencia = todasAlertas.stream()
                .filter(a -> "ADVERTENCIA".equals(a.getNivelAlerta())).count();
        long countPopulares = todasAlertas.stream()
                .filter(AlertaStockDTO::isEsPopular).count();

        model.addAttribute("totalClientes", totalClientes);
        model.addAttribute("totalProveedores", totalProveedores);
        model.addAttribute("totalProductos", totalProductos);
        model.addAttribute("totalServicios", totalServicios);
        model.addAttribute("alertasStock", alertasPagina);
        model.addAttribute("pageAlertasStock", pageAlertas);
        model.addAttribute("totalAlertas", (long) todasAlertas.size());
        model.addAttribute("countAgotado", countAgotado);
        model.addAttribute("countCritico", countCritico);
        model.addAttribute("countBajo", countBajo);
        model.addAttribute("countAdvertencia", countAdvertencia);
        model.addAttribute("countPopulares", countPopulares);

        return "dashboard";
    }
}