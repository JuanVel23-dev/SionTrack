package com.siontrack.siontrack.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.siontrack.siontrack.models.Clientes;
import com.siontrack.siontrack.services.ClienteServicios;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;


@RestController
@RequestMapping("/siontrack/clientes")
public class ClienteController {

    @Autowired
    private ClienteServicios clienteServicios;

    @PostMapping(value = "/agregarCliente", 
             consumes = {"application/json", "application/json;charset=UTF-8"},
             produces = "application/json")
    public ResponseEntity <Clientes> crearCliente(@RequestBody Clientes cliente) {
        Clientes nuevoCliente = clienteServicios.saveCliente(cliente);
        return new ResponseEntity<>(nuevoCliente, HttpStatus.CREATED);
    }

    @GetMapping
    public List<Clientes> obtenerClientes() {
        return clienteServicios.obtenerListaClientes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Clientes> obtenerClienteID(@PathVariable int id) {
        return clienteServicios.getClienteById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Clientes> actualizarCliente(@PathVariable int id, @RequestBody Clientes detalleCliente) {
         return clienteServicios.getClienteById(id)
        .map(clienteExistente -> {
            Clientes clienteActualizado = clienteServicios.actualizarCliente(clienteExistente, detalleCliente);
            return ResponseEntity.ok(clienteActualizado);
        })
        .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente(@PathVariable int id){
        if(clienteServicios.getClienteById(id).isPresent()){
            clienteServicios.deleteCliente(id);
            return ResponseEntity.noContent().build();
        }else{
            return ResponseEntity.notFound().build();
        }
    }
}
