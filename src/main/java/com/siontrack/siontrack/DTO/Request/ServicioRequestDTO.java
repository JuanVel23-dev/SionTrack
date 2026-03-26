package com.siontrack.siontrack.DTO.Request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


import lombok.Getter;
import lombok.Setter;

public class ServicioRequestDTO {

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
    private Integer vehiculo_id;

    @Getter
    @Setter
    private Integer cliente_id;

    @Getter
    @Setter
    private PagoRequestDTO pago;

    @Getter
    @Setter
    private List<DetalleServicioRequestDTO> detalles;
    
}
