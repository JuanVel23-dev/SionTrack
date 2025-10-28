package com.siontrack.siontrack.DTO.Response;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

public class PagoResponseDTO {

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

}
