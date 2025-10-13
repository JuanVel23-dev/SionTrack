package com.siontrack.siontrack.models;



import java.security.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "movimientos_inventario")
public class Mov_Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private int movimiento_id;

    @Getter @Setter
    private String tipo_movimiento;

    @Getter @Setter
    private int cantidad;

    @Getter @Setter 
    private LocalDateTime fecha;

    /* 
    @OneToMany(mappedBy = "movimientos", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Getter @Setter
    private List<Productos> productos = new ArrayList<>();

    @OneToMany(mappedBy = "proveedores", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Getter @Setter
    private List<Proveedores> proveedores = new ArrayList<>();

    @OneToMany(mappedBy = "movimientos", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Getter @Setter
    private List<Servicios> servicios = new ArrayList<>();
    */
}
