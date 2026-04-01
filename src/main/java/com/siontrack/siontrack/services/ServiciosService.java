package com.siontrack.siontrack.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.siontrack.siontrack.DTO.Request.DetalleServicioRequestDTO;
import com.siontrack.siontrack.DTO.Request.ServicioRequestDTO;
import com.siontrack.siontrack.DTO.Response.ClienteResponseDTO;
import com.siontrack.siontrack.DTO.Response.DetalleServicioResponseDTO;
import com.siontrack.siontrack.DTO.Response.ServicioResponseDTO;
import com.siontrack.siontrack.DTO.Response.VehiculosResponseDTO;
import com.siontrack.siontrack.models.Clientes;
import com.siontrack.siontrack.models.Detalle_Servicio;
import com.siontrack.siontrack.models.Productos;
import com.siontrack.siontrack.models.Servicios;
import com.siontrack.siontrack.models.Vehiculos;
import com.siontrack.siontrack.repository.ClienteRepository;
import com.siontrack.siontrack.repository.ProductosRepository;
import com.siontrack.siontrack.repository.ServiciosRepository;
import com.siontrack.siontrack.repository.VehiculosRepository;

import jakarta.transaction.Transactional;

@Service
public class ServiciosService {

    @Autowired
    private ServiciosRepository serviciosRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private VehiculosRepository vehiculoRepository;
    @Autowired
    private ProductosRepository productosRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private RecordatorioService recordatorioService;

    /**
     * Mapea una entidad Servicios a ServicioResponseDTO,
     * resolviendo manualmente las relaciones con nombres distintos
     * (entity: vehiculos/clientes → DTO: vehiculo/cliente)
     */
    private ServicioResponseDTO mapearServicioADTO(Servicios servicio) {
        ServicioResponseDTO dto = modelMapper.map(servicio, ServicioResponseDTO.class);

        // Mapeo manual: entity.getVehiculos() → dto.setVehiculo()
        if (servicio.getVehiculos() != null) {
            dto.setVehiculo(modelMapper.map(servicio.getVehiculos(), VehiculosResponseDTO.class));
        }

        // Mapeo manual: entity.getClientes() → dto.setCliente()
        if (servicio.getClientes() != null) {
            dto.setCliente(modelMapper.map(servicio.getClientes(), ClienteResponseDTO.class));
        }

        // Mapeo manual de detalles: ModelMapper no resuelve nombre_producto
        // porque viene de la relación detalle → producto → nombre
        if (servicio.getDetalles() != null) {
            List<DetalleServicioResponseDTO> detallesDTO = servicio.getDetalles().stream()
                    .map(detalle -> {
                        DetalleServicioResponseDTO d = new DetalleServicioResponseDTO();
                        d.setDetalle_id(detalle.getDetalle_id());
                        d.setCantidad(detalle.getCantidad());
                        d.setPrecio_unitario_congelado(detalle.getPrecio_unitario_congelado());
                        d.setTipoItem(detalle.getTipo() != null ? detalle.getTipo().name() : "PRODUCTO");
                        // Nombre del producto desde la relación
                        if (detalle.getProducto() != null) {
                            d.setNombre_producto(detalle.getProducto().getNombre());
                        }
                        return d;
                    })
                    .collect(Collectors.toList());
            dto.setDetalles(detallesDTO);
        }

        return dto;
    }

    @Transactional
    public ServicioResponseDTO crearServicio(ServicioRequestDTO dto) {

        // 1. Validar y Buscar Entidades Padre
        Clientes cliente = clienteRepository.findById(dto.getCliente_id())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + dto.getCliente_id()));

        Vehiculos vehiculo = null;
        if (dto.getVehiculo_id() != null) {
            vehiculo = vehiculoRepository.findById(dto.getVehiculo_id())
                    .orElseThrow(() -> new RuntimeException("Vehículo no encontrado: " + dto.getVehiculo_id()));
        }

        // 2. Crear Entidad Servicio Base
        Servicios servicio = new Servicios();
        servicio.setFecha_servicio(dto.getFecha_servicio());
        servicio.setKilometraje_servicio(dto.getKilometraje_servicio());
        servicio.setTipo_servicio(dto.getTipo_servicio() != null ? dto.getTipo_servicio() : "PRODUCTO");
        servicio.setObservaciones(dto.getObservaciones());
        servicio.setCreado_en(LocalDateTime.now());

        // Asignar Relaciones
        servicio.setClientes(cliente);
        servicio.setVehiculos(vehiculo);

        if (vehiculo != null && servicio.getKilometraje_servicio() != null) {
            actualizarKilometrajeVehiculo(vehiculo, servicio.getKilometraje_servicio());
        }

        // 3. Procesar Detalles y Calcular Total
        BigDecimal totalServicio = BigDecimal.ZERO;
        List<Detalle_Servicio> listaDetalles = new ArrayList<>();

        if (dto.getDetalles() != null && !dto.getDetalles().isEmpty()) {

            for (DetalleServicioRequestDTO detalleDto : dto.getDetalles()) {

                // Buscar el producto
                Productos producto = productosRepository.findById(detalleDto.getProducto_id())
                        .orElseThrow(
                                () -> new RuntimeException("Producto no encontrado: " + detalleDto.getProducto_id()));

                // Crear entidad detalle
                Detalle_Servicio detalle = new Detalle_Servicio();
                detalle.setProducto(producto);
                detalle.setServicio(servicio); // Vincular al padre

                // Datos del detalle
                detalle.setCantidad(detalleDto.getCantidad());

                // PRECIO: Usar el enviado o el del producto actual (congelamiento de precio)
                BigDecimal precioFinal = (detalleDto.getPrecio_unitario_congelado() != null)
                        ? detalleDto.getPrecio_unitario_congelado()
                        : producto.getPrecio_venta();

                detalle.setPrecio_unitario_congelado(precioFinal);

                // Definir tipo (usando el Enum de tu entidad)
                detalle.setTipo(Detalle_Servicio.tipoItem.valueOf(detalleDto.getTipoItem()));

                // Lógica de Stock (Descontar inventario)
                // Se usa ceiling para cantidades decimales: 1.5 unidades descuenta 2 del stock entero
                if (producto.getInventario() != null) {
                    int cantidadADescontar = detalleDto.getCantidad()
                            .setScale(0, java.math.RoundingMode.CEILING).intValue();
                    int nuevaCantidad = producto.getInventario().getCantidad_disponible() - cantidadADescontar;
                    if (nuevaCantidad < 0)
                        throw new RuntimeException("Stock insuficiente para: " + producto.getNombre());
                    producto.getInventario().setCantidad_disponible(nuevaCantidad);
                }

                // Sumar al total (Precio * Cantidad)
                BigDecimal subtotal = precioFinal.multiply(detalleDto.getCantidad());
                totalServicio = totalServicio.add(subtotal);

                listaDetalles.add(detalle);
            }
        }

        servicio.setDetalles(listaDetalles);
        servicio.setTotal(totalServicio); // Asignar el total calculado

        // 4. Guardar (Cascade guardará los detalles)
        Servicios servicioGuardado = serviciosRepository.save(servicio);

        try {
            recordatorioService.procesarServicioParaRecordatorios(servicioGuardado);
        } catch (Exception e) {
            System.err.println("⚠️ Error creando recordatorio: " + e.getMessage());
        }

        // 5. Retornar DTO con mapeo correcto
        return mapearServicioADTO(servicioGuardado);
    }

    // Listar todos
    public List<ServicioResponseDTO> obtenerTodos() {
        return serviciosRepository.findAll().stream()
                .map(this::mapearServicioADTO)
                .collect(Collectors.toList());
    }

    // Obtener por ID
    public ServicioResponseDTO obtenerServicioPorId(Integer id) {
        Servicios servicio = serviciosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado con ID: " + id));
        return mapearServicioADTO(servicio);
    }

    public void eliminarServicio(Integer idServicio) {
        if (!serviciosRepository.existsById(idServicio)) {
            throw new RuntimeException("El servicio con ID " + idServicio + " no existe.");
        }
        serviciosRepository.deleteById(idServicio);
    }

    private void actualizarKilometrajeVehiculo(Vehiculos vehiculo, String kilometrajeNuevo) {
        if (kilometrajeNuevo == null || kilometrajeNuevo.trim().isEmpty()) {
            return;
        }
        vehiculo.setKilometraje_actual(kilometrajeNuevo.trim());
        vehiculoRepository.save(vehiculo);
    }
}