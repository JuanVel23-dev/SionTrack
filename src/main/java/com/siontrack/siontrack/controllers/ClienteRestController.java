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

@RestController
@RequestMapping("/api/clientes")
public class ClienteRestController {

    @Autowired
    private ClienteServicios clienteServicios;

    // Búsqueda paginada — usado por el selector del formulario de servicios
    @GetMapping("/buscar")
    public ResponseEntity<Page<ClienteResponseDTO>> buscarClientes(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ClienteResponseDTO> resultado = clienteServicios.obtenerListaClientesPaginado(
                PageRequest.of(page, size), q.isEmpty() ? null : q);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Devuelve todos los datos de un cliente por su ID.
     * Usado por el modal de detalle de clientes.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> obtenerClientePorId(@PathVariable Integer id) {
        ClienteResponseDTO cliente = clienteServicios.obtenerClientePorId(id);
        return ResponseEntity.ok(cliente);
    }

    /**
     * Devuelve los vehículos de un cliente por su ID.
     * Usado por el formulario de servicios para la cascada cliente → vehículos.
     */
    @GetMapping("/{id}/vehiculos")
    public ResponseEntity<List<VehiculosResponseDTO>> obtenerVehiculosPorCliente(@PathVariable Integer id) {
        ClienteResponseDTO cliente = clienteServicios.obtenerClientePorId(id);
        List<VehiculosResponseDTO> vehiculos = cliente.getVehiculos();
        if (vehiculos == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(vehiculos);
    }
}