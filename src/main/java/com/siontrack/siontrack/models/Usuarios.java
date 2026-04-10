package com.siontrack.siontrack.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuarios", schema = "siontrack")
public class Usuarios {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    @Setter
    private Integer usuario_id;

    @Getter
    @Setter
    @Column(nullable = false, unique = true, length = 50)
    private String nombreusuario;

    @Getter
    @Setter
    @Column(nullable = false)
    private String contrasena;

    @Getter
    @Setter
    @Column(length = 100)
    private String nombre;

    @Getter
    @Setter
    @Column(length = 100)
    private String email;

    @Getter
    @Setter
    @Column(nullable = false, length = 20)
    private String rol = "ADMIN";

    @Getter
    @Setter
    @Column(nullable = false)
    private Boolean activo = true;

    @Getter
    @Setter
    @Column(name = "creado_en")
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Getter
    @Setter
    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;
}
