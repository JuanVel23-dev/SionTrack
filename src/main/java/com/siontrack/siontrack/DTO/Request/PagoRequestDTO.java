package com.siontrack.siontrack.DTO.Request;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

public class PagoRequestDTO {

    @Getter
    @Setter
    private Integer pago_id;

    @Getter
    @Setter
    private String metodo_pago;

    @Getter
    @Setter
    private BigDecimal monto;

    @Getter
    @Setter
    private LocalDate fecha_pago;

    @Getter
    @Setter
    private Integer servicio_id;
}
