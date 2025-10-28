package com.siontrack.siontrack.DTO.Response;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

public class DetalleServicioResponseDTO {

    @Getter
    @Setter
    private int detalle_id;

    @Getter
    @Setter
    private BigDecimal cantidad;

    @Getter
    @Setter
    private BigDecimal precio_unitario_congelado;

    @Getter
    @Setter
    private String tipoItem;

    @Getter
    @Setter
    private String nombre_producto;
}
