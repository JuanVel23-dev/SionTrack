package com.siontrack.siontrack.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.siontrack.siontrack.models.Productos;
import com.siontrack.siontrack.services.ProductosServicios;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/siontrack/productos")
public class ProductosController {

    @Autowired
    private ProductosServicios productosServicios;

    @PostMapping("/agregarProducto")
    public ResponseEntity<Productos> crearProducto(@RequestBody Map<String, Object> payload) {
        Productos nuevoProducto = productosServicios.crearProductoConProveedor(payload);
        return new ResponseEntity<>(nuevoProducto, HttpStatus.CREATED);
    }

    @GetMapping
    public List<Productos> obtenerProductos() {
        return productosServicios.obtenerListaProductos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Productos> obtenerProductoId(@PathVariable int id) {
        return productosServicios.obtenerProductoByID(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProductos(@PathVariable int id) {
        if (productosServicios.obtenerProductoByID(id).isPresent()) {
            productosServicios.borrarProducto(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/actualizar/{id}")
    public ResponseEntity<Productos> actualizarProducto(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> payload) {

        Productos productoActualizado = productosServicios.actualizarProducto(id, payload);
        return ResponseEntity.ok(productoActualizado);
    }

}
