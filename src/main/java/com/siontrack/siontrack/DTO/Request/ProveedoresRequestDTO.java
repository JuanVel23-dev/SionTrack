package com.siontrack.siontrack.DTO.Request;


import lombok.Getter;
import lombok.Setter;

public class ProveedoresRequestDTO {

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
}
