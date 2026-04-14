package com.siontrack.siontrack.services;

import com.siontrack.siontrack.DTO.Request.PromocionesRequestDTO;
import com.siontrack.siontrack.DTO.Response.ClientePreviewDTO;
import com.siontrack.siontrack.models.Clientes;
import com.siontrack.siontrack.models.Detalle_Servicio;
import com.siontrack.siontrack.models.Notificaciones;
import com.siontrack.siontrack.models.Productos;
import com.siontrack.siontrack.models.Vehiculos;
import com.siontrack.siontrack.models.enums.ResultadoEnvioMensaje;
import com.siontrack.siontrack.repository.ClienteRepository;
import com.siontrack.siontrack.repository.DetalleServicioRepository;
import com.siontrack.siontrack.repository.NotificacionesRepository;
import com.siontrack.siontrack.repository.ProductosRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gestiona el envío de notificaciones por WhatsApp a los clientes.
 *
 * <p>Soporta tres tipos de notificación:
 * <ul>
 *   <li><b>RECORDATORIO_SERVICIO</b>: programado automáticamente al crear un servicio.
 *       Se envía cada miércoles al mediodía a los clientes con recordatorios pendientes.</li>
 *   <li><b>CONSENTIMIENTO</b>: solicitud de opt-in enviada masivamente a clientes que aún
 *       no han respondido si desean recibir notificaciones.</li>
 *   <li><b>PROMOCION</b>: envío puntual a los clientes que hayan usado un producto específico,
 *       con posibilidad de selección manual desde el frontend.</li>
 * </ul>
 *
 * <p>Cada envío queda registrado en la tabla de notificaciones con su estado y resultado.
 */
@Service
public class NotificacionesService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesService.class);

    private final NotificacionesRepository notificacionesRepository;
    private final WhatsAppService whatsAppService;
    private final ProductosRepository productosRepository;
    private final DetalleServicioRepository detalleServicioRepository;
    private final ClienteRepository clienteRepository;

    public NotificacionesService(NotificacionesRepository notificacionesRepository,
            WhatsAppService whatsAppService,
            ProductosRepository productosRepository,
            DetalleServicioRepository detalleServicioRepository,
            ClienteRepository clienteRepository) {
        this.notificacionesRepository = notificacionesRepository;
        this.whatsAppService = whatsAppService;
        this.productosRepository = productosRepository;
        this.detalleServicioRepository = detalleServicioRepository;
        this.clienteRepository = clienteRepository;
    }

    /**
     * Job programado que se ejecuta todos los miércoles al mediodía.
     * Recupera los recordatorios pendientes cuya fecha ya venció y los envía por WhatsApp.
     * Actualiza el estado de cada notificación según el resultado del envío.
     */
    @Scheduled(cron = "0 0 12 * * WED")
    @Transactional
    public void enviarNotificacionesProgramadas() {
        log.info("Enviando recordatorios programados...");

        Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());
        List<Notificaciones> pendientes = notificacionesRepository.findNotificacionesPendientes(ahora);

        log.info("Pendientes: {}", pendientes.size());

        int enviados = 0, fallidos = 0;

        for (Notificaciones notificacion : pendientes) {
            ResultadoEnvioMensaje resultado = enviarNotificacion(notificacion);

            notificacion.setFecha_envio(ahora);
            notificacion.setResultadoEnvio(resultado.name());

            if (resultado == ResultadoEnvioMensaje.ENVIADO) {
                notificacion.setEstado("enviado");
                enviados++;
                log.info("Recordatorio enviado: {}", notificacion.getNombreServicio());
            } else {
                notificacion.setEstado("fallido");
                fallidos++;
                log.warn("Falló definitivamente: {}", notificacion.getNombreServicio());
            }

            notificacionesRepository.save(notificacion);
        }

        log.info("Completado — Enviados: {}, Fallidos: {}", enviados, fallidos);
    }

    /**
     * Envía el mensaje de recordatorio de servicio para una notificación concreta.
     * Devuelve {@link ResultadoEnvioMensaje#NUMERO_INVALIDO} si el cliente no tiene teléfono.
     */
    private ResultadoEnvioMensaje enviarNotificacion(Notificaciones notificacion) {
        var cliente = notificacion.getClientes();

        if (cliente.getTelefonos() == null || cliente.getTelefonos().isEmpty()) {
            log.warn("Cliente {} sin teléfono", cliente.getNombre());
            return ResultadoEnvioMensaje.NUMERO_INVALIDO;
        }

        String telefono = cliente.getTelefonos().get(0).getTelefono();
        Vehiculos vehiculo = notificacion.getVehiculo();

        String placa = (vehiculo != null && vehiculo.getPlaca() != null)
                ? vehiculo.getPlaca()
                : "N/A";

        String kilometraje = (notificacion.getKilometrajeServicio() != null)
                ? notificacion.getKilometrajeServicio()
                : "N/A";

        return whatsAppService.enviarRecordatorioServicio(
                telefono,
                cliente.getNombre(),
                placa,
                kilometraje);
    }

    /**
     * Devuelve la lista completa de clientes que aún no han respondido la solicitud
     * de consentimiento para recibir notificaciones.
     *
     * @return lista de mapas con los campos {@code id}, {@code nombre}, {@code cedula},
     *         {@code telefono} y {@code fechaCreacion}
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerClientesPendientesConsentimiento() {
        return clienteRepository.findClientesPendientesDeConsentimiento().stream()
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getCliente_id());
                    map.put("nombre", c.getNombre());
                    map.put("cedula", c.getCedula_ruc());
                    map.put("telefono", c.getTelefonos() != null && !c.getTelefonos().isEmpty()
                            ? c.getTelefonos().get(0).getTelefono()
                            : "Sin teléfono");
                    map.put("fechaCreacion", c.getFecha_registro() != null
                            ? c.getFecha_registro().toString()
                            : null);
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * Versión paginada de {@link #obtenerClientesPendientesConsentimiento()}.
     * Acepta un término de búsqueda opcional para filtrar por nombre o cédula.
     *
     * @param pageable  configuración de paginación y orden
     * @param busqueda  término de búsqueda (puede ser nulo o vacío)
     * @return página de mapas con los datos del cliente
     */
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> obtenerClientesPendientesPaginado(Pageable pageable, String busqueda) {
        Page<Clientes> page;
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            page = clienteRepository.buscarPendientesPaginado(busqueda.trim(), pageable);
        } else {
            page = clienteRepository.findClientesPendientesDeConsentimientoPaginado(pageable);
        }
        return page.map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getCliente_id());
                    map.put("nombre", c.getNombre());
                    map.put("cedula", c.getCedula_ruc());
                    map.put("telefono", c.getTelefonos() != null && !c.getTelefonos().isEmpty()
                            ? c.getTelefonos().get(0).getTelefono()
                            : "Sin teléfono");
                    map.put("fechaCreacion", c.getFecha_registro() != null
                            ? c.getFecha_registro().toString()
                            : null);
                    return map;
                });
    }

    /**
     * Devuelve el conteo total de clientes pendientes de consentimiento.
     *
     * @return número de clientes sin respuesta de consentimiento
     */
    @Transactional(readOnly = true)
    public long contarClientesPendientesConsentimiento() {
        return clienteRepository.countClientesPendientesDeConsentimiento();
    }

    /**
     * Envía la solicitud de consentimiento por WhatsApp únicamente a los clientes
     * cuyos IDs fueron seleccionados manualmente en el frontend.
     *
     * @param idsSeleccionados lista de IDs de clientes a los que se enviará el mensaje
     * @return mapa con los contadores {@code totalProcesados}, {@code enviados},
     *         {@code fallidos} y {@code sinTelefono}
     */
    @Transactional
    public Map<String, Object> enviarConsentimientoMasivo(List<Integer> idsSeleccionados) {
        log.info("Iniciando envío masivo para {} clientes seleccionados...", idsSeleccionados.size());

        List<Clientes> clientesATrabajar = clienteRepository.findAllById(idsSeleccionados);

        int enviados = 0, fallidos = 0, sinTelefono = 0;
        Map<String, Object> resumen = new HashMap<>();

        for (Clientes cliente : clientesATrabajar) {
            if (cliente.getTelefonos() == null || cliente.getTelefonos().isEmpty()) {
                sinTelefono++;
                continue;
            }

            String telefono = cliente.getTelefonos().get(0).getTelefono();
            ResultadoEnvioMensaje resultado = whatsAppService.enviarSolicitudConsentimiento(telefono, cliente.getNombre());

            if (resultado == ResultadoEnvioMensaje.ENVIADO) {
                enviados++;
                guardarNotificacion(cliente, "Solicitud de consentimiento enviada", "enviado", resultado.name());
            } else {
                fallidos++;
                guardarNotificacion(cliente, "Fallo al enviar solicitud", "fallido", resultado.name());
            }
        }

        resumen.put("totalProcesados", clientesATrabajar.size());
        resumen.put("enviados", enviados);
        resumen.put("fallidos", fallidos);
        resumen.put("sinTelefono", sinTelefono);

        return resumen;
    }

    /**
     * Persiste una notificación de tipo {@code CONSENTIMIENTO} en el historial.
     */
    private void guardarNotificacion(Clientes cliente, String mensaje, String estado, String resultadoEnvio) {
        Notificaciones n = new Notificaciones();
        n.setClientes(cliente);
        n.setCanal("whatsapp");
        n.setTipoNotificacion("CONSENTIMIENTO");
        n.setMensaje_enviado(mensaje);
        n.setEstado(estado);
        n.setResultadoEnvio(resultadoEnvio);
        n.setFecha_envio(new Timestamp(System.currentTimeMillis()));
        n.setIntentosEnvio(1);
        notificacionesRepository.save(n);
    }

    /**
     * Devuelve la lista de clientes elegibles para una promoción del producto indicado,
     * enriquecida con el estado de contacto reciente para mostrar advertencias en el frontend.
     *
     * <p>Un cliente se considera "contactado recientemente" si recibió una promoción
     * enviada exitosamente en los últimos 30 días.
     *
     * @param productoId ID del producto sobre el que se quiere lanzar la promoción
     * @return lista de {@link ClientePreviewDTO} con datos del cliente y advertencias
     */
    @Transactional(readOnly = true)
    public List<ClientePreviewDTO> obtenerPreviewClientes(Integer productoId) {
        List<Detalle_Servicio> detalles = detalleServicioRepository.findByProductoId(productoId);

        Set<Integer> vistos = new HashSet<>();
        List<Clientes> clientesUnicos = new ArrayList<>();
        for (Detalle_Servicio detalle : detalles) {
            if (detalle.getServicio() == null)
                continue;
            Clientes c = detalle.getServicio().getClientes();
            if (c == null)
                continue;
            if (vistos.add(c.getCliente_id()))
                clientesUnicos.add(c);
        }

        List<Integer> ids = clientesUnicos.stream()
                .map(Clientes::getCliente_id).collect(Collectors.toList());

        Map<Integer, Timestamp> ultimaPromocion = new HashMap<>();
        if (!ids.isEmpty()) {
            notificacionesRepository.findUltimaPromocionEnviadaPorClientes(ids)
                    .forEach(row -> ultimaPromocion.put((Integer) row[0], (Timestamp) row[1]));
        }

        Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());

        return clientesUnicos.stream().map(c -> {
            String telefono = (c.getTelefonos() != null && !c.getTelefonos().isEmpty())
                    ? c.getTelefonos().get(0).getTelefono()
                    : null;

            Timestamp ultima = ultimaPromocion.get(c.getCliente_id());
            Integer dias = null;
            boolean reciente = false;
            if (ultima != null) {
                dias = (int) ChronoUnit.DAYS.between(ultima.toLocalDateTime(), ahora.toLocalDateTime());
                reciente = dias <= 30;
            }

            return new ClientePreviewDTO(
                    c.getCliente_id(),
                    c.getNombre(),
                    telefono,
                    Boolean.TRUE.equals(c.getRecibeNotificaciones()),
                    telefono != null,
                    reciente,
                    dias);
        }).collect(Collectors.toList());
    }

    /**
     * Envía una promoción por WhatsApp a los clientes que han usado el producto indicado.
     * Si el DTO incluye {@code clientesSeleccionados}, el envío se limita a esos IDs.
     *
     * <p>Los clientes sin consentimiento o sin teléfono no reciben el mensaje pero
     * quedan registrados en el historial con el estado correspondiente.
     *
     * @param dto datos de la promoción: producto, texto, precio oferta y rango de fechas
     * @return mapa con los contadores {@code producto}, {@code clientesEncontrados},
     *         {@code enviados}, {@code fallidos}, {@code sinTelefono} y {@code sinConsentimiento}
     */
    @Transactional
    public Map<String, Object> enviarPromocion(PromocionesRequestDTO dto) {
        Optional<Productos> productoOpt = productosRepository.findById(dto.getProductoId());

        Map<String, Object> resumen = new HashMap<>();

        if (productoOpt.isEmpty()) {
            log.warn("Producto con ID {} no encontrado", dto.getProductoId());
            resumen.put("clientesEncontrados", 0);
            resumen.put("enviados", 0);
            resumen.put("fallidos", 0);
            resumen.put("sinTelefono", 0);
            resumen.put("sinConsentimiento", 0);
            return resumen;
        }

        Productos producto = productoOpt.get();
        log.info("Enviando promoción para producto: {}", producto.getNombre());

        List<Detalle_Servicio> detalles = detalleServicioRepository.findByProductoId(dto.getProductoId());
        log.info("Detalles de servicio encontrados: {}", detalles.size());

        Set<Integer> clientesVistos = new HashSet<>();
        List<Clientes> clientesUnicos = new ArrayList<>();

        for (Detalle_Servicio detalle : detalles) {
            if (detalle.getServicio() == null)
                continue;
            Clientes cliente = detalle.getServicio().getClientes();
            if (cliente == null)
                continue;
            if (clientesVistos.add(cliente.getCliente_id())) {
                clientesUnicos.add(cliente);
            }
        }

        List<Integer> seleccionados = dto.getClientesSeleccionados();
        if (seleccionados != null && !seleccionados.isEmpty()) {
            Set<Integer> idsSeleccionados = new HashSet<>(seleccionados);
            clientesUnicos = clientesUnicos.stream()
                    .filter(c -> idsSeleccionados.contains(c.getCliente_id()))
                    .collect(Collectors.toList());
            log.info("Clientes filtrados por selección manual: {}", clientesUnicos.size());
        }

        log.info("Clientes únicos encontrados: {}", clientesUnicos.size());

        int enviados = 0, fallidos = 0, sinTelefono = 0, sinConsentimiento = 0;

        String descripcionPromo = dto.getPromocion() + " | Precio: " + dto.getPrecioOferta()
                + " | Vigencia: " + dto.getRangoFechas();

        for (Clientes cliente : clientesUnicos) {
            if (!Boolean.TRUE.equals(cliente.getRecibeNotificaciones())) {
                sinConsentimiento++;
                guardarNotificacionPromocion(cliente, descripcionPromo,
                        "sin_consentimiento", "SIN_CONSENTIMIENTO");
                continue;
            }

            if (cliente.getTelefonos() == null || cliente.getTelefonos().isEmpty()) {
                sinTelefono++;
                guardarNotificacionPromocion(cliente, descripcionPromo,
                        "fallido", "SIN_TELEFONO");
                continue;
            }

            String telefono = cliente.getTelefonos().get(0).getTelefono();

            ResultadoEnvioMensaje resultado = whatsAppService.enviarMensajePromo(
                    telefono, cliente.getNombre(), dto.getPromocion(), dto.getPrecioOferta(), dto.getRangoFechas());

            if (resultado == ResultadoEnvioMensaje.ENVIADO) {
                enviados++;
                guardarNotificacionPromocion(cliente, descripcionPromo, "enviado", resultado.name());
            } else {
                fallidos++;
                guardarNotificacionPromocion(cliente, descripcionPromo, "fallido", resultado.name());
            }
        }

        resumen.put("producto", producto.getNombre());
        resumen.put("clientesEncontrados", clientesUnicos.size());
        resumen.put("enviados", enviados);
        resumen.put("fallidos", fallidos);
        resumen.put("sinTelefono", sinTelefono);
        resumen.put("sinConsentimiento", sinConsentimiento);
        return resumen;
    }

    /**
     * Persiste una notificación de tipo {@code PROMOCION} en el historial.
     */
    private void guardarNotificacionPromocion(Clientes cliente,
            String mensajePromo, String estado,
            String resultadoEnvio) {
        Notificaciones n = new Notificaciones();
        n.setClientes(cliente);
        n.setCanal("whatsapp");
        n.setTipoNotificacion("PROMOCION");
        n.setMensaje_enviado(mensajePromo);
        n.setEstado(estado);
        n.setResultadoEnvio(resultadoEnvio);
        n.setFecha_envio(Timestamp.valueOf(LocalDateTime.now()));
        n.setIntentosEnvio(1);
        notificacionesRepository.save(n);
    }

    /**
     * Devuelve todas las notificaciones de tipo {@code PROMOCION} ordenadas.
     *
     * @return lista completa de promociones registradas
     */
    @Transactional(readOnly = true)
    public List<Notificaciones> obtenerPromocionesEnviadas() {
        return notificacionesRepository.findByTipoNotificacionOrdenado("PROMOCION");
    }

    /**
     * Versión paginada de {@link #obtenerPromocionesEnviadas()}.
     *
     * @param pageable configuración de paginación y orden
     * @return página de notificaciones de tipo {@code PROMOCION}
     */
    @Transactional(readOnly = true)
    public Page<Notificaciones> obtenerPromocionesEnviadasPaginado(Pageable pageable) {
        return notificacionesRepository.findByTipoNotificacionPaginado("PROMOCION", pageable);
    }

    /**
     * Devuelve las promociones paginadas filtradas por rango de fechas de envío.
     *
     * @param pageable configuración de paginación
     * @param desde    inicio del rango (inclusive)
     * @param hasta    fin del rango (inclusive)
     * @return página de notificaciones de tipo {@code PROMOCION} en el rango
     */
    @Transactional(readOnly = true)
    public Page<Notificaciones> obtenerPromocionesPorFechaPaginado(Pageable pageable,
            java.time.LocalDateTime desde, java.time.LocalDateTime hasta) {
        return notificacionesRepository.findByTipoNotificacionYFechaPaginado("PROMOCION", desde, hasta, pageable);
    }

    /**
     * Devuelve todos los recordatorios de servicio ordenados.
     *
     * @return lista completa de recordatorios registrados
     */
    @Transactional(readOnly = true)
    public List<Notificaciones> obtenerRecordatorios() {
        return notificacionesRepository.findRecordatoriosOrdenados();
    }

    /**
     * Versión paginada de {@link #obtenerRecordatorios()}.
     *
     * @param pageable configuración de paginación y orden
     * @return página de recordatorios
     */
    @Transactional(readOnly = true)
    public Page<Notificaciones> obtenerRecordatoriosPaginado(Pageable pageable) {
        return notificacionesRepository.findRecordatoriosPaginados(pageable);
    }

    /**
     * Devuelve los recordatorios paginados filtrados por rango de fechas programadas.
     *
     * @param pageable configuración de paginación
     * @param desde    inicio del rango (inclusive)
     * @param hasta    fin del rango (inclusive)
     * @return página de recordatorios en el rango
     */
    @Transactional(readOnly = true)
    public Page<Notificaciones> obtenerRecordatoriosPorFechaPaginado(Pageable pageable,
            java.time.LocalDateTime desde, java.time.LocalDateTime hasta) {
        return notificacionesRepository.findRecordatoriosPaginadosPorFecha(desde, hasta, pageable);
    }

    /**
     * Devuelve la lista de productos disponibles para seleccionar en el formulario de promociones.
     *
     * @return productos ordenados alfabéticamente por nombre
     */
    @Transactional(readOnly = true)
    public List<Productos> obtenerProductosDisponibles() {
        return productosRepository.findAll().stream()
                .filter(p -> p.getNombre() != null && !p.getNombre().trim().isEmpty())
                .sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()))
                .collect(Collectors.toList());
    }

    /**
     * Actualiza la fecha programada de un recordatorio pendiente.
     * Solo se permite modificar recordatorios en estado {@code "pendiente"}.
     *
     * @param id         ID del recordatorio a modificar
     * @param nuevaFecha nueva fecha de envío programado
     * @throws RuntimeException si el recordatorio no existe o no está en estado pendiente
     */
    @Transactional
    public void actualizarFechaProgramada(Integer id, java.time.LocalDate nuevaFecha) {
        Notificaciones notificacion = notificacionesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recordatorio no encontrado"));

        if (!"pendiente".equals(notificacion.getEstado())) {
            throw new RuntimeException("Solo se puede modificar la fecha de recordatorios pendientes");
        }

        notificacion.setFecha_programada(Timestamp.valueOf(nuevaFecha.atTime(12, 0)));
        notificacionesRepository.save(notificacion);
        log.info("Fecha de recordatorio {} actualizada a {}", id, nuevaFecha);
    }
}
