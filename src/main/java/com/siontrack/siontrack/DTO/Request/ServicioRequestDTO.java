package com.siontrack.siontrack.DTO.Request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class ServicioRequestDTO {

    @Getter
    @Setter
    private int servicio_id;

    @NotNull(message = "La fecha del servicio es obligatoria")
    @Getter
    @Setter
    private LocalDate fecha_servicio;

    @Size(max = 20, message = "El kilometraje no puede exceder 20 caracteres")
    @Getter
    @Setter
    private String kilometraje_servicio;

    @DecimalMin(value = "0.00", message = "El total no puede ser negativo")
    @Getter
    @Setter
    private BigDecimal total;

    @NotBlank(message = "El tipo de servicio es obligatorio")
    @Size(max = 50, message = "El tipo de servicio no puede exceder 50 caracteres")
    @Getter
    @Setter
    private String tipo_servicio;

    @Size(max = 1000, message = "Las observaciones no pueden exceder 1000 caracteres")
    @Getter
    @Setter
    private String observaciones;

    @Getter
    @Setter
    private Integer vehiculo_id;

    @NotNull(message = "El cliente es obligatorio")
    @Getter
    @Setter
    private Integer cliente_id;

    @Getter
    @Setter
    private PagoRequestDTO pago;

    @Valid
    @NotEmpty(message = "El servicio debe tener al menos un detalle")
    @Getter
    @Setter
    private List<DetalleServicioRequestDTO> detalles;

}
