package com.siontrack.siontrack.services;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.siontrack.siontrack.models.Notificaciones;
import com.siontrack.siontrack.models.Servicios;
import com.siontrack.siontrack.repository.NotificacionesRepository;

import jakarta.transaction.Transactional;

@Service
public class RecordatorioService {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioService.class);

    private static final int meses_proximo_servicio = 8;
    private static final int mes_primer_recordatorio = 1;

    private final NotificacionesRepository notificacionesRepository;

    public RecordatorioService(NotificacionesRepository notificacionesRepository) {
        this.notificacionesRepository = notificacionesRepository;
    }

    @Transactional
    public void procesarServicioParaRecordatorios(Servicios servicio) {
        log.info("📋 Procesando servicio {} para recordatorios", servicio.getServicio_id());

        String kilometraje = servicio.getKilometraje_servicio();
        if (kilometraje == null || kilometraje.trim().isEmpty()) {
            log.info("Sin kilometraje, no se crean recordatorios");
            return;
        }

        if (notificacionesRepository.existsByServicio(servicio)) {
            log.info("Ya existen recordatorios para este servicio");
            return;
        }

        Integer clienteId = servicio.getClientes().getCliente_id();
        Integer vehiculoId = servicio.getVehiculos() != null ? servicio.getVehiculos().getVehiculo_id() : null;

        // Verificar si ya existen recordatorios pendientes para este cliente+vehículo
        List<Notificaciones> pendientesExistentes = notificacionesRepository
                .findRecordatoriosPendientesByClienteVehiculo(clienteId, vehiculoId);

        if (!pendientesExistentes.isEmpty()) {
            // Eliminar los recordatorios desactualizados y recrearlos con el nuevo servicio
            log.info("🔄 Se encontraron {} recordatorio(s) pendiente(s) para cliente {} / vehículo {}. Reemplazando...",
                    pendientesExistentes.size(), clienteId, vehiculoId);
            notificacionesRepository.deleteAll(pendientesExistentes);
        }

        crearNotificaciones(servicio);
    }

    private void crearNotificaciones(Servicios servicio) {
        LocalDate fechaServicio = servicio.getFecha_servicio();
        LocalDate fechaProximoServicio = fechaServicio.plusMonths(meses_proximo_servicio);
        LocalDate fechaPrimerRecordatorio = fechaProximoServicio.minusMonths(mes_primer_recordatorio);
        LocalDate fechaSegundoRecordatorio = fechaProximoServicio;

        log.info("📅 Servicio: {} | Próximo: {} | Recordatorios: {} y {}",
                fechaServicio, fechaProximoServicio, fechaPrimerRecordatorio, fechaSegundoRecordatorio);

        Integer clienteId = servicio.getClientes().getCliente_id();
        Integer vehiculoId = servicio.getVehiculos() != null ? servicio.getVehiculos().getVehiculo_id() : null;
        Timestamp fechaProximoServicioTs = Timestamp.valueOf(fechaProximoServicio.atStartOfDay());

        // Verificar si ya existe un recordatorio para este cliente+vehículo con la misma fecha objetivo
        if (notificacionesRepository.existsRecordatorioDuplicado(clienteId, vehiculoId, fechaProximoServicioTs)) {
            log.info("Ya existen recordatorios para cliente {} y vehículo {} con fecha objetivo {}",
                    clienteId, vehiculoId, fechaProximoServicio);
            return;
        }

        // Primer recordatorio: 1 mes antes
        if (fechaPrimerRecordatorio.isAfter(LocalDate.now()) || fechaPrimerRecordatorio.equals(LocalDate.now())) {
            crearNotificacion(servicio, fechaPrimerRecordatorio, fechaProximoServicio, 1);
        }

        // Segundo recordatorio: el día del próximo servicio
        if (fechaSegundoRecordatorio.isAfter(LocalDate.now())) {
            crearNotificacion(servicio, fechaSegundoRecordatorio, fechaProximoServicio, 2);
        }
    }

    private void crearNotificacion(Servicios servicio, LocalDate fechaRecordatorio,
                                    LocalDate fechaProximoServicio, int numeroRecordatorio) {
        Notificaciones notificacion = new Notificaciones();
        notificacion.setClientes(servicio.getClientes());
        notificacion.setServicio(servicio);
        notificacion.setVehiculo(servicio.getVehiculos());
        notificacion.setCanal("whatsapp");
        notificacion.setTipoNotificacion("RECORDATORIO_SERVICIO");
        notificacion.setNombreServicio("Cambio de aceite");
        notificacion.setKilometrajeServicio(servicio.getKilometraje_servicio());
        notificacion.setFecha_programada(Timestamp.valueOf(fechaRecordatorio.atTime(12, 00)));
        notificacion.setFechaProximoServicio(Timestamp.valueOf(fechaProximoServicio.atStartOfDay()));
        notificacion.setEstado("pendiente");
        notificacion.setIntentosEnvio(0);

        notificacionesRepository.save(notificacion);

        log.info("✅ Recordatorio #{} creado para: {}", numeroRecordatorio, fechaRecordatorio);
    }
}
