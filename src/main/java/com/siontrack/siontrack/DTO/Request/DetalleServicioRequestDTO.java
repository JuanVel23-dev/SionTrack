package com.siontrack.siontrack.DTO.Request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class DetalleServicioRequestDTO {

    @Getter
    @Setter
    private int detalle_id;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a cero")
    @Getter
    @Setter
    private BigDecimal cantidad;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio unitario no puede ser negativo")
    @Getter
    @Setter
    private BigDecimal precio_unitario_congelado;

    @Size(max = 30, message = "El tipo de item no puede exceder 30 caracteres")
    @Getter
    @Setter
    private String tipoItem;

    @Getter
    @Setter
    private Integer producto_id;

}
