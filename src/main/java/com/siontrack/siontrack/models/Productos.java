package com.siontrack.siontrack.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "productos")
public class Productos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    @Setter
    private int producto_id;

    @Column(nullable = false, length = 100)
    @Getter
    @Setter
    private String nombre;

    @Column(name = "codigo_producto", unique = true, length = 50)
    @Getter
    @Setter
    private String codigoProducto;

    @Column(length = 50)
    @Getter
    @Setter
    private String categoria;

    @Column(nullable = false)
    @Getter
    @Setter
    private BigDecimal precio_compra;

    @Column(nullable = false)
    @Getter
    @Setter
    private BigDecimal precio_venta;

    @Column(name = "fecha_compra")
    @Getter
    @Setter
    private LocalDate fecha_compra;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    @JsonBackReference("productos")
    @Getter
    @Setter
    private Proveedores proveedor;

    @OneToOne(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("inventario")
    @Getter @Setter
    private Inventario inventario;

}
