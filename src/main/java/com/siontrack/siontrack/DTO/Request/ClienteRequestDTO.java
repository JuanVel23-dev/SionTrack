package com.siontrack.siontrack.DTO.Request;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class ClienteRequestDTO {

    @Getter
    @Setter
    private Integer cliente_id;
    @Getter
    @Setter
    private String nombre;
    @Getter
    @Setter
    private String cedula_ruc;
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
    private List<TelefonosRequestDTO> telefonos;
    @Getter
    @Setter
    private List<DireccionesRequestDTO> direcciones;
    @Getter
    @Setter
    private List<CorreosRequestDTO> correos;
    @Getter
    @Setter
    private List<VehiculosRequestDTO> vehiculos;

}
