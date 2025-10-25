package com.siontrack.siontrack.models;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "detalle_servicio")
public class Detalle_Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private int detalle_id;

    @Getter @Setter
    private BigDecimal cantidad;

    @Getter @Setter
    private BigDecimal precio_unitario_congelado;

    /*
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = false, unique = true)
    @Getter @Setter
    private Servicios servicio;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false, unique = true)
    @Getter @Setter
    private Productos producto;
    */
}
