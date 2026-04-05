package com.siontrack.siontrack.services;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siontrack.siontrack.DTO.Request.ProductosRequestDTO;
import com.siontrack.siontrack.DTO.Response.AlertaStockDTO;
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

        if (dto.getCantidad_disponible() != null || dto.getStock_minimo() != null) {
            Inventario inventario = new Inventario();
            inventario.setCantidad_disponible(dto.getCantidad_disponible() != null ? dto.getCantidad_disponible() : 0);
            inventario.setStock_minimo(dto.getStock_minimo() != null ? dto.getStock_minimo() : 10);

            inventario.setProducto(producto);
            producto.setInventario(inventario);
        }
        proveedor.getProductos().add(producto);

        Productos savedProducto = productosRepository.save(producto);

        return modelMapper.map(savedProducto, ProductosResponseDTO.class);
    }

    /**
     * Upsert: si el producto ya existe (por código o nombre) lo actualiza,
     * si no existe lo crea. Devuelve true si fue actualización, false si fue creación.
     */
    @Transactional
    public boolean upsertProducto(ProductosRequestDTO dto) {
        // Buscar producto existente por código primero, luego por nombre
        Optional<Productos> existente = Optional.empty();

        if (dto.getCodigo_producto() != null && !dto.getCodigo_producto().isBlank()) {
            existente = productosRepository.findByCodigoProducto(dto.getCodigo_producto());
        }
        if (existente.isEmpty() && dto.getNombre() != null && !dto.getNombre().isBlank()) {
            existente = productosRepository.findByNombreIgnoreCase(dto.getNombre());
        }

        if (existente.isPresent()) {
            // Actualizar campos del producto existente
            Productos producto = existente.get();

            if (dto.getPrecio_compra() != null) producto.setPrecio_compra(dto.getPrecio_compra());
            if (dto.getPrecio_venta() != null) producto.setPrecio_venta(dto.getPrecio_venta());
            if (dto.getFecha_compra() != null) producto.setFecha_compra(dto.getFecha_compra());

            // Actualizar inventario si se proporcionan datos de stock
            if (dto.getCantidad_disponible() != null || dto.getStock_minimo() != null ) {
                Inventario inventario = producto.getInventario();
                if (inventario == null) {
                    inventario = new Inventario();
                    inventario.setProducto(producto);
                    producto.setInventario(inventario);
                }
                if (dto.getCantidad_disponible() != null) {
                    inventario.setCantidad_disponible(dto.getCantidad_disponible());
                }
                if (dto.getStock_minimo() != null) inventario.setStock_minimo(dto.getStock_minimo());
            }

            productosRepository.save(producto);
            return true; // fue actualización
        }

        // No existe → crear nuevo
        crearProducto(dto);
        return false; // fue creación
    }

    @Transactional
    public ProductosResponseDTO actualizarProducto(Integer id, ProductosRequestDTO dto) {

        Productos productoExistente = productosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));

        LocalDate fechaCompraActual = productoExistente.getFecha_compra();
        modelMapper.map(dto, productoExistente);
        if (dto.getFecha_compra() == null) {
            productoExistente.setFecha_compra(fechaCompraActual);
        }

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
        boolean inventarioDataProvided = dto.getCantidad_disponible() != null || dto.getStock_minimo() != null;

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
            
        }

        System.out.println("ID before save: " + productoExistente.getProducto_id());
        Productos productoActualizado = productosRepository.save(productoExistente);

        return modelMapper.map(productoActualizado, ProductosResponseDTO.class);
    }

    @Transactional
    public void actualizarSoloStock(Integer id, Integer cantidad) {
        Productos producto = productosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));

        Inventario inventario = producto.getInventario();
        if (inventario == null) {
            inventario = new Inventario();
            inventario.setProducto(producto);
            producto.setInventario(inventario);
        }
        inventario.setCantidad_disponible(cantidad);
        productosRepository.save(producto);
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

        // 1. Obtener top 5 productos populares (periodo general)
        Pageable top5 = PageRequest.of(0, 5);
        LocalDate fechaInicio = LocalDate.of(2000, 1, 1);
        List<ProductoPopularDTO> populares = detalleServicioRepository
                .encontrarProductsoPopulares(fechaInicio, top5);

        Map<Integer, int[]> popularMap = new HashMap<>();
        for (int i = 0; i < populares.size(); i++) {
            ProductoPopularDTO p = populares.get(i);
            popularMap.put(p.getProductoId(), new int[]{ i + 1, p.getTotalVendido().intValue() });
        }

        // 2. Obtener todos los productos con inventario y evaluar alertas
        return productosRepository.findAll().stream()
            .filter(producto -> producto.getInventario() != null)
            .filter(producto -> {
                int cantidad = producto.getInventario().getCantidad_disponible();
                int minimo = producto.getInventario().getStock_minimo();
                if (minimo <= 0) return false;

                // Incluir productos hasta 1.5x el mínimo (nivel ADVERTENCIA)
                return cantidad <= (int)(minimo * 1.5);
            })
            .map(producto -> {
                Inventario inv = producto.getInventario();
                int cantidad = inv.getCantidad_disponible();
                int minimo = inv.getStock_minimo();
                int necesita = Math.max(minimo - cantidad, 0);

                // --- Determinar nivel de alerta ---
                String nivel;
                int nivelNumerico; // Para ordenamiento: mayor = más urgente

                if (cantidad == 0) {
                    nivel = "AGOTADO";
                    nivelNumerico = 40;
                } else if (cantidad <= (int)(minimo * 0.3)) {
                    nivel = "CRITICO";
                    nivelNumerico = 30;
                } else if (cantidad <= minimo) {
                    nivel = "BAJO";
                    nivelNumerico = 20;
                } else {
                    // cantidad > minimo && cantidad <= minimo * 1.5
                    nivel = "ADVERTENCIA";
                    nivelNumerico = 10;
                    necesita = 0; // Aún no necesita reabastecimiento urgente
                }

                // --- Cruce con popularidad ---
                boolean esPopular = popularMap.containsKey(producto.getProducto_id());
                Integer ranking = null;
                Long totalVendido = null;

                if (esPopular) {
                    int[] data = popularMap.get(producto.getProducto_id());
                    ranking = data[0];
                    totalVendido = (long) data[1];
                }

                // Prioridad compuesta:
                // Base: nivelNumerico (10-40)
                // Bonus popularidad: +50 si es top 1-2, +40 si es top 3, +30 si es top 4-5
                int prioridad = nivelNumerico;
                if (esPopular && ranking != null) {
                    if (ranking <= 2) prioridad += 50;
                    else if (ranking == 3) prioridad += 40;
                    else prioridad += 30;
                }

                // --- Construir DTO ---
                AlertaStockDTO dto = new AlertaStockDTO();
                dto.setProductoId(producto.getProducto_id());
                dto.setNombre(producto.getNombre());
                dto.setCategoria(producto.getCategoria());
                dto.setCantidadDisponible(cantidad);
                dto.setStockMinimo(minimo);
                dto.setUbicacion(inv.getUbicacion());
                dto.setNivelAlerta(nivel);
                dto.setCantidadNecesaria(necesita);
                dto.setEsPopular(esPopular);
                dto.setRankingPopular(ranking);
                dto.setTotalVendido(totalVendido);
                dto.setPrioridadCompuesta(prioridad);

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
            // Ordenar por prioridad compuesta descendente, luego por cantidad ascendente
            .sorted((a, b) -> {
                int cmp = b.getPrioridadCompuesta().compareTo(a.getPrioridadCompuesta());
                if (cmp != 0) return cmp;
                return a.getCantidadDisponible().compareTo(b.getCantidadDisponible());
            })
            .collect(Collectors.toList());
    }}