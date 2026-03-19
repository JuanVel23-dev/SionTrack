package com.siontrack.siontrack.services;

import com.siontrack.siontrack.DTO.Request.PromocionesRequestDTO;
import com.siontrack.siontrack.models.Clientes;
import com.siontrack.siontrack.models.Detalle_Servicio;
import com.siontrack.siontrack.models.Notificaciones;
import com.siontrack.siontrack.models.Productos;
import com.siontrack.siontrack.models.Vehiculos;
import com.siontrack.siontrack.models.enums.ResultadoEnvioMensaje;
import com.siontrack.siontrack.repository.DetalleServicioRepository;
import com.siontrack.siontrack.repository.NotificacionesRepository;
import com.siontrack.siontrack.repository.ProductosRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
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

    public NotificacionesService(NotificacionesRepository notificacionesRepository,
                                         WhatsAppService whatsAppService,
                                         ProductosRepository productosRepository,
                                         DetalleServicioRepository detalleServicioRepository) {
        this.notificacionesRepository = notificacionesRepository;
        this.whatsAppService = whatsAppService;
        this.productosRepository = productosRepository;
        this.detalleServicioRepository = detalleServicioRepository;
    }

    // =============================================
    // RECORDATORIOS PROGRAMADOS (sin cambios)
    // =============================================

    @Scheduled(cron = "0 20 20 * * *")
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
                kilometraje
        );
    }

    // =============================================
    // PROMOCIONES
    // =============================================

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
            if (detalle.getServicio() == null) continue;
            Clientes cliente = detalle.getServicio().getClientes();
            if (cliente == null) continue;
            if (clientesVistos.add(cliente.getCliente_id())) {
                clientesUnicos.add(cliente);
            }
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
                telefono, cliente.getNombre(), producto.getNombre(),
                dto.getPromocion(), dto.getPrecioOferta(), dto.getRangoFechas()
            );

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
    public List<Notificaciones> obtenerRecordatorios() {
        return notificacionesRepository.findRecordatoriosOrdenados();
    }

    @Transactional(readOnly = true)
    public List<Productos> obtenerProductosDisponibles() {
        return productosRepository.findAll().stream()
                .filter(p -> p.getNombre() != null && !p.getNombre().trim().isEmpty())
                .sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()))
                .collect(Collectors.toList());
    }
}