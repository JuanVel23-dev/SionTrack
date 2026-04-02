package com.siontrack.siontrack.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class TelefonosRequestDTO {

    @NotBlank(message = "El telefono es obligatorio")
    @Size(max = 20, message = "El telefono no puede exceder 20 caracteres")
    @Getter
    @Setter
    private String telefono;
}
