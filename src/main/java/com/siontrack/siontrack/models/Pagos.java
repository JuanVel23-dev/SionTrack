package com.siontrack.siontrack.models;



import java.math.BigDecimal;
import java.security.Timestamp;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pagos")
public class Pagos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private int pago_id;

    @Getter @Setter
    private String metodo_pago;

    @Getter @Setter
    private BigDecimal monto;

    @Getter @Setter 
    private LocalDate fecha_pago;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = false, unique = true)
    @Getter @Setter
    private Servicios servicio;


}
