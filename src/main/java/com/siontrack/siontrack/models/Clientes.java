package com.siontrack.siontrack.models;

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
    @Getter
    @Setter
    private int cliente_id;

    @Getter
    @Setter
    private String nombre;

    @Column(name = "cedula_ruc", updatable = true)
    @Getter
    @Setter
    private String cedula_ruc;

    @Getter
    @Setter
    private String tipo_cliente;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    @Getter
    @Setter
    private LocalDate fecha_registro;

    @Column(name = "fecha_ultima_modificacion", nullable = true, updatable = true)
    @Getter
    @Setter
    private LocalDate fecha_modificacion;

    @OneToMany(mappedBy = "clientes", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("cliente_telefonos")
    @Getter
    @Setter
    private List<Cliente_Telefonos> telefonos = new ArrayList<>();

    @OneToMany(mappedBy = "clientes", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("cliente_correos")
    @Getter
    @Setter
    private List<Cliente_Correos> correos = new ArrayList<>();

    @OneToMany(mappedBy = "clientes", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("cliente_direcciones")
    @Getter
    @Setter
    private List<Cliente_Direcciones> direcciones = new ArrayList<>();

    @OneToMany(mappedBy = "clientes", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("vehiculos")
    @Getter
    @Setter
    private List<Vehiculos> vehiculos = new ArrayList<>();

    @OneToMany(mappedBy = "clientes", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("servicios")
    @Getter
    @Setter
    private List<Servicios> servicios = new ArrayList<>();

    @OneToMany(mappedBy = "clientes", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("notificaciones_clientes")
    @Getter
    @Setter
    private List<Notificaciones> notificaciones = new ArrayList<>();

    @PrePersist
    protected void asignarFecha() {
        fecha_registro = LocalDate.now();
    }

}
