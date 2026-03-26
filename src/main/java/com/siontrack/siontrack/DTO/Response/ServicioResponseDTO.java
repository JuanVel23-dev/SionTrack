package com.siontrack.siontrack.DTO.Response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


import lombok.Getter;
import lombok.Setter;

public class ServicioResponseDTO {

    @Getter
    @Setter
    private int servicio_id;

    @Getter
    @Setter
    private LocalDate fecha_servicio;

    @Getter
    @Setter
    private String kilometraje_servicio;

    @Getter
    @Setter
    private BigDecimal total;

    @Getter
    @Setter
    private String tipo_servicio;

    @Getter
    @Setter
    private String observaciones;
    
    @Getter
    @Setter
    private LocalDateTime creadoEn;

    @Getter
    @Setter
    private VehiculosResponseDTO vehiculo;

    @Getter
    @Setter
    private ClienteResponseDTO cliente;

    @Getter
    @Setter
    private List<DetalleServicioResponseDTO> detalles;

    @Getter
    @Setter
    private PagoResponseDTO pago;
}
