package com.siontrack.siontrack.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.siontrack.siontrack.DTO.Response.ClienteResponseDTO;
import com.siontrack.siontrack.DTO.Response.VehiculosResponseDTO;
import com.siontrack.siontrack.services.ClienteServicios;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * API REST de consulta de clientes.
 * Las operaciones de creación, actualización y eliminación se realizan
 * desde los controladores de vista ({@code ClienteViewController}).
 */
@Tag(name = "Clientes", description = "Consulta y búsqueda de clientes y sus vehículos")
@RestController
@RequestMapping("/api/clientes")
public class ClienteRestController {

    @Autowired
    private ClienteServicios clienteServicios;

    /**
     * Búsqueda paginada de clientes por nombre o cédula.
     * Usado por el selector del formulario de servicios para la cascada cliente → vehículos.
     */
    @Operation(
        summary = "Buscar clientes paginado",
        description = "Devuelve una página de clientes filtrada por nombre o cédula. "
                    + "Sin parámetro de búsqueda devuelve todos los clientes ordenados por ID descendente."
    )
    @GetMapping("/buscar")
    public ResponseEntity<Page<ClienteResponseDTO>> buscarClientes(
            @Parameter(description = "Término de búsqueda por nombre o cédula")
            @RequestParam(defaultValue = "") String q,
            @Parameter(description = "Número de página (base 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de la página")
            @RequestParam(defaultValue = "20") int size) {
        Page<ClienteResponseDTO> resultado = clienteServicios.obtenerListaClientesPaginado(
                PageRequest.of(page, size), q.isEmpty() ? null : q);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Devuelve todos los datos de un cliente por su ID.
     * Usado por el modal de detalle de clientes.
     */
    @Operation(
        summary = "Obtener cliente por ID",
        description = "Devuelve el cliente con todos sus datos de contacto, vehículos y estado de notificaciones."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> obtenerClientePorId(
            @Parameter(description = "ID del cliente") @PathVariable Integer id) {
        ClienteResponseDTO cliente = clienteServicios.obtenerClientePorId(id);
        return ResponseEntity.ok(cliente);
    }

    /**
     * Devuelve los vehículos de un cliente por su ID.
     * Usado por el formulario de servicios para la cascada cliente → vehículos.
     */
    @Operation(
        summary = "Obtener vehículos de un cliente",
        description = "Devuelve la lista de vehículos asociados al cliente indicado. "
                    + "Devuelve lista vacía si el cliente no tiene vehículos registrados."
    )
    @GetMapping("/{id}/vehiculos")
    public ResponseEntity<List<VehiculosResponseDTO>> obtenerVehiculosPorCliente(
            @Parameter(description = "ID del cliente") @PathVariable Integer id) {
        ClienteResponseDTO cliente = clienteServicios.obtenerClientePorId(id);
        List<VehiculosResponseDTO> vehiculos = cliente.getVehiculos();
        if (vehiculos == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(vehiculos);
    }
}
