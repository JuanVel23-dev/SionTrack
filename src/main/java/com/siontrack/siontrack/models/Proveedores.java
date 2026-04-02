package com.siontrack.siontrack.models;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "proveedores")
public class Proveedores {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    @Setter
    private int proveedor_id;

    @Column(nullable = false, length = 100)
    @Getter
    @Setter
    private String nombre;

    @Column(length = 20)
    @Getter
    @Setter
    private String telefono;

    @Column(length = 100)
    @Getter
    @Setter
    private String email;

    @Column(length = 200)
    @Getter
    @Setter
    private String direccion;

    @Column(length = 100)
    @Getter
    @Setter
    private String nombre_contacto;

    @OneToMany(mappedBy = "proveedor", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JsonManagedReference("productos")
    @Getter 
    @Setter
    private List<Productos> productos = new ArrayList<>();
}
