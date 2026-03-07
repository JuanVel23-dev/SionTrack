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

}

