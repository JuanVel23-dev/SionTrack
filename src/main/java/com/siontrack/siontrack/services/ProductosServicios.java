package com.siontrack.siontrack.services;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import com.siontrack.siontrack.DTO.Response.AlertaStockDTO;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siontrack.siontrack.DTO.Request.ProductosRequestDTO;
import com.siontrack.siontrack.DTO.Response.ProductoPopularDTO;
import com.siontrack.siontrack.DTO.Response.ProductosResponseDTO;

import com.siontrack.siontrack.models.Inventario;
import com.siontrack.siontrack.models.Productos;
import com.siontrack.siontrack.models.Proveedores;
import com.siontrack.siontrack.repository.DetalleServicioRepository;
import com.siontrack.siontrack.repository.ProductosRepository;
import com.siontrack.siontrack.repository.ProveedoresRepository;

@Service
public class ProductosServicios {

    @Autowired
    private ProductosRepository productosRepository;

    @Autowired
    private ProveedoresRepository proveedorRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private DetalleServicioRepository detalleServicioRepository;

    @Transactional(readOnly = true)
    public List<ProductosResponseDTO> obtenerListaProductos() {
        return productosRepository.findAll().stream()
                .map(producto -> {
                // 1. Mapeo automático de campos base
                ProductosResponseDTO dto = modelMapper.map(producto, ProductosResponseDTO.class);
                
                // 2. Lógica manual para Inventario y Alertas
                // Verificamos si el producto tiene inventario asociado
                if (producto.getInventario() != null) {
                    
                    // Aseguramos el mapeo de datos (por si ModelMapper falló con los anidados)
                    dto.setCantidad_disponible(producto.getInventario().getCantidad_disponible());
                    dto.setStock_minimo(producto.getInventario().getStock_minimo());
                    dto.setUbicacion(producto.getInventario().getUbicacion());

                    // --- CÁLCULO DE LA ALERTA ---
                    // Si hay stock mínimo definido y la cantidad actual es menor o igual
                    if (dto.getStock_minimo() != null && dto.getCantidad_disponible() != null 
                        && dto.getCantidad_disponible() <= dto.getStock_minimo()) {
                        
                        dto.setAlerta_stock(true); // ¡ACTIVAR ALERTA!
                    } else {
                        dto.setAlerta_stock(false);
                    }
                }
                return dto;
            })
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductosResponseDTO obtenerProductoByID(Integer id) {
        Productos producto = productosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));

        ProductosResponseDTO dto = modelMapper.map(producto, ProductosResponseDTO.class);

        // Aplicar la misma lógica para el producto individual
        if (producto.getInventario() != null) {
            dto.setCantidad_disponible(producto.getInventario().getCantidad_disponible());
            dto.setStock_minimo(producto.getInventario().getStock_minimo());
            dto.setUbicacion(producto.getInventario().getUbicacion());

            if (dto.getStock_minimo() != null && dto.getCantidad_disponible() != null 
                && dto.getCantidad_disponible() <= dto.getStock_minimo()) {
                dto.setAlerta_stock(true);
            }
        }
        
        return dto;
    }

    @Transactional
    public ProductosResponseDTO crearProducto(ProductosRequestDTO dto) {
        Proveedores proveedor = proveedorRepository.findById(dto.getProveedor_id())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + dto.getProveedor_id()));

        Productos producto = modelMapper.map(dto, Productos.class);

        producto.setProveedor(proveedor);

        if (dto.getCantidad_disponible() != null) {
            Inventario inventario = new Inventario();
            inventario.setCantidad_disponible(dto.getCantidad_disponible());
            inventario.setStock_minimo(dto.getStock_minimo());
            inventario.setUbicacion(dto.getUbicacion());

            inventario.setProducto(producto);
            producto.setInventario(inventario);
        }
        proveedor.getProductos().add(producto);

        Productos savedProducto = productosRepository.save(producto);

        return modelMapper.map(savedProducto, ProductosResponseDTO.class);
    }

    @Transactional
    public ProductosResponseDTO actualizarProducto(Integer id, ProductosRequestDTO dto) {

        Productos productoExistente = productosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));

        modelMapper.map(dto, productoExistente);

        if (dto.getProveedor_id() != null &&
                (productoExistente.getProveedor() == null
                        || !dto.getProveedor_id().equals(productoExistente.getProveedor().getProveedor_id()))) {
            Proveedores nuevoProveedor = proveedorRepository.findById(dto.getProveedor_id())
                    .orElseThrow(() -> new RuntimeException("Proveedor no encontrado: " + dto.getProveedor_id()));

            if (productoExistente.getProveedor() != null) {
                productoExistente.getProveedor().getProductos().remove(productoExistente);
            }
            nuevoProveedor.getProductos().add(productoExistente);
            productoExistente.setProveedor(nuevoProveedor);
        }

        Inventario inventario = productoExistente.getInventario();
        boolean inventarioDataProvided = dto.getCantidad_disponible() != null || dto.getStock_minimo() != null
                || dto.getUbicacion() != null;

        if (inventarioDataProvided) {
            if (inventario == null) {
                inventario = new Inventario();
                inventario.setProducto(productoExistente);
                productoExistente.setInventario(inventario);
            }
            if (dto.getCantidad_disponible() != null) {
                inventario.setCantidad_disponible(dto.getCantidad_disponible());
            }
            if (dto.getStock_minimo() != null) {
                inventario.setStock_minimo(dto.getStock_minimo());
            }
            if (dto.getUbicacion() != null) {
                inventario.setUbicacion(dto.getUbicacion());
            }
        }

        System.out.println("ID before save: " + productoExistente.getProducto_id());
        Productos productoActualizado = productosRepository.save(productoExistente);

        return modelMapper.map(productoActualizado, ProductosResponseDTO.class);
    }

    @Transactional
    public void borrarProducto(Integer id) {
        if (!productosRepository.existsById(id)) {
            throw new RuntimeException("Producto no encontrado con ID: " + id);
        }
        productosRepository.deleteById(id);
    }

    @Transactional
    public List<ProductoPopularDTO> obtenerListaPopulares(int limite, String periodo){

        Pageable pageable = PageRequest.of(0, limite);
        LocalDate fechaInicio;
        LocalDate hoy = LocalDate.now();

        // Calculamos la fecha de corte según lo que pida el usuario
        switch (periodo.toLowerCase()) {
            case "semana":
                fechaInicio = hoy.minusWeeks(1); // Últimos 7 días
                break;
            case "mes":
                fechaInicio = hoy.minusMonths(1); // Último mes
                break;
            case "trimestre":
                fechaInicio = hoy.minusMonths(3); // Últimos 3 meses
                break;
            case "anio":
                fechaInicio = hoy.minusYears(1); // Último año
                break;
            case "general":
            default:
                // Para "general", ponemos una fecha muy antigua (ej. año 2000)
                fechaInicio = LocalDate.of(2000, 1, 1);
                break;
        }

        return detalleServicioRepository.encontrarProductsoPopulares(fechaInicio, pageable);
    }
 
    @Transactional(readOnly = true)
public List<AlertaStockDTO> obtenerAlertasStock() {
    return productosRepository.findAll().stream()
        .filter(producto -> producto.getInventario() != null)
        .filter(producto -> {
            int cantidad = producto.getInventario().getCantidad_disponible();
            int minimo = producto.getInventario().getStock_minimo();
            return minimo > 0 && cantidad <= minimo;
        })
        .map(producto -> {
            Inventario inv = producto.getInventario();
            int cantidad = inv.getCantidad_disponible();
            int minimo = inv.getStock_minimo();
            int necesita = minimo - cantidad;

            String nivel;
            if (cantidad == 0) {
                nivel = "AGOTADO";
            } else if (cantidad <= (minimo * 0.5)) {
                nivel = "CRITICO";
            } else {
                nivel = "BAJO";
            }

            AlertaStockDTO dto = new AlertaStockDTO();
            dto.setProductoId(producto.getProducto_id());
            dto.setNombre(producto.getNombre());
            dto.setCategoria(producto.getCategoria());
            dto.setMarca(producto.getMarca());
            dto.setCantidadDisponible(cantidad);
            dto.setStockMinimo(minimo);
            dto.setUbicacion(inv.getUbicacion());
            dto.setNivelAlerta(nivel);
            dto.setCantidadNecesaria(Math.max(necesita, 0));

            // Datos del proveedor
            if (producto.getProveedor() != null) {
                var prov = producto.getProveedor();
                dto.setProveedorId(prov.getProveedor_id());
                dto.setProveedorNombre(prov.getNombre());
                dto.setProveedorTelefono(prov.getTelefono());
                dto.setProveedorEmail(prov.getEmail());
                dto.setProveedorDireccion(prov.getDireccion());
            }

            return dto;
        })
        .sorted((a, b) -> a.getCantidadDisponible().compareTo(b.getCantidadDisponible()))
        .collect(Collectors.toList());
}
}