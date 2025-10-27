package com.siontrack.siontrack.controllers;

import org.springframework.web.bind.annotation.RestController;


import com.siontrack.siontrack.models.Proveedores;
import com.siontrack.siontrack.services.ProveedoresService;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/siontrack/proveedores")
public class ProveedoresController {

    @Autowired
    private ProveedoresService proveedoresService;

    @PostMapping("/agregarProveedor")
    public ResponseEntity <Proveedores> crearProveedor(@RequestBody Proveedores provedor) {
        Proveedores nuevoProveedor = proveedoresService.guardarProveedor(provedor);
        return new ResponseEntity<>(nuevoProveedor, HttpStatus.CREATED);
    }

    @GetMapping
    public List<Proveedores> obtenerProveedores() {
        return proveedoresService.obtenerListaProveedores();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Proveedores> obtenerProveedorId(@PathVariable int id) {
        return proveedoresService.obtenerProveedorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProveedor(@PathVariable int id){
        if(proveedoresService.obtenerProveedorId(id).isPresent()){
            proveedoresService.borrarProveedor(id);
            return ResponseEntity.noContent().build();
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/actualizar/{id}")
    public ResponseEntity<Proveedores> actualizarProveedor(
            @PathVariable Integer id, 
            @RequestBody Map<String, Object> payload) {
        
        Proveedores proveedorActualizado = proveedoresService.actualizarProveedor(id, payload);
        return ResponseEntity.ok(proveedorActualizado);
    }
    
}
