package com.siontrack.siontrack.DTO.Request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class PagoRequestDTO {

    @Getter
    @Setter
    private Integer pago_id;

    @NotBlank(message = "El metodo de pago es obligatorio")
    @Size(max = 25, message = "El metodo de pago no puede exceder 25 caracteres")
    @Getter
    @Setter
    private String metodo_pago;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @Getter
    @Setter
    private BigDecimal monto;

    @NotNull(message = "La fecha de pago es obligatoria")
    @Getter
    @Setter
    private LocalDate fecha_pago;

    @Getter
    @Setter
    private Integer servicio_id;
}
