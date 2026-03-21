package com.siontrack.siontrack.DTO.Response;

import lombok.Getter;
import lombok.Setter;

public class VehiculosResponseDTO {

    @Getter @Setter
    private Integer vehiculo_id;

    @Getter @Setter
    private String placa;

    @Getter @Setter
    private String kilometraje_actual;
}
