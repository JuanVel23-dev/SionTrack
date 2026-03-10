package com.siontrack.siontrack.DTO.Response;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

public class RecordatorioDTO {

    @Getter @Setter
    private Integer notificacionId;

    @Getter @Setter
    private String clienteNombre;

    @Getter @Setter
    private String telefono;

    @Getter @Setter
    private String vehiculoInfo;

    @Getter @Setter
    private String placa;

    @Getter @Setter
    private String nombreServicio;

    @Getter @Setter
    private String kilometrajeServicio;

    @Getter @Setter
    private String estado;

    @Getter @Setter
    private String resultadoEnvio;

    @Getter @Setter
    private Timestamp fechaProgramada;

    @Getter @Setter
    private Timestamp fechaEnvio;

    @Getter @Setter
    private LocalDateTime creadoEn;
}