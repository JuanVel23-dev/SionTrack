package com.siontrack.siontrack.models;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
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

    @Column(nullable = false, length = 25)
    @Getter
    @Setter
    private String canal;

    @Getter
    @Setter
    private Timestamp fecha_programada;

    @Getter
    @Setter
    private Timestamp fecha_envio;

    @Column(nullable = false, length = 25)
    @Getter
    @Setter
    private String estado;

    @Getter
    @Setter
    private LocalDateTime creado_en;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonBackReference("notificaciones_clientes")
    @Getter
    @Setter
    private Clientes clientes;

    @Getter @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = true)
    @JsonBackReference("notificaciones_servicios")
    private Servicios servicio;

    @Getter @Setter
    @Column(name = "tipo_notificacion")
    private String tipoNotificacion; 

    @Getter @Setter
    @Column(name = "nombre_servicio")
    private String nombreServicio;  

    @Getter @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id")
    private Vehiculos vehiculo;

    @Getter @Setter
    @Column(name = "kilometraje_servicio")
    private String kilometrajeServicio;

    @Getter @Setter
    @Column(name = "fecha_proximo_servicio")
    private Timestamp fechaProximoServicio;

    @Getter @Setter
    @Column(name = "resultado_envio")
    private String resultadoEnvio;  

    @Getter @Setter
    @Column(name = "respuesta_recibida")
    private Boolean respuestaRecibida = false;

    @Getter @Setter
    @Column(name = "contenido_respuesta")
    private String contenidoRespuesta;

    @Getter @Setter
    @Column(name = "intentos_envio")
    private int intentosEnvio;
    
    @PrePersist
    protected void asignarFecha() {
        creado_en = LocalDateTime.now();
        if (estado == null) {
            estado = "pendiente";
        }
    }
}
