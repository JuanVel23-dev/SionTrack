package com.siontrack.siontrack.services;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

/**
 * Gestiona el catálogo de productos, su inventario y las alertas de stock.
 *
 * <p>El mapeo base de {@link Productos} → {@link ProductosResponseDTO} lo hace ModelMapper,
 * pero los campos de inventario ({@code cantidad_disponible}, {@code stock_minimo}) y el
 * flag {@code alerta_stock} se resuelven manualmente porque ModelMapper en modo STRICT
 * no navega la relación {@code producto → inventario → campo}.
 *
 * <p>Las alertas de stock se calculan cruzando los productos con stock bajo contra el
 * ranking de popularidad para producir una prioridad compuesta que ordena la lista
 * por urgencia de reabastecimiento.
 */
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

    /**
     * Devuelve la lista completa de productos con sus datos de inventario y alerta de stock.
     *
     * @return lista de todos los productos mapeados a DTO
     */
    @Transactional(readOnly = true)
    public List<ProductosResponseDTO> obtenerListaProductos() {
        return productosRepository.findAll().stream()
                .map(producto -> {
                    ProductosResponseDTO dto = modelMapper.map(producto, ProductosResponseDTO.class);
                    if (producto.getInventario() != null) {
                        dto.setCantidad_disponible(producto.getInventario().getCantidad_disponible());
                        dto.setStock_minimo(producto.getInventario().getStock_minimo());
                        dto.setAlerta_stock(
                            dto.getStock_minimo() != null && dto.getCantidad_disponible() != null
                            && dto.getCantidad_disponible() <= dto.getStock_minimo());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Devuelve los productos paginados, con búsqueda opcional por nombre o código.
     *
     * @param pageable  configuración de paginación y orden
     * @param busqueda  término de búsqueda (puede ser nulo o vacío para listar todos)
     * @return página de productos mapeados a DTO
     */
    @Transactional(readOnly = true)
    public Page<ProductosResponseDTO> obtenerListaProductosPaginado(Pageable pageable, String busqueda) {
        Page<Productos> page;
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            page = productosRepository.buscarPaginado(busqueda.trim(), pageable);
        } else {
            page = productosRepository.findAllOrderByIdDesc(pageable);
        }
        return page.map(producto -> {
            ProductosResponseDTO dto = modelMapper.map(producto, ProductosResponseDTO.class);
            if (producto.getInventario() != null) {
                dto.setCantidad_disponible(producto.getInventario().getCantidad_disponible());
                dto.setStock_minimo(producto.getInventario().getStock_minimo());
                dto.setAlerta_stock(
                    dto.getStock_minimo() != null && dto.getCantidad_disponible() != null
                    && dto.getCantidad_disponible() <= dto.getStock_minimo());
            }
            return dto;
        });
    }

    /**
     * Obtiene un producto por su ID, incluyendo sus datos de inventario y alerta de stock.
     *
     * @param id ID del producto
     * @return DTO del producto
     * @throws RuntimeException si el producto no existe
     */
    @Transactional(readOnly = true)
    public ProductosResponseDTO obtenerProductoByID(Integer id) {
        Productos producto = productosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));

        ProductosResponseDTO dto = modelMapper.map(producto, ProductosResponseDTO.class);

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

    /**
     * Crea un nuevo producto con su inventario inicial.
     * Si el DTO no incluye {@code cantidad_disponible} ni {@code stock_minimo},
     * no se crea el registro de inventario.
     *
     * @param dto datos del producto a crear
     * @return DTO del producto creado
     * @throws RuntimeException si el proveedor indicado no existe
     */
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
     * Crea o actualiza un producto según si ya existe en la base de datos.
     * La búsqueda prioriza el código de producto; si no hay código, busca por nombre.
     *
     * @param dto datos del producto
     * @return {@code true} si fue una actualización, {@code false} si fue una creación
     * @throws RuntimeException si el proveedor indicado no existe
     */
    @Transactional
    public boolean upsertProducto(ProductosRequestDTO dto) {
        Optional<Productos> existente = Optional.empty();

        if (dto.getCodigo_producto() != null && !dto.getCodigo_producto().isBlank()) {
            existente = productosRepository.findByCodigoProducto(dto.getCodigo_producto());
        }
        if (existente.isEmpty() && dto.getNombre() != null && !dto.getNombre().isBlank()) {
            existente = productosRepository.findByNombreIgnoreCase(dto.getNombre());
        }

        if (existente.isPresent()) {
            Productos producto = existente.get();

            if (dto.getPrecio_compra() != null) producto.setPrecio_compra(dto.getPrecio_compra());
            if (dto.getPrecio_venta() != null) producto.setPrecio_venta(dto.getPrecio_venta());
            if (dto.getFecha_compra() != null) producto.setFecha_compra(dto.getFecha_compra());
            if (dto.getProveedor_id() != null) {
                Proveedores proveedor = proveedorRepository.findById(dto.getProveedor_id())
                        .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + dto.getProveedor_id()));
                producto.setProveedor(proveedor);
            }

            if (dto.getCantidad_disponible() != null || dto.getStock_minimo() != null) {
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
            return true;
        }

        // No existe → crear nuevo
        crearProducto(dto);
        return false;
    }

    /**
     * Actualiza todos los campos de un producto existente.
     * Si el DTO no incluye {@code fecha_compra}, se conserva la fecha actual del producto.
     *
     * @param id  ID del producto a actualizar
     * @param dto datos nuevos del producto
     * @return DTO del producto actualizado
     * @throws RuntimeException si el producto o el proveedor no existen
     */
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

    /**
     * Actualiza únicamente el stock disponible de un producto.
     * Si el producto no tiene inventario, se crea el registro.
     *
     * @param id       ID del producto
     * @param cantidad nueva cantidad disponible en inventario
     * @throws RuntimeException si el producto no existe
     */
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

    /**
     * Elimina un producto por su ID.
     *
     * @param id ID del producto a eliminar
     * @throws RuntimeException si el producto no existe
     */
    @Transactional
    public void borrarProducto(Integer id) {
        if (!productosRepository.existsById(id)) {
            throw new RuntimeException("Producto no encontrado con ID: " + id);
        }
        productosRepository.deleteById(id);
    }

    /**
     * Devuelve los productos más vendidos en el período indicado, ordenados por cantidad.
     *
     * @param limite  número máximo de productos a retornar
     * @param periodo período de análisis: {@code "semana"}, {@code "mes"},
     *                {@code "trimestre"}, {@code "anio"} o {@code "general"}
     * @return lista de DTOs con nombre, categoría y total vendido
     */
    @Transactional
    public List<ProductoPopularDTO> obtenerListaPopulares(int limite, String periodo){

        Pageable pageable = PageRequest.of(0, limite);
        LocalDate fechaInicio;
        LocalDate hoy = LocalDate.now();

        switch (periodo.toLowerCase()) {
            case "semana":
                fechaInicio = hoy.minusWeeks(1);
                break;
            case "mes":
                fechaInicio = hoy.minusMonths(1);
                break;
            case "trimestre":
                fechaInicio = hoy.minusMonths(3);
                break;
            case "anio":
                fechaInicio = hoy.minusYears(1);
                break;
            case "general":
            default:
                // Para el histórico general se usa una fecha de inicio muy antigua
                fechaInicio = LocalDate.of(2000, 1, 1);
                break;
        }

        return detalleServicioRepository.encontrarProductsoPopulares(fechaInicio, pageable);
    }

    /**
     * Devuelve las alertas de stock para los productos con inventario bajo, ordenadas
     * por prioridad compuesta descendente.
     *
     * <p>La prioridad compuesta se calcula así:
     * <ul>
     *   <li>Base por nivel de alerta: AGOTADO=40, CRÍTICO=30, BAJO=20, ADVERTENCIA=10.</li>
     *   <li>Bonus por popularidad: +50 si es top 1–2, +40 si es top 3, +30 si es top 4–5.</li>
     * </ul>
     * Dentro del mismo nivel de prioridad, se ordena por cantidad disponible ascendente.
     *
     * @return lista completa de alertas de stock ordenada por urgencia
     */
    @Transactional(readOnly = true)
    public List<AlertaStockDTO> obtenerAlertasStock() {

        Pageable top5 = PageRequest.of(0, 5);
        LocalDate fechaInicio = LocalDate.of(2000, 1, 1);
        List<ProductoPopularDTO> populares = detalleServicioRepository
                .encontrarProductsoPopulares(fechaInicio, top5);

        Map<Integer, int[]> popularMap = new HashMap<>();
        for (int i = 0; i < populares.size(); i++) {
            ProductoPopularDTO p = populares.get(i);
            popularMap.put(p.getProductoId(), new int[]{ i + 1, p.getTotalVendido().intValue() });
        }

        return productosRepository.findProductosNecesitanRestock().stream()
            .map(producto -> {
                Inventario inv = producto.getInventario();
                int cantidad = inv.getCantidad_disponible();
                int minimo = inv.getStock_minimo();
                int necesita = Math.max(minimo - cantidad, 0);

                String nivel;
                int nivelNumerico;

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
                    nivel = "ADVERTENCIA";
                    nivelNumerico = 10;
                    necesita = 0;
                }

                boolean esPopular = popularMap.containsKey(producto.getProducto_id());
                Integer ranking = null;
                Long totalVendido = null;

                if (esPopular) {
                    int[] data = popularMap.get(producto.getProducto_id());
                    ranking = data[0];
                    totalVendido = (long) data[1];
                }

                // Prioridad compuesta: base del nivel + bonus por popularidad
                // Base: AGOTADO=40, CRÍTICO=30, BAJO=20, ADVERTENCIA=10
                // Bonus: +50 si top 1–2, +40 si top 3, +30 si top 4–5
                int prioridad = nivelNumerico;
                if (esPopular && ranking != null) {
                    if (ranking <= 2) prioridad += 50;
                    else if (ranking == 3) prioridad += 40;
                    else prioridad += 30;
                }

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
    }

    /**
     * Versión paginada de {@link #obtenerAlertasStock()}.
     * Obtiene la lista completa ordenada y devuelve el subconjunto solicitado.
     *
     * @param page número de página (base 0)
     * @param size tamaño de la página
     * @return página con el subconjunto de alertas ordenadas
     */
    @Transactional(readOnly = true)
    public Page<AlertaStockDTO> obtenerAlertasStockPaginado(int page, int size) {
        List<AlertaStockDTO> todas = obtenerAlertasStock();
        int fromIndex = page * size;
        if (fromIndex >= todas.size()) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size), todas.size());
        }
        int toIndex = Math.min(fromIndex + size, todas.size());
        return new PageImpl<>(todas.subList(fromIndex, toIndex), PageRequest.of(page, size), todas.size());
    }
}
