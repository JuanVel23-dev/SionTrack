package com.siontrack.siontrack.DTO.Response;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

public class PromocionEnviadaDTO {

    @Getter @Setter
    private Integer notificacionId;

    @Getter @Setter
    private String clienteNombre;

    @Getter @Setter
    private String telefono;

    @Getter @Setter
    private String mensajeEnviado;

    @Getter @Setter
    private String estado;

    @Getter @Setter
    private String resultadoEnvio;

    @Getter @Setter
    private LocalDateTime fechaEnvio;

    @Getter @Setter
    private String vehiculoInfo;

    @Getter @Setter
    private String placa;
}