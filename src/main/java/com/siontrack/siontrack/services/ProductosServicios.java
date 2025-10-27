package com.siontrack.siontrack.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.siontrack.siontrack.models.Inventario;
import com.siontrack.siontrack.models.Productos;
import com.siontrack.siontrack.models.Proveedores;
import com.siontrack.siontrack.repository.ProductosRepository;
import com.siontrack.siontrack.repository.ProveedoresRepository;

import jakarta.transaction.Transactional;

@Service
public class ProductosServicios {

    @Autowired
    private ProductosRepository productosRepository;

    @Autowired
    private ProveedoresRepository proveedorRepository;

    public List<Productos> obtenerListaProductos() {
        return productosRepository.findAll();
    }

    public Optional<Productos> obtenerProductoByID(int id) {
        return productosRepository.findById(id);
    }

    @Transactional
    public Productos crearProductoConProveedor(Map<String, Object> payload) {

        Integer proveedorId = ((Number) payload.get("proveedor_id")).intValue();
        Proveedores proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + proveedorId));

        Productos producto = new Productos();

        producto.setNombre((String) payload.get("nombre"));
        producto.setCategoria((String) payload.get("categoria"));
        producto.setMarca((String) payload.get("marca"));
        producto.setUnidad_medida((String) payload.get("unidad_medida"));
        producto.setPrecio_venta(new BigDecimal(payload.get("precio_venta").toString()));
        producto.setPrecio_compra(new BigDecimal(payload.get("precio_compra").toString()));
        producto.setEstado((String) payload.get("estado"));

        producto.setProveedor(proveedor);
        proveedor.getProductos().add(producto);

        Map<String, Object> inventarioPayload = (Map<String, Object>) payload.get("inventario");

        Inventario inventario = new Inventario();
        inventario.setCantidad_disponible(((Number) inventarioPayload.get("cantidad_disponible")).intValue());
        inventario.setStock_minimo(((Number) inventarioPayload.get("stock_minimo")).intValue());
        inventario.setUbicacion((String) inventarioPayload.get("ubicacion"));

        inventario.setProducto(producto);
        producto.setInventario(inventario);

        return productosRepository.save(producto);
    }

    public void borrarProducto(int id) {
        productosRepository.deleteById(id);
    }

    @Transactional
    public Productos actualizarProducto(Integer id, Map<String, Object> payload) {

        // 1. Buscar el producto existente
        Productos producto = productosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));

        // --- 2. ASIGNACIÓN SEGURA Y RESISTENTE A NULOS ---

        // 2a. Campos de Texto
        // Verificar múltiples variaciones de la clave (snake_case, camelCase) si no usa
        // la configuración global.
        String nombre = (String) payload.getOrDefault("nombre", payload.get("Nombre"));
        if (nombre != null) {
            producto.setNombre(nombre);
        }

        // 2b. Campos BigDecimal
        Object precioVentaObj = payload.get("precio_venta"); // Primero intenta con snake_case
        if (precioVentaObj == null) {
            precioVentaObj = payload.get("precioVenta"); // Luego intenta con camelCase
        }

        if (precioVentaObj != null) {
            // Asegurarse de que se convierte de forma segura (sin llamar a .toString() en
            // null)
            producto.setPrecio_venta(new BigDecimal(precioVentaObj.toString()));
        }

        // 2c. Campos de Marca
        String marca = (String) payload.get("marca");
        if (marca != null) {
            producto.setMarca(marca);
        }

        boolean inventarioPayloadPresente = payload.containsKey("cantidad_disponible") ||
                payload.containsKey("stock_minimo") ||
                payload.containsKey("ubicacion");

        Inventario inventario = producto.getInventario();

        if (inventarioPayloadPresente) {

            if (inventario == null) {
                inventario = new Inventario();
                inventario.setProducto(producto);
                producto.setInventario(inventario);
            }

            if (payload.containsKey("cantidad_disponible")) {
                inventario.setCantidad_disponible(((Number) payload.get("cantidad_disponible")).intValue());
            }
            if (payload.containsKey("stock_minimo")) {
                inventario.setStock_minimo(((Number) payload.get("stock_minimo")).intValue());
            }
            if (payload.containsKey("ubicacion")) {
                inventario.setUbicacion((String) payload.get("ubicacion"));
            }
        }

        return productosRepository.save(producto);
    }
}
