package com.siontrack.siontrack.DTO.Response;

import lombok.Getter;
import lombok.Setter;

public class ProveedoresResponseDTO {

    @Getter
    @Setter
    private Integer proveedor_id;
    
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
