package com.siontrack.siontrack.models;


import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "inventario", schema = "siontrack")
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private int inventario_id;

    @Getter @Setter
    private int cantidad_disponible;

    @Getter @Setter
    private int stock_minimo;

    @Getter @Setter
    private String ubicacion;

    @Getter @Setter
    private LocalDateTime ultima_actualizacion;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id",nullable = false, unique = true)
    @JsonBackReference("inventario")
    @Getter @Setter
    private Productos producto;

    @PrePersist
    protected void onCreate() {
        this.ultima_actualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.ultima_actualizacion = LocalDateTime.now();
    }
}
