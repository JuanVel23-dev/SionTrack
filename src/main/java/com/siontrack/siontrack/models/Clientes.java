package com.siontrack.siontrack.models;


import java.security.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "clientes")
public class Clientes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter 
    private int cliente_id;

    @Getter @Setter 
    private String nombre;

    @Getter @Setter 
    private String cedula_ruc;

    @Getter @Setter 
    private String tipo_cliente;

    @Getter @Setter 
    private LocalDate fecha_registro;
    
    @OneToMany(mappedBy = "clientes", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Getter @Setter
    private List<Cliente_Telefonos> telefonos = new ArrayList<>();

    @OneToMany(mappedBy = "clientes", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Getter @Setter
    private List<Cliente_Correos> correos = new ArrayList<>();

    @OneToMany(mappedBy = "clientes", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Getter @Setter
    private List<Cliente_Direcciones> direcciones = new ArrayList<>();

    @OneToMany(mappedBy = "clientes", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Getter @Setter
    private List<Vehiculos> vehiculos = new ArrayList<>();

    @OneToMany(mappedBy = "clientes", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Getter @Setter
    private List<Servicios> servicios = new ArrayList<>();
    
    

}
