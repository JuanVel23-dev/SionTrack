package com.siontrack.siontrack.DTO.Request;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

public class ProductosRequestDTO {

    @Getter
    @Setter
    private String nombre;

    @Getter
    @Setter
    private String codigo_producto;
    
    @Getter
    @Setter
    private String categoria;

    @Getter
    @Setter
    private String marca;

    @Getter
    @Setter
    private String unidad_medida;

    @Getter
    @Setter
    private BigDecimal precio_compra;

    @Getter
    @Setter
    private BigDecimal precio_venta;

    @Getter
    @Setter
    private LocalDate fecha_compra;

    @Getter
    @Setter
    private Integer proveedor_id;

    @Getter
    @Setter
    private Integer cantidad_disponible; 

    @Getter
    @Setter
    private Integer stock_minimo;        

    @Getter
    @Setter
    private String ubicacion;    
    
    
}
