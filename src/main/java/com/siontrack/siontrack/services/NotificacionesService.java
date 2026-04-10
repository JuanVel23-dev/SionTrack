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

    // =============================================
    // RECORDATORIOS PROGRAMADOS (sin cambios)
    // =============================================

    @Scheduled(cron = "0 0 12 * * WED")
    @Transactional
    public void enviarNotificacionesProgramadas() {
        log.info("🔔 Enviando recordatorios programados...");

        Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());
        List<Notificaciones> pendientes = notificacionesRepository.findNotificacionesPendientes(ahora);

        log.info("📋 Pendientes: {}", pendientes.size());

        int enviados = 0, fallidos = 0;

        for (Notificaciones notificacion : pendientes) {
            ResultadoEnvioMensaje resultado = enviarNotificacion(notificacion);

            notificacion.setFecha_envio(ahora);
            notificacion.setResultadoEnvio(resultado.name());

            if (resultado == ResultadoEnvioMensaje.ENVIADO) {
                notificacion.setEstado("enviado");
                enviados++;
                log.info("✅ Recordatorio enviado: {}", notificacion.getNombreServicio());
            } else {
                notificacion.setEstado("fallido");
                fallidos++;
                log.warn("❌ Falló definitivamente: {}", notificacion.getNombreServicio());
            }

            notificacionesRepository.save(notificacion);
        }

        log.info("✅ Completado - Enviados: {}, Fallidos: {}", enviados, fallidos);
    }

    private ResultadoEnvioMensaje enviarNotificacion(Notificaciones notificacion) {
        var cliente = notificacion.getClientes();

        if (cliente.getTelefonos() == null || cliente.getTelefonos().isEmpty()) {
            log.warn("⚠️ Cliente {} sin teléfono", cliente.getNombre());
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

    // 1. Método para alimentar la lista del Frontend
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

    @Transactional(readOnly = true)
    public long contarClientesPendientesConsentimiento() {
        return clienteRepository.countClientesPendientesDeConsentimiento();
    }

    // 2. Método actualizado para recibir únicamente los IDs seleccionados
    @Transactional
    public Map<String, Object> enviarConsentimientoMasivo(List<Integer> idsSeleccionados) {
        log.info("📢 Iniciando envío masivo para {} clientes seleccionados...", idsSeleccionados.size());

        // Busca solo los clientes que el usuario marcó en el HTML
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

    // 3. Método auxiliar para guardar en el historial
    private void guardarNotificacion(Clientes cliente, String mensaje, String estado, String resultadoEnvio) {
        Notificaciones n = new Notificaciones();
        n.setClientes(cliente);
        n.setCanal("whatsapp");
        n.setTipoNotificacion("CONSENTIMIENTO"); // Para distinguirlo de PROMOCION y RECORDATORIO
        n.setMensaje_enviado(mensaje);
        n.setEstado(estado);
        n.setResultadoEnvio(resultadoEnvio);
        n.setFecha_envio(new Timestamp(System.currentTimeMillis()));
        n.setIntentosEnvio(1);
        notificacionesRepository.save(n);
    }
    // =============================================
    // PROMOCIONES
    // =============================================

    /**
     * Retorna la lista de clientes elegibles para una promoción de un producto
     * dado,
     * incluyendo el estado de contacto reciente para mostrar advertencias en el
     * frontend.
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

        // Obtiene la fecha de la última promoción enviada exitosamente por cliente
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

    @Transactional
    public Map<String, Object> enviarPromocion(PromocionesRequestDTO dto) {
        Optional<Productos> productoOpt = productosRepository.findById(dto.getProductoId());

        Map<String, Object> resumen = new HashMap<>();

        if (productoOpt.isEmpty()) {
            log.warn("⚠️ Producto con ID {} no encontrado", dto.getProductoId());
            resumen.put("clientesEncontrados", 0);
            resumen.put("enviados", 0);
            resumen.put("fallidos", 0);
            resumen.put("sinTelefono", 0);
            resumen.put("sinConsentimiento", 0);
            return resumen;
        }

        Productos producto = productoOpt.get();
        log.info("📢 Enviando promoción para producto: {}", producto.getNombre());

        List<Detalle_Servicio> detalles = detalleServicioRepository.findByProductoId(dto.getProductoId());
        log.info("🔍 Detalles de servicio encontrados: {}", detalles.size());

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

        // Filtra por selección manual si el usuario eligió clientes específicos
        List<Integer> seleccionados = dto.getClientesSeleccionados();
        if (seleccionados != null && !seleccionados.isEmpty()) {
            Set<Integer> idsSeleccionados = new HashSet<>(seleccionados);
            clientesUnicos = clientesUnicos.stream()
                    .filter(c -> idsSeleccionados.contains(c.getCliente_id()))
                    .collect(Collectors.toList());
            log.info("🎯 Clientes filtrados por selección manual: {}", clientesUnicos.size());
        }

        log.info("👥 Clientes únicos encontrados: {}", clientesUnicos.size());

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

    // =============================================
    // CONSULTAS PARA LAS VISTAS
    // Retorna directamente List<Notificaciones>
    // sin DTOs intermedios
    // =============================================

    @Transactional(readOnly = true)
    public List<Notificaciones> obtenerPromocionesEnviadas() {
        return notificacionesRepository.findByTipoNotificacionOrdenado("PROMOCION");
    }

    @Transactional(readOnly = true)
    public Page<Notificaciones> obtenerPromocionesEnviadasPaginado(Pageable pageable) {
        return notificacionesRepository.findByTipoNotificacionPaginado("PROMOCION", pageable);
    }

    @Transactional(readOnly = true)
    public Page<Notificaciones> obtenerPromocionesPorFechaPaginado(Pageable pageable,
            java.time.LocalDateTime desde, java.time.LocalDateTime hasta) {
        return notificacionesRepository.findByTipoNotificacionYFechaPaginado("PROMOCION", desde, hasta, pageable);
    }

    @Transactional(readOnly = true)
    public List<Notificaciones> obtenerRecordatorios() {
        return notificacionesRepository.findRecordatoriosOrdenados();
    }

    @Transactional(readOnly = true)
    public Page<Notificaciones> obtenerRecordatoriosPaginado(Pageable pageable) {
        return notificacionesRepository.findRecordatoriosPaginados(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Notificaciones> obtenerRecordatoriosPorFechaPaginado(Pageable pageable,
            java.time.LocalDateTime desde, java.time.LocalDateTime hasta) {
        return notificacionesRepository.findRecordatoriosPaginadosPorFecha(desde, hasta, pageable);
    }

    @Transactional(readOnly = true)
    public List<Productos> obtenerProductosDisponibles() {
        return productosRepository.findAll().stream()
                .filter(p -> p.getNombre() != null && !p.getNombre().trim().isEmpty())
                .sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()))
                .collect(Collectors.toList());
    }

    // Actualiza únicamente la fecha de envío de un recordatorio pendiente
    @Transactional
    public void actualizarFechaProgramada(Integer id, java.time.LocalDate nuevaFecha) {
        Notificaciones notificacion = notificacionesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recordatorio no encontrado"));

        if (!"pendiente".equals(notificacion.getEstado())) {
            throw new RuntimeException("Solo se puede modificar la fecha de recordatorios pendientes");
        }

        notificacion.setFecha_programada(Timestamp.valueOf(nuevaFecha.atTime(12, 0)));
        notificacionesRepository.save(notificacion);
        log.info("📅 Fecha de recordatorio {} actualizada a {}", id, nuevaFecha);
    }
}