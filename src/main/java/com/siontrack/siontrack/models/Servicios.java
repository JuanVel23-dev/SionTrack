package com.siontrack.siontrack.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "servicios")
public class Servicios {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    @Setter
    private int servicio_id;

    @Getter
    @Setter
    private LocalDate fecha_servicio;

    @Getter
    @Setter
    private int kilometraje_servicio;

    @Getter
    @Setter
    private BigDecimal total;

    @Getter
    @Setter
    private String estado;

    @Getter
    @Setter
    private String observaciones;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Getter
    @Setter
    private LocalDateTime creado_en;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    @JsonIgnore
    @Getter
    @Setter
    private Vehiculos vehiculos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonBackReference("servicios")
    @Getter
    @Setter
    private Clientes clientes;

    @OneToOne(mappedBy = "servicio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("pagos")
    @Getter
    @Setter
    private Pagos pago;

    @OneToMany(mappedBy = "servicio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("detalle_servicio")
    @Getter
    @Setter
    private List<Detalle_Servicio> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "servicio", cascade = CascadeType.ALL)
    @JsonManagedReference("notificaciones_servicios")
    @Getter
    @Setter
    private List<Notificaciones> notificaciones = new ArrayList<>();

    @PrePersist
    protected void asignarFecha() {
        creado_en = LocalDateTime.now();
    }

}
