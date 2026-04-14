package com.siontrack.siontrack.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.siontrack.siontrack.DTO.Response.ProductoPopularDTO;
import com.siontrack.siontrack.DTO.Response.ProductosResponseDTO;
import com.siontrack.siontrack.services.ProductosServicios;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * API REST de consulta de productos.
 * Las operaciones de creación, actualización y eliminación se realizan
 * desde los controladores de vista ({@code ProductosViewController}).
 */
@Tag(name = "Productos", description = "Consulta, búsqueda y ranking de productos")
@RestController
@RequestMapping("/api/productos")
public class ProductosRestController {

    @Autowired
    private ProductosServicios productosServicios;

    /**
     * Búsqueda paginada de productos por nombre o código.
     * Usado por el selector del formulario de detalles de servicio.
     */
    @Operation(
        summary = "Buscar productos paginado",
        description = "Devuelve una página de productos filtrada por nombre o código. "
                    + "Sin parámetro de búsqueda devuelve todos los productos ordenados por ID descendente."
    )
    @GetMapping("/buscar")
    public ResponseEntity<Page<ProductosResponseDTO>> buscarProductos(
            @Parameter(description = "Término de búsqueda por nombre o código")
            @RequestParam(defaultValue = "") String q,
            @Parameter(description = "Número de página (base 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de la página")
            @RequestParam(defaultValue = "20") int size) {
        Page<ProductosResponseDTO> resultado = productosServicios.obtenerListaProductosPaginado(
                PageRequest.of(page, size), q.isEmpty() ? null : q);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Devuelve el ranking de productos más vendidos en el período indicado.
     * El parámetro {@code limite} se restringe al rango [1, 100] para prevenir
     * consumo excesivo de memoria.
     */
    @Operation(
        summary = "Obtener productos populares",
        description = "Devuelve los productos más vendidos ordenados por cantidad total vendida. "
                    + "Períodos válidos: semana, mes, trimestre, anio, general."
    )
    @GetMapping("/populares")
    public ResponseEntity<List<ProductoPopularDTO>> getPopulares(
            @Parameter(description = "Número máximo de resultados (1–100)")
            @RequestParam(defaultValue = "10") int limite,
            @Parameter(description = "Período de análisis: semana | mes | trimestre | anio | general")
            @RequestParam(defaultValue = "general") String periodo) {

        if (limite < 1) limite = 1;
        if (limite > 100) limite = 100;

        return ResponseEntity.ok(productosServicios.obtenerListaPopulares(limite, periodo));
    }
}
