package com.siontrack.siontrack.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class DireccionesRequestDTO {

    @NotBlank(message = "La direccion es obligatoria")
    @Size(max = 300, message = "La direccion no puede exceder 300 caracteres")
    @Getter
    @Setter
    private String direccion;
}
