package com.siontrack.siontrack.models;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "detalle_servicio")
public class Detalle_Servicio {

    public enum tipoItem{
        PRODUCTO,
        SERVICIO,
        INSUMO, 
        PAQUETE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private int detalle_id;

    @Getter @Setter
    private BigDecimal cantidad;

    @Getter @Setter
    private BigDecimal precio_unitario_congelado;

    @Enumerated(EnumType.STRING) 
    @Column(name = "tipo_item", nullable = false, length = 25)
    @Getter @Setter
    private tipoItem tipo = tipoItem.PRODUCTO; 
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = false)           // ← QUITADO unique = true
    @JsonBackReference("detalle_servicio")
    @Getter @Setter
    private Servicios servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)           // ← QUITADO unique = true
    @JsonBackReference("detalle_servicio_producto")
    @Getter @Setter
    private Productos producto;

}