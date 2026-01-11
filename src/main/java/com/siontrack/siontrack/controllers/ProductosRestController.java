package com.siontrack.siontrack.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.siontrack.siontrack.DTO.Response.ProductoPopularDTO;
import com.siontrack.siontrack.services.ProductosServicios;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/productos")
public class ProductosRestController {

    @Autowired
    private ProductosServicios productosServicios;

   @GetMapping("/populares")
   public ResponseEntity<List<ProductoPopularDTO>> getPopulares(
            @RequestParam(defaultValue = "10") int limite,
            @RequestParam(defaultValue = "general") String periodo) {
        
        return ResponseEntity.ok(productosServicios.obtenerListaPopulares(limite, periodo));
    }
    
}
