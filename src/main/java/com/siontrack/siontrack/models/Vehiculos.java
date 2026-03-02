package com.siontrack.siontrack.models;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "vehiculos")
public class Vehiculos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private int vehiculo_id;

    @Getter @Setter
    private String marca;

    @Getter @Setter
    private String modelo;

    @Getter @Setter
    private int anio;

    @Getter @Setter
    private String placa;

    @Getter @Setter
    private String tipo_motor;

    @Getter @Setter
    private String kilometraje_actual;

    @Getter @Setter
    @Column(name = "creado_en" , nullable = false , updatable = false)
    private Timestamp creado_en;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonBackReference("vehiculos")
    @Getter @Setter
    private Clientes clientes;


    @OneToMany(mappedBy = "vehiculos", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Getter @Setter
    private List<Servicios> servicios = new ArrayList<>();

    @PrePersist
    protected void establecerFecha(){
        creado_en = Timestamp.valueOf(LocalDateTime.now());
    }
    

}
