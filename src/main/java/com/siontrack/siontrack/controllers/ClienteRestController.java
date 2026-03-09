package com.siontrack.siontrack.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.siontrack.siontrack.DTO.Response.ClienteResponseDTO;
import com.siontrack.siontrack.DTO.Response.VehiculosResponseDTO;
import com.siontrack.siontrack.services.ClienteServicios;

@RestController
@RequestMapping("/api/clientes")
public class ClienteRestController {

    @Autowired
    private ClienteServicios clienteServicios;

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