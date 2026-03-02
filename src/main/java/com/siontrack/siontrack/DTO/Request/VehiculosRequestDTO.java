package com.siontrack.siontrack.DTO.Request;

import java.security.Timestamp;

import lombok.Getter;
import lombok.Setter;

public class VehiculosRequestDTO {

    @Getter
    @Setter
    private String marca;
    @Getter
    @Setter
    private String modelo;
    @Getter
    @Setter
    private Integer anio;
    @Getter
    @Setter
    private String placa;
    @Getter
    @Setter
    private String tipo_motor;
    @Getter
    @Setter
    private String kilometraje_actual;
    @Getter
    @Setter
    private Timestamp creado_en;
}
