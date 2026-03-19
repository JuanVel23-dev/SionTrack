package com.siontrack.siontrack.DTO.Request;

import lombok.Getter;
import lombok.Setter;

public class PromocionesRequestDTO {

    @Getter @Setter
    private String nombreCliente;

    @Getter @Setter
    private Integer productoId;

    @Getter @Setter
    private String promocion;

    @Getter @Setter
    private String rangoFechas;

    @Getter @Setter 
    private String precioOferta;

}
