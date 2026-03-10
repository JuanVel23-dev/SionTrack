package com.siontrack.siontrack.services;

import com.siontrack.siontrack.DTO.Request.PromocionesRequestDTO;
import com.siontrack.siontrack.DTO.Response.PromocionEnviadaDTO;
import com.siontrack.siontrack.DTO.Response.RecordatorioDTO;
import com.siontrack.siontrack.models.Clientes;
import com.siontrack.siontrack.models.Notificaciones;
import com.siontrack.siontrack.models.Vehiculos;
import com.siontrack.siontrack.models.enums.ResultadoEnvioMensaje;
import com.siontrack.siontrack.repository.NotificacionesRepository;
import com.siontrack.siontrack.repository.VehiculosRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificacionesService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesService.class);

    private final NotificacionesRepository notificacionesRepository;
    private final WhatsAppService whatsAppService;
    private final VehiculosRepository vehiculosRepository;

    public NotificacionesService(NotificacionesRepository notificacionesRepository,
                                         WhatsAppService whatsAppService,
                                         VehiculosRepository vehiculosRepository) {
        this.notificacionesRepository = notificacionesRepository;
        this.whatsAppService = whatsAppService;
        this.vehiculosRepository = vehiculosRepository;
    }

    // =============================================
    // RECORDATORIOS PROGRAMADOS (sin cambios)
    // =============================================

    @Scheduled(cron = "0 19 22 * * *")
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
        log.info("📢 Enviando promoción para marca: {}", dto.getMarcaVehiculo());

        List<Vehiculos> vehiculos = vehiculosRepository.findByMarcaIgnoreCase(dto.getMarcaVehiculo());

        log.info("🚗 Vehículos encontrados: {}", vehiculos.size());

        int enviados = 0, fallidos = 0, sinTelefono = 0, sinConsentimiento = 0;

        String descripcionPromo = dto.getPromocion() + " | Precio: " + dto.getPrecioOferta()
                + " | Vigencia: " + dto.getRangoFechas();

        for (Vehiculos vehiculo : vehiculos) {
            Clientes cliente = vehiculo.getClientes();

            if (cliente == null) {
                log.warn("⚠️ Vehículo {} sin cliente asociado", vehiculo.getPlaca());
                continue;
            }

            if (!Boolean.TRUE.equals(cliente.getRecibeNotificaciones())) {
                sinConsentimiento++;
                guardarNotificacionPromocion(cliente, vehiculo, descripcionPromo,
                        "sin_consentimiento", "SIN_CONSENTIMIENTO");
                continue;
            }

            if (cliente.getTelefonos() == null || cliente.getTelefonos().isEmpty()) {
                sinTelefono++;
                guardarNotificacionPromocion(cliente, vehiculo, descripcionPromo,
                        "fallido", "SIN_TELEFONO");
                continue;
            }

            String telefono = cliente.getTelefonos().get(0).getTelefono();

            ResultadoEnvioMensaje resultado = whatsAppService.enviarMensajePromo(
                telefono, cliente.getNombre(), dto.getMarcaVehiculo(),
                dto.getPromocion(), dto.getPrecioOferta(), dto.getRangoFechas()
            );

            if (resultado == ResultadoEnvioMensaje.ENVIADO) {
                enviados++;
                guardarNotificacionPromocion(cliente, vehiculo, descripcionPromo,
                        "enviado", resultado.name());
            } else {
                fallidos++;
                guardarNotificacionPromocion(cliente, vehiculo, descripcionPromo,
                        "fallido", resultado.name());
            }
        }

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("marca", dto.getMarcaVehiculo());
        resumen.put("vehiculosEncontrados", vehiculos.size());
        resumen.put("enviados", enviados);
        resumen.put("fallidos", fallidos);
        resumen.put("sinTelefono", sinTelefono);
        resumen.put("sinConsentimiento", sinConsentimiento);
        return resumen;
    }

    private void guardarNotificacionPromocion(Clientes cliente, Vehiculos vehiculo,
                                               String mensajePromo, String estado,
                                               String resultadoEnvio) {
        Notificaciones n = new Notificaciones();
        n.setClientes(cliente);
        n.setVehiculo(vehiculo);
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
    // =============================================

    @Transactional(readOnly = true)
    public List<PromocionEnviadaDTO> obtenerPromocionesEnviadas() {
        return notificacionesRepository.findByTipoNotificacionOrdenado("PROMOCION")
                .stream().map(this::mapearAPromocionDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecordatorioDTO> obtenerRecordatorios() {
        return notificacionesRepository.findRecordatoriosOrdenados()
                .stream().map(this::mapearARecordatorioDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> obtenerMarcasVehiculos() {
        return vehiculosRepository.findAll().stream()
                .map(Vehiculos::getMarca)
                .filter(m -> m != null && !m.trim().isEmpty())
                .map(String::trim).distinct().sorted()
                .collect(Collectors.toList());
    }

    private PromocionEnviadaDTO mapearAPromocionDTO(Notificaciones n) {
        PromocionEnviadaDTO dto = new PromocionEnviadaDTO();
        dto.setNotificacionId(n.getNotificacion_id());
        dto.setMensajeEnviado(n.getMensaje_enviado());
        dto.setEstado(n.getEstado());
        dto.setResultadoEnvio(n.getResultadoEnvio());
        dto.setFechaEnvio(n.getCreado_en());
        if (n.getClientes() != null) {
            dto.setClienteNombre(n.getClientes().getNombre());
            if (n.getClientes().getTelefonos() != null && !n.getClientes().getTelefonos().isEmpty())
                dto.setTelefono(n.getClientes().getTelefonos().get(0).getTelefono());
        }
        if (n.getVehiculo() != null) {
            String info = (n.getVehiculo().getMarca() != null ? n.getVehiculo().getMarca() : "")
                    + (n.getVehiculo().getModelo() != null ? " " + n.getVehiculo().getModelo() : "");
            dto.setVehiculoInfo(info.trim());
            dto.setPlaca(n.getVehiculo().getPlaca());
        }
        return dto;
    }

    private RecordatorioDTO mapearARecordatorioDTO(Notificaciones n) {
        RecordatorioDTO dto = new RecordatorioDTO();
        dto.setNotificacionId(n.getNotificacion_id());
        dto.setNombreServicio(n.getNombreServicio());
        dto.setKilometrajeServicio(n.getKilometrajeServicio());
        dto.setEstado(n.getEstado());
        dto.setResultadoEnvio(n.getResultadoEnvio());
        dto.setFechaProgramada(n.getFecha_programada());
        dto.setFechaEnvio(n.getFecha_envio());
        dto.setCreadoEn(n.getCreado_en());
        if (n.getClientes() != null) {
            dto.setClienteNombre(n.getClientes().getNombre());
            if (n.getClientes().getTelefonos() != null && !n.getClientes().getTelefonos().isEmpty())
                dto.setTelefono(n.getClientes().getTelefonos().get(0).getTelefono());
        }
        if (n.getVehiculo() != null) {
            String info = (n.getVehiculo().getMarca() != null ? n.getVehiculo().getMarca() : "")
                    + (n.getVehiculo().getModelo() != null ? " " + n.getVehiculo().getModelo() : "");
            dto.setVehiculoInfo(info.trim());
            dto.setPlaca(n.getVehiculo().getPlaca());
        }
        return dto;
    }
}