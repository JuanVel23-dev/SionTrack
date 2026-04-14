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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * API REST de alertas de stock bajo.
 */
@Tag(name = "Alertas de Stock", description = "Consulta de productos con stock por debajo del mínimo")
@RestController
@RequestMapping("/api/alertas")
public class AlertasRestController {

    @Autowired
    private ProductosServicios productosServicios;

    /**
     * Devuelve las alertas de stock paginadas.
     * Usado por el modal de reabastecimiento (20 registros por página por defecto).
     */
    @Operation(
        summary = "Obtener alertas de stock paginadas",
        description = "Devuelve una página de alertas de stock ordenadas por prioridad compuesta "
                    + "(nivel de urgencia × popularidad del producto)."
    )
    @GetMapping("/stock")
    public ResponseEntity<Page<AlertaStockDTO>> obtenerAlertasStockPaginado(
            @Parameter(description = "Número de página (base 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de la página")
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productosServicios.obtenerAlertasStockPaginado(page, size));
    }

    /**
     * Devuelve todas las alertas de stock sin paginación.
     * Usado por la campana de notificaciones del dashboard para calcular el conteo total.
     */
    @Operation(
        summary = "Obtener todas las alertas de stock",
        description = "Devuelve la lista completa de alertas sin paginación, ordenadas por prioridad. "
                    + "Usar con precaución en catálogos grandes."
    )
    @GetMapping("/stock/all")
    public ResponseEntity<List<AlertaStockDTO>> obtenerTodasAlertasStock() {
        return ResponseEntity.ok(productosServicios.obtenerAlertasStock());
    }
}
