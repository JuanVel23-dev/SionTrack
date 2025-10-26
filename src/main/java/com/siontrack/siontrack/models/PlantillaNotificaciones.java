package com.siontrack.siontrack.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "plantillas_notificaciones")
public class PlantillaNotificaciones {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    @Setter
    private int plantilla_id;

    @Getter
    @Setter
    private String nombre;

    @Getter
    @Setter
    private String tipo;

    @Getter
    @Setter
    private String mensaje_base;

    @Getter
    @Setter
    private boolean activo;

    @Getter
    @Setter
    private LocalDateTime fecha_creacion;

    @Getter
    @Setter
    private LocalDateTime ultima_actualizacion;

    @OneToMany(mappedBy = "plantilla", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("notificaciones")
    @Getter
    @Setter
    private List<Notificaciones> notificaciones = new ArrayList<>();

    @PrePersist
    protected void asignarFecha() {
        fecha_creacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.ultima_actualizacion = LocalDateTime.now();
    }
}
