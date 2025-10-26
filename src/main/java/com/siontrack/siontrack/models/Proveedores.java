package com.siontrack.siontrack.models;

import java.util.ArrayList;
import java.util.List;

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
@Table(name = "proveedores")
public class Proveedores {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    @Setter
    private int proveedor_id;

    @Getter
    @Setter
    private String nombre;

    @Getter
    @Setter
    private String telefono;

    @Getter
    @Setter
    private String email;

    @Getter
    @Setter
    private String direccion;

    @Getter
    @Setter
    private String nombre_contacto;

    @OneToMany(mappedBy = "proveedor", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @Getter @Setter
    private List<Productos> productos = new ArrayList<>();
}
