package com.siontrack.siontrack.models;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notificaciones")
public class Notificaciones {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    @Setter
    private int notificacion_id;

    @Getter
    @Setter
    private String mensaje_enviado;

    @Getter
    @Setter
    private String canal;

    @Getter
    @Setter
    private Timestamp fecha_programada;

    @Getter
    @Setter
    private Timestamp fecha_envio;

    @Getter
    @Setter
    private String estado;

    @Getter
    @Setter
    private LocalDateTime creado_en;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @Getter
    @Setter
    private Clientes clientes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plantilla_id", nullable = false)
    @Getter
    @Setter
    private PlantillaNotificaciones plantilla;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = true)
    private Servicios servicio;

    @PrePersist
    protected void asignarFecha() {
        creado_en = LocalDateTime.now();
    }
}
