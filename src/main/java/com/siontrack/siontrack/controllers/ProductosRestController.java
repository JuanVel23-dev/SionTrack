package com.siontrack.siontrack.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.siontrack.siontrack.DTO.Response.ProductoPopularDTO;
import com.siontrack.siontrack.DTO.Response.ProductosResponseDTO;
import com.siontrack.siontrack.services.ProductosServicios;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/productos")
public class ProductosRestController {

    @Autowired
    private ProductosServicios productosServicios;

   // Búsqueda paginada — usado por el selector del formulario de servicios
   @GetMapping("/buscar")
   public ResponseEntity<Page<ProductosResponseDTO>> buscarProductos(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ProductosResponseDTO> resultado = productosServicios.obtenerListaProductosPaginado(
                PageRequest.of(page, size), q.isEmpty() ? null : q);
        return ResponseEntity.ok(resultado);
   }

   @GetMapping("/populares")
   public ResponseEntity<List<ProductoPopularDTO>> getPopulares(
            @RequestParam(defaultValue = "10") int limite,
            @RequestParam(defaultValue = "general") String periodo) {

        // Limitar el parametro para prevenir consumo excesivo de memoria
        if (limite < 1) limite = 1;
        if (limite > 100) limite = 100;

        return ResponseEntity.ok(productosServicios.obtenerListaPopulares(limite, periodo));
    }
    
}
