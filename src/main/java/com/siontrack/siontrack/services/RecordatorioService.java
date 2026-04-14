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

/**
 * Gestiona la creación automática de recordatorios de servicio para los clientes.
 *
 * <p>Cada vez que se registra un servicio con kilometraje, se programan hasta dos
 * notificaciones por WhatsApp:
 * <ul>
 *   <li><b>Primer recordatorio</b>: {@value #mes_primer_recordatorio} mes antes de la
 *       fecha estimada del próximo servicio.</li>
 *   <li><b>Segundo recordatorio</b>: el día de la fecha estimada del próximo servicio.</li>
 * </ul>
 *
 * <p>La fecha estimada del próximo servicio se calcula sumando
 * {@value #meses_proximo_servicio} meses a la fecha del servicio registrado.
 *
 * <p>Si el cliente ya tiene recordatorios pendientes para el mismo vehículo, estos se
 * reemplazan con los valores del servicio más reciente para mantener la vigencia.
 */
@Service
public class RecordatorioService {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioService.class);

    /** Meses que se estiman entre un servicio y el siguiente. */
    private static final int meses_proximo_servicio = 8;

    /** Meses de anticipación con los que se envía el primer recordatorio. */
    private static final int mes_primer_recordatorio = 1;

    private final NotificacionesRepository notificacionesRepository;

    public RecordatorioService(NotificacionesRepository notificacionesRepository) {
        this.notificacionesRepository = notificacionesRepository;
    }

    /**
     * Punto de entrada principal. Recibe un servicio recién guardado, valida que tenga
     * kilometraje (requisito de negocio para generar recordatorios) y delega en
     * {@link #crearNotificaciones(Servicios)}.
     *
     * <p>Si ya existen recordatorios pendientes para el mismo cliente y vehículo,
     * se eliminan antes de crear los nuevos para que siempre reflejen el último servicio.
     *
     * @param servicio servicio persistido desde el que se calcularán los recordatorios
     */
    @Transactional
    public void procesarServicioParaRecordatorios(Servicios servicio) {
        log.info("Procesando servicio {} para recordatorios", servicio.getServicio_id());

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

        List<Notificaciones> pendientesExistentes = notificacionesRepository
                .findRecordatoriosPendientesByClienteVehiculo(clienteId, vehiculoId);

        if (!pendientesExistentes.isEmpty()) {
            // Los recordatorios existentes se eliminan y se recrean con el nuevo servicio
            // para que siempre apunten al mantenimiento más reciente del vehículo
            log.info("Se encontraron {} recordatorio(s) pendiente(s) para cliente {} / vehículo {}. Reemplazando...",
                    pendientesExistentes.size(), clienteId, vehiculoId);
            notificacionesRepository.deleteAll(pendientesExistentes);
        }

        crearNotificaciones(servicio);
    }

    /**
     * Calcula las fechas de los recordatorios a partir de la fecha del servicio y los
     * persiste si aún están en el futuro.
     */
    private void crearNotificaciones(Servicios servicio) {
        LocalDate fechaServicio = servicio.getFecha_servicio();
        LocalDate fechaProximoServicio = fechaServicio.plusMonths(meses_proximo_servicio);
        LocalDate fechaPrimerRecordatorio = fechaProximoServicio.minusMonths(mes_primer_recordatorio);
        LocalDate fechaSegundoRecordatorio = fechaProximoServicio;

        log.info("Servicio: {} | Próximo: {} | Recordatorios: {} y {}",
                fechaServicio, fechaProximoServicio, fechaPrimerRecordatorio, fechaSegundoRecordatorio);

        Integer clienteId = servicio.getClientes().getCliente_id();
        Integer vehiculoId = servicio.getVehiculos() != null ? servicio.getVehiculos().getVehiculo_id() : null;
        Timestamp fechaProximoServicioTs = Timestamp.valueOf(fechaProximoServicio.atStartOfDay());

        if (notificacionesRepository.existsRecordatorioDuplicado(clienteId, vehiculoId, fechaProximoServicioTs)) {
            log.info("Ya existen recordatorios para cliente {} y vehículo {} con fecha objetivo {}",
                    clienteId, vehiculoId, fechaProximoServicio);
            return;
        }

        // Primer recordatorio: 1 mes antes del próximo servicio estimado
        if (fechaPrimerRecordatorio.isAfter(LocalDate.now()) || fechaPrimerRecordatorio.equals(LocalDate.now())) {
            crearNotificacion(servicio, fechaPrimerRecordatorio, fechaProximoServicio, 1);
        }

        // Segundo recordatorio: el día del próximo servicio estimado
        if (fechaSegundoRecordatorio.isAfter(LocalDate.now())) {
            crearNotificacion(servicio, fechaSegundoRecordatorio, fechaProximoServicio, 2);
        }
    }

    /**
     * Construye y persiste una {@link Notificaciones} de tipo {@code RECORDATORIO_SERVICIO}
     * programada para la fecha indicada.
     *
     * @param servicio             servicio de referencia
     * @param fechaRecordatorio    fecha en que se enviará el mensaje
     * @param fechaProximoServicio fecha estimada del próximo servicio (incluida en el mensaje)
     * @param numeroRecordatorio   número de secuencia (1 o 2) para el log
     */
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

        log.info("Recordatorio #{} creado para: {}", numeroRecordatorio, fechaRecordatorio);
    }
}
