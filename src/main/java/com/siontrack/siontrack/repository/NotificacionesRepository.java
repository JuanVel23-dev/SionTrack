package com.siontrack.siontrack.repository;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.siontrack.siontrack.models.Notificaciones;
import com.siontrack.siontrack.models.Servicios;

@Repository
public interface NotificacionesRepository extends JpaRepository<Notificaciones, Integer>{

    @Query("""
        SELECT n FROM Notificaciones n
        WHERE n.fecha_programada <= :fecha
        AND n.canal = 'whatsapp'
        AND n.clientes.recibe_notificaciones = true
        AND (
            n.estado = 'pendiente'
            OR
            (n.estado = 'fallido' AND n.intentosEnvio < 2)
        )
        """)
    List<Notificaciones> findNotificacionesPendientes(@Param("fecha") Timestamp fecha);

    boolean existsByServicio(Servicios servicio);

    @Query("SELECT COUNT(n) > 0 FROM Notificaciones n " +
           "WHERE n.clientes.cliente_id = :clienteId " +
           "AND (:vehiculoId IS NULL OR n.vehiculo.vehiculo_id = :vehiculoId) " +
           "AND n.tipoNotificacion = 'RECORDATORIO_SERVICIO' " +
           "AND n.fechaProximoServicio = :fechaProximoServicio")
    boolean existsRecordatorioDuplicado(@Param("clienteId") Integer clienteId,
                                        @Param("vehiculoId") Integer vehiculoId,
                                        @Param("fechaProximoServicio") Timestamp fechaProximoServicio);

    @Query("SELECT DISTINCT n FROM Notificaciones n " +
           "LEFT JOIN FETCH n.clientes c " +
           "LEFT JOIN FETCH c.telefonos " +
           "LEFT JOIN FETCH n.vehiculo " +
           "WHERE n.tipoNotificacion = :tipo " +
           "ORDER BY n.creado_en DESC")
    List<Notificaciones> findByTipoNotificacionOrdenado(@Param("tipo") String tipoNotificacion);

    @Query("SELECT DISTINCT n FROM Notificaciones n " +
           "LEFT JOIN FETCH n.clientes c " +
           "LEFT JOIN FETCH c.telefonos " +
           "LEFT JOIN FETCH n.vehiculo " +
           "WHERE n.tipoNotificacion = 'RECORDATORIO_SERVICIO' " +
           "ORDER BY n.creado_en DESC")
    List<Notificaciones> findRecordatoriosOrdenados();

    // Busca recordatorios pendientes para un cliente+vehículo específico
    @Query("SELECT n FROM Notificaciones n " +
           "WHERE n.clientes.cliente_id = :clienteId " +
           "AND (:vehiculoId IS NULL OR n.vehiculo.vehiculo_id = :vehiculoId) " +
           "AND n.tipoNotificacion = 'RECORDATORIO_SERVICIO' " +
           "AND n.estado = 'pendiente'")
    List<Notificaciones> findRecordatoriosPendientesByClienteVehiculo(
            @Param("clienteId") Integer clienteId,
            @Param("vehiculoId") Integer vehiculoId);

    /**
     * Retorna el cliente_id y la fecha del último envío exitoso de promoción
     * para cada cliente de la lista proporcionada.
     * Se usa para detectar quiénes fueron contactados recientemente.
     */
    @Query("SELECT n.clientes.cliente_id, MAX(n.fecha_envio) " +
           "FROM Notificaciones n " +
           "WHERE n.tipoNotificacion = 'PROMOCION' " +
           "AND n.estado = 'enviado' " +
           "AND n.clientes.cliente_id IN :clienteIds " +
           "GROUP BY n.clientes.cliente_id")
    List<Object[]> findUltimaPromocionEnviadaPorClientes(@Param("clienteIds") List<Integer> clienteIds);
}