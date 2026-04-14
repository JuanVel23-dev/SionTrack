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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * API REST de consulta de proveedores.
 * Las operaciones de creación, actualización y eliminación se realizan
 * desde los controladores de vista ({@code ProveedoresViewController}).
 */
@Tag(name = "Proveedores", description = "Consulta y búsqueda de proveedores")
@RestController
@RequestMapping("/api/proveedores")
public class ProveedoresRestController {

    @Autowired
    private ProveedoresService proveedoresService;

    /**
     * Búsqueda paginada de proveedores por nombre.
     * Usado por el selector del formulario de productos.
     */
    @Operation(
        summary = "Buscar proveedores paginado",
        description = "Devuelve una página de proveedores filtrada por nombre. "
                    + "Sin parámetro de búsqueda devuelve todos los proveedores ordenados por ID descendente."
    )
    @GetMapping("/buscar")
    public ResponseEntity<Page<ProveedoresResponseDTO>> buscarProveedores(
            @Parameter(description = "Término de búsqueda por nombre")
            @RequestParam(defaultValue = "") String q,
            @Parameter(description = "Número de página (base 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de la página")
            @RequestParam(defaultValue = "20") int size) {
        Page<ProveedoresResponseDTO> resultado = proveedoresService.obtenerListaProveedoresPaginado(
                PageRequest.of(page, size), q.isEmpty() ? null : q);
        return ResponseEntity.ok(resultado);
    }
}
