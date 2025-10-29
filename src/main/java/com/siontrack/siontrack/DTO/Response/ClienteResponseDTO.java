package com.siontrack.siontrack.DTO.Response;

import java.time.LocalDate;
import java.util.List;


import lombok.Getter;
import lombok.Setter;

public class ClienteResponseDTO {

    @Getter
    @Setter
    private int cliente_id;
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
    private List<TelefonosResponseDTO> telefonos;
    @Getter
    @Setter
    private List<DireccionesResponseDTO> direcciones;
    @Getter
    @Setter
    private List<CorreosResponseDTO> correos;
    @Getter
    @Setter
    private List<VehiculosResponseDTO> vehículos;
}
