package com.siontrack.siontrack.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.siontrack.siontrack.DTO.Request.ServicioRequestDTO;
import com.siontrack.siontrack.DTO.Response.ServicioResponseDTO;
import com.siontrack.siontrack.services.ServiciosService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/servicios")
public class ServiciosController {

    @Autowired
    private ServiciosService serviciosService;

    // GET: Listar todos los servicios
    @GetMapping
    public ResponseEntity<List<ServicioResponseDTO>> listarServicios() {
        return ResponseEntity.ok(serviciosService.obtenerTodos());
    }

    // GET: Obtener un servicio por ID con sus detalles completos
    @GetMapping("/{id}")
    public ResponseEntity<ServicioResponseDTO> obtenerServicio(@PathVariable Integer id) {
        return ResponseEntity.ok(serviciosService.obtenerServicioPorId(id));
    }

    // POST: Crear un nuevo servicio
    @PostMapping("/crear")
    public ResponseEntity<ServicioResponseDTO> crearServicio(@Valid @RequestBody ServicioRequestDTO dto) {
        ServicioResponseDTO nuevoServicio = serviciosService.crearServicio(dto);
        return new ResponseEntity<>(nuevoServicio, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminarServicio(@PathVariable Integer id) {
        try {
            serviciosService.eliminarServicio(id);
            
            // Retornamos un JSON de éxito
            Map<String, String> respuesta = new HashMap<>();
            respuesta.put("mensaje", "Servicio y sus detalles eliminados correctamente.");
            return ResponseEntity.ok(respuesta);
            
        } catch (RuntimeException e) {
            // Si no lo encuentra, devolvemos un error 404
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}
