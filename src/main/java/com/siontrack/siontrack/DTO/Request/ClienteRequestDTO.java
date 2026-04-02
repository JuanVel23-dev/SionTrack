package com.siontrack.siontrack.DTO.Request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class ClienteRequestDTO {

    @Getter
    @Setter
    private Integer cliente_id;

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    @Getter
    @Setter
    private String nombre;

    @NotBlank(message = "La cedula o RUC es obligatoria")
    @Size(max = 20, message = "La cedula/RUC no puede exceder 20 caracteres")
    @Getter
    @Setter
    private String cedula_ruc;

    @NotBlank(message = "El tipo de cliente es obligatorio")
    @Size(max = 25, message = "El tipo de cliente no puede exceder 25 caracteres")
    @Getter
    @Setter
    private String tipo_cliente;

    @Getter
    @Setter
    private LocalDate fecha_registro;

    @Getter
    @Setter
    private LocalDate fecha_modificacion;

    @Getter
    @Setter
    private Boolean recibe_notificaciones;

    @Valid
    @Getter
    @Setter
    private List<TelefonosRequestDTO> telefonos;

    @Valid
    @Getter
    @Setter
    private List<DireccionesRequestDTO> direcciones;

    @Valid
    @Getter
    @Setter
    private List<CorreosRequestDTO> correos;

    @Valid
    @Getter
    @Setter
    private List<VehiculosRequestDTO> vehiculos;

}
