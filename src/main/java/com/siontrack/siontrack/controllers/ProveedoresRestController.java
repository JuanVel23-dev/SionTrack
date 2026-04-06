package com.siontrack.siontrack.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.siontrack.siontrack.DTO.Response.ProveedoresResponseDTO;
import com.siontrack.siontrack.services.ProveedoresService;

@RestController
@RequestMapping("/api/proveedores")
public class ProveedoresRestController {

    @Autowired
    private ProveedoresService proveedoresService;

    // Busqueda paginada — usado por el selector del formulario de productos
    @GetMapping("/buscar")
    public ResponseEntity<Page<ProveedoresResponseDTO>> buscarProveedores(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ProveedoresResponseDTO> resultado = proveedoresService.obtenerListaProveedoresPaginado(
                PageRequest.of(page, size), q.isEmpty() ? null : q);
        return ResponseEntity.ok(resultado);
    }
}
