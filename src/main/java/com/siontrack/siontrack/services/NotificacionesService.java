package com.siontrack.siontrack.services;

import com.siontrack.siontrack.DTO.Request.PromocionesRequestDTO;
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

    @Scheduled(cron = "0 0 23 * * *")
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
                log.info("✅ Recordatorio enviado: {}", notificacion.getNombreServicio());
            } else {
                log.warn("❌ Falló definitivamente: {}", notificacion.getNombreServicio());
                fallidos++;
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

        public Map<String, Object> enviarPromocion(PromocionesRequestDTO dto) {
        log.info("📢 Enviando promoción para marca: {}", dto.getMarcaVehiculo());

        // Buscar vehículos por marca
        List<Vehiculos> vehiculos = vehiculosRepository.findByMarcaIgnoreCase(dto.getMarcaVehiculo());

        log.info("🚗 Vehículos encontrados: {}", vehiculos.size());

        int enviados = 0;
        int fallidos = 0;
        int sinTelefono = 0;
        int sinConsentimiento = 0;

        for (Vehiculos vehiculo : vehiculos) {
            Clientes cliente = vehiculo.getClientes();

            if (cliente == null) {
                log.warn("⚠️ Vehículo {} sin cliente asociado", vehiculo.getPlaca());
                continue;
            }

            // Verificar consentimiento
            if (!Boolean.TRUE.equals(cliente.getRecibeNotificaciones())) {
                log.debug("Cliente {} no acepta notificaciones", cliente.getNombre());
                sinConsentimiento++;
                continue;
            }

            // Obtener teléfono
            if (cliente.getTelefonos() == null || cliente.getTelefonos().isEmpty()) {
                log.warn("⚠️ Cliente {} sin teléfono", cliente.getNombre());
                sinTelefono++;
                continue;
            }

            String telefono = cliente.getTelefonos().get(0).getTelefono();

            // Enviar promoción
            ResultadoEnvioMensaje resultado = whatsAppService.enviarMensajePromo(
                telefono,
                cliente.getNombre(),
                dto.getMarcaVehiculo(),
                dto.getPromocion(),
                dto.getPrecioOferta(),
                dto.getRangoFechas()
            );

            if (resultado == ResultadoEnvioMensaje.ENVIADO) {
                enviados++;
                log.info("✅ Promoción enviada a: {}", cliente.getNombre());
            } else {
                fallidos++;
                log.warn("❌ Falló envío a: {} - {}", cliente.getNombre(), resultado);
            }
        }

        log.info("📊 Resumen: Enviados={}, Fallidos={}, SinTelefono={}, SinConsentimiento={}",
                enviados, fallidos, sinTelefono, sinConsentimiento);

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("marca", dto.getMarcaVehiculo());
        resumen.put("vehiculosEncontrados", vehiculos.size());
        resumen.put("enviados", enviados);
        resumen.put("fallidos", fallidos);
        resumen.put("sinTelefono", sinTelefono);
        resumen.put("sinConsentimiento", sinConsentimiento);

        return resumen;
    }

}