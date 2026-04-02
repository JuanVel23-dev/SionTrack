package com.siontrack.siontrack.DTO.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class ProveedoresRequestDTO {

    @NotBlank(message = "El nombre del proveedor es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    @Getter
    @Setter
    private String nombre;

    @Size(max = 20, message = "El telefono no puede exceder 20 caracteres")
    @Getter
    @Setter
    private String telefono;

    @Email(message = "El email debe tener un formato valido")
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    @Getter
    @Setter
    private String email;

    @Size(max = 200, message = "La direccion no puede exceder 200 caracteres")
    @Getter
    @Setter
    private String direccion;

    @Size(max = 100, message = "El nombre de contacto no puede exceder 100 caracteres")
    @Getter
    @Setter
    private String nombre_contacto;
}
