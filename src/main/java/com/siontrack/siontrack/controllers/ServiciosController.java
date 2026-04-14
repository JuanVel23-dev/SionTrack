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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * API REST para la gestión de servicios prestados a clientes.
 */
@Tag(name = "Servicios", description = "Creación, consulta y eliminación de servicios")
@RestController
@RequestMapping("/api/servicios")
public class ServiciosController {

    @Autowired
    private ServiciosService serviciosService;

    /**
     * Devuelve todos los servicios registrados.
     */
    @Operation(summary = "Listar todos los servicios")
    @GetMapping
    public ResponseEntity<List<ServicioResponseDTO>> listarServicios() {
        return ResponseEntity.ok(serviciosService.obtenerTodos());
    }

    /**
     * Obtiene un servicio por su ID con sus detalles completos.
     */
    @Operation(
        summary = "Obtener servicio por ID",
        description = "Devuelve el servicio con sus detalles de productos, cliente y vehículo."
    )
    @ApiResponse(responseCode = "200", description = "Servicio encontrado")
    @ApiResponse(responseCode = "500", description = "Servicio no encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<ServicioResponseDTO> obtenerServicio(
            @Parameter(description = "ID del servicio") @PathVariable Integer id) {
        return ResponseEntity.ok(serviciosService.obtenerServicioPorId(id));
    }

    /**
     * Crea un nuevo servicio con sus detalles de productos o mano de obra.
     * Al crear el servicio se descuenta el stock de los productos involucrados
     * y se programan automáticamente los recordatorios del próximo servicio.
     */
    @Operation(
        summary = "Crear servicio",
        description = "Crea un nuevo servicio. Descuenta el inventario de los productos incluidos "
                    + "y genera recordatorios automáticos de WhatsApp para el próximo mantenimiento."
    )
    @ApiResponse(responseCode = "201", description = "Servicio creado exitosamente")
    @PostMapping("/crear")
    public ResponseEntity<ServicioResponseDTO> crearServicio(@Valid @RequestBody ServicioRequestDTO dto) {
        ServicioResponseDTO nuevoServicio = serviciosService.crearServicio(dto);
        return new ResponseEntity<>(nuevoServicio, HttpStatus.CREATED);
    }

    /**
     * Elimina un servicio y sus detalles asociados.
     */
    @Operation(summary = "Eliminar servicio")
    @ApiResponse(responseCode = "200", description = "Servicio eliminado")
    @ApiResponse(responseCode = "404", description = "Servicio no encontrado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminarServicio(
            @Parameter(description = "ID del servicio a eliminar") @PathVariable Integer id) {
        try {
            serviciosService.eliminarServicio(id);
            Map<String, String> respuesta = new HashMap<>();
            respuesta.put("mensaje", "Servicio y sus detalles eliminados correctamente.");
            return ResponseEntity.ok(respuesta);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}
