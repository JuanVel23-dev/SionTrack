package com.siontrack.siontrack.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

/**
 * Gestiona el ciclo de vida de los servicios prestados a los clientes.
 *
 * <p>Al crear un servicio:
 * <ul>
 *   <li>Se validan las entidades relacionadas (cliente y, si aplica, vehículo).</li>
 *   <li>Se procesa cada detalle: se descuenta el stock del inventario y se congela
 *       el precio unitario en el momento de la venta.</li>
 *   <li>Se calcula el total del servicio sumando los subtotales de cada ítem.</li>
 *   <li>Se actualiza el kilometraje del vehículo si se proporcionó.</li>
 *   <li>Se delega en {@link RecordatorioService} para crear los recordatorios
 *       del próximo servicio.</li>
 * </ul>
 */
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
     * Mapea {@link Servicios} → {@link ServicioResponseDTO} resolviendo manualmente
     * las relaciones cuyo nombre difiere entre entidad y DTO:
     * {@code entity.vehiculos} → {@code dto.vehiculo} y
     * {@code entity.clientes} → {@code dto.cliente}.
     *
     * <p>El nombre del producto en cada detalle también se resuelve manualmente porque
     * ModelMapper en modo STRICT no puede navegar la ruta
     * {@code detalle → producto → nombre}.
     */
    private ServicioResponseDTO mapearServicioADTO(Servicios servicio) {
        ServicioResponseDTO dto = modelMapper.map(servicio, ServicioResponseDTO.class);

        if (servicio.getVehiculos() != null) {
            dto.setVehiculo(modelMapper.map(servicio.getVehiculos(), VehiculosResponseDTO.class));
        }

        if (servicio.getClientes() != null) {
            dto.setCliente(modelMapper.map(servicio.getClientes(), ClienteResponseDTO.class));
        }

        if (servicio.getDetalles() != null) {
            List<DetalleServicioResponseDTO> detallesDTO = servicio.getDetalles().stream()
                    .map(detalle -> {
                        DetalleServicioResponseDTO d = new DetalleServicioResponseDTO();
                        d.setDetalle_id(detalle.getDetalle_id());
                        d.setCantidad(detalle.getCantidad());
                        d.setPrecio_unitario_congelado(detalle.getPrecio_unitario_congelado());
                        d.setTipoItem(detalle.getTipo() != null ? detalle.getTipo().name() : "PRODUCTO");
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

    /**
     * Crea un nuevo servicio con todos sus detalles de productos/mano de obra.
     *
     * <p>El precio unitario de cada ítem se congela en el momento de la creación:
     * si el DTO trae precio, se usa ese; si no, se usa el precio de venta actual del producto.
     * El stock se descuenta usando techo ({@code CEILING}) para cantidades decimales,
     * de modo que 1.5 unidades descuenta 2 unidades del inventario entero.
     *
     * @param dto datos del servicio a crear, incluyendo sus detalles
     * @return DTO del servicio creado con total calculado
     * @throws RuntimeException si el cliente, vehículo o algún producto no existen,
     *                          o si hay stock insuficiente
     */
    @Transactional
    public ServicioResponseDTO crearServicio(ServicioRequestDTO dto) {

        Clientes cliente = clienteRepository.findById(dto.getCliente_id())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + dto.getCliente_id()));

        // El vehículo es obligatorio únicamente para servicios de MANO_DE_OBRA
        if ("MANO_DE_OBRA".equals(dto.getTipo_servicio()) && dto.getVehiculo_id() == null) {
            throw new RuntimeException("El vehiculo es obligatorio para servicios de mano de obra");
        }

        Vehiculos vehiculo = null;
        if (dto.getVehiculo_id() != null) {
            vehiculo = vehiculoRepository.findById(dto.getVehiculo_id())
                    .orElseThrow(() -> new RuntimeException("Vehiculo no encontrado con ID: " + dto.getVehiculo_id()));
        }

        Servicios servicio = new Servicios();
        servicio.setFecha_servicio(dto.getFecha_servicio());
        servicio.setKilometraje_servicio(dto.getKilometraje_servicio());
        servicio.setTipo_servicio(dto.getTipo_servicio() != null ? dto.getTipo_servicio() : "PRODUCTO");
        servicio.setObservaciones(dto.getObservaciones());
        servicio.setCreado_en(LocalDateTime.now());
        servicio.setClientes(cliente);
        servicio.setVehiculos(vehiculo);

        if (vehiculo != null && servicio.getKilometraje_servicio() != null) {
            actualizarKilometrajeVehiculo(vehiculo, servicio.getKilometraje_servicio());
        }

        BigDecimal totalServicio = BigDecimal.ZERO;
        List<Detalle_Servicio> listaDetalles = new ArrayList<>();

        if (dto.getDetalles() != null && !dto.getDetalles().isEmpty()) {

            for (DetalleServicioRequestDTO detalleDto : dto.getDetalles()) {

                Productos producto = productosRepository.findById(detalleDto.getProducto_id())
                        .orElseThrow(
                                () -> new RuntimeException("Producto no encontrado: " + detalleDto.getProducto_id()));

                Detalle_Servicio detalle = new Detalle_Servicio();
                detalle.setProducto(producto);
                detalle.setServicio(servicio);
                detalle.setCantidad(detalleDto.getCantidad());

                // Precio congelado: se usa el del DTO si viene; si no, el precio de venta actual del producto
                BigDecimal precioFinal = (detalleDto.getPrecio_unitario_congelado() != null)
                        ? detalleDto.getPrecio_unitario_congelado()
                        : producto.getPrecio_venta();

                detalle.setPrecio_unitario_congelado(precioFinal);
                detalle.setTipo(Detalle_Servicio.tipoItem.valueOf(detalleDto.getTipoItem()));

                // Descuento de inventario: se usa CEILING para cantidades decimales
                // (ej: 1.5 litros descuenta 2 unidades del stock entero)
                if (producto.getInventario() != null) {
                    int cantidadADescontar = detalleDto.getCantidad()
                            .setScale(0, java.math.RoundingMode.CEILING).intValue();
                    int nuevaCantidad = producto.getInventario().getCantidad_disponible() - cantidadADescontar;
                    if (nuevaCantidad < 0)
                        throw new RuntimeException("Stock insuficiente para: " + producto.getNombre());
                    producto.getInventario().setCantidad_disponible(nuevaCantidad);
                }

                BigDecimal subtotal = precioFinal.multiply(detalleDto.getCantidad());
                totalServicio = totalServicio.add(subtotal);

                listaDetalles.add(detalle);
            }
        }

        servicio.setDetalles(listaDetalles);
        servicio.setTotal(totalServicio);

        // Cascade guarda automáticamente los detalles junto con el servicio
        Servicios servicioGuardado = serviciosRepository.save(servicio);

        try {
            recordatorioService.procesarServicioParaRecordatorios(servicioGuardado);
        } catch (Exception e) {
            System.err.println("Error creando recordatorio: " + e.getMessage());
        }

        return mapearServicioADTO(servicioGuardado);
    }

    /**
     * Devuelve todos los servicios registrados sin paginación.
     *
     * @return lista de todos los servicios mapeados a DTO
     */
    public List<ServicioResponseDTO> obtenerTodos() {
        return serviciosRepository.findAll().stream()
                .map(this::mapearServicioADTO)
                .collect(Collectors.toList());
    }

    /**
     * Devuelve los servicios paginados, con búsqueda opcional por nombre de cliente o placa.
     *
     * @param pageable configuración de paginación y orden
     * @param busqueda término de búsqueda (puede ser nulo o vacío para listar todos)
     * @return página de servicios mapeados a DTO
     */
    public Page<ServicioResponseDTO> obtenerTodosPaginado(Pageable pageable, String busqueda) {
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            return serviciosRepository.buscarPaginado(busqueda.trim(), pageable)
                    .map(this::mapearServicioADTO);
        }
        return serviciosRepository.findAllOrderByIdDesc(pageable)
                .map(this::mapearServicioADTO);
    }

    /**
     * Obtiene un servicio por su ID, incluyendo sus detalles de productos.
     *
     * @param id ID del servicio
     * @return DTO del servicio con sus detalles
     * @throws RuntimeException si el servicio no existe
     */
    public ServicioResponseDTO obtenerServicioPorId(Integer id) {
        Servicios servicio = serviciosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado con ID: " + id));
        return mapearServicioADTO(servicio);
    }

    /**
     * Elimina un servicio y sus detalles asociados (vía cascade).
     *
     * @param idServicio ID del servicio a eliminar
     * @throws RuntimeException si el servicio no existe
     */
    public void eliminarServicio(Integer idServicio) {
        if (!serviciosRepository.existsById(idServicio)) {
            throw new RuntimeException("El servicio con ID " + idServicio + " no existe.");
        }
        serviciosRepository.deleteById(idServicio);
    }

    /**
     * Actualiza el kilometraje actual de un vehículo si el valor no está vacío.
     */
    private void actualizarKilometrajeVehiculo(Vehiculos vehiculo, String kilometrajeNuevo) {
        if (kilometrajeNuevo == null || kilometrajeNuevo.trim().isEmpty()) {
            return;
        }
        vehiculo.setKilometraje_actual(kilometrajeNuevo.trim());
        vehiculoRepository.save(vehiculo);
    }
}
