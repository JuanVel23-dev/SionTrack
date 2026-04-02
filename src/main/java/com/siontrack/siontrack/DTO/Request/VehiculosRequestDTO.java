package com.siontrack.siontrack.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class VehiculosRequestDTO {

    @NotBlank(message = "La placa del vehiculo es obligatoria")
    @Size(max = 10, message = "La placa no puede exceder 10 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "La placa solo puede contener letras, numeros y guiones")
    @Getter @Setter
    private String placa;

    @Size(max = 20, message = "El kilometraje no puede exceder 20 caracteres")
    @Getter @Setter
    private String kilometraje_actual;
}
