package com.siontrack.siontrack.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.siontrack.siontrack.models.Clientes;
import com.siontrack.siontrack.services.ClienteServicios;
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

    @PostMapping
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
                .map(Clientes -> {
                    Clientes.setNombre(detalleCliente.getNombre());
                    Clientes.setCedula_ruc(detalleCliente.getCedula_ruc());
                    Clientes.setTipo_cliente(detalleCliente.getTipo_cliente());
                    Clientes.setCorreos(detalleCliente.getCorreos());
                    Clientes.setDirecciones(detalleCliente.getDirecciones());
                    Clientes.setTelefonos(detalleCliente.getTelefonos());
                    return ResponseEntity.ok(clienteServicios.saveCliente(Clientes));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
