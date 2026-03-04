    package com.siontrack.siontrack.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.siontrack.siontrack.DTO.Response.AlertaStockDTO;
import com.siontrack.siontrack.services.ProductosServicios;

@RestController
@RequestMapping("/api/alertas")
public class AlertasRestController {

    @Autowired
    private ProductosServicios productosServicios;

    @GetMapping("/stock")
    public ResponseEntity<List<AlertaStockDTO>> obtenerAlertasStock() {
        return ResponseEntity.ok(productosServicios.obtenerAlertasStock());
    }
}