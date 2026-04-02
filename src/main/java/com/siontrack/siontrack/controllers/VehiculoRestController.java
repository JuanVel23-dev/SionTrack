package com.siontrack.siontrack.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.siontrack.siontrack.models.Vehiculos;
import com.siontrack.siontrack.repository.VehiculosRepository;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoRestController {

    @Autowired
    private VehiculosRepository vehiculosRepository;

    /**
     * Busca vehículos por placa (parcial o completa, sin distinción de mayúsculas).
     * Devuelve nombre, cédula y placa del dueño de cada vehículo.
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<Map<String, Object>>> buscarPorPlaca(@RequestParam String placa) {
        // Evitar busquedas vacias que retornan toda la tabla
        if (placa == null || placa.trim().isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        // Limitar longitud de busqueda
        if (placa.length() > 10) {
            return ResponseEntity.badRequest().build();
        }
        List<Vehiculos> vehiculos = vehiculosRepository.buscarPorPlacaContiene(placa.trim());
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Vehiculos v : vehiculos) {
            Map<String, Object> item = new HashMap<>();
            item.put("vehiculo_id", v.getVehiculo_id());
            item.put("placa", v.getPlaca());
            item.put("kilometraje_actual", v.getKilometraje_actual());
            item.put("nombre_cliente", v.getClientes().getNombre());
            item.put("cedula_cliente", v.getClientes().getCedula_ruc());
            item.put("cliente_id", v.getClientes().getCliente_id());
            resultado.add(item);
        }

        return ResponseEntity.ok(resultado);
    }

    /**
     * Elimina un vehículo por su ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarVehiculo(@PathVariable Integer id) {
        if (!vehiculosRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        vehiculosRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
