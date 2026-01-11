package com.siontrack.siontrack.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.siontrack.siontrack.DTO.Request.ServicioRequestDTO;
import com.siontrack.siontrack.DTO.Response.ServicioResponseDTO;
import com.siontrack.siontrack.services.ServiciosService;

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

    // POST: Crear un nuevo servicio
    @PostMapping("/crear")
    public ResponseEntity<ServicioResponseDTO> crearServicio(@RequestBody ServicioRequestDTO dto) {
        ServicioResponseDTO nuevoServicio = serviciosService.crearServicio(dto);
        return new ResponseEntity<>(nuevoServicio, HttpStatus.CREATED);
    }

}
