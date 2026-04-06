    package com.siontrack.siontrack.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.siontrack.siontrack.DTO.Response.AlertaStockDTO;
import com.siontrack.siontrack.services.ProductosServicios;

@RestController
@RequestMapping("/api/alertas")
public class AlertasRestController {

    @Autowired
    private ProductosServicios productosServicios;

    // Paginado — usado por el modal de reabastecer (20 por página)
    @GetMapping("/stock")
    public ResponseEntity<Page<AlertaStockDTO>> obtenerAlertasStockPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productosServicios.obtenerAlertasStockPaginado(page, size));
    }

    // Sin paginar — usado por la campana de notificaciones
    @GetMapping("/stock/all")
    public ResponseEntity<List<AlertaStockDTO>> obtenerTodasAlertasStock() {
        return ResponseEntity.ok(productosServicios.obtenerAlertasStock());
    }
}