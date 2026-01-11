package com.siontrack.siontrack.DTO.Response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

public class ProductosResponseDTO {

    @Getter
    @Setter
    private Integer producto_id;

    @Getter
    @Setter
    private String nombre;

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
    private String estado;

    @Getter
    @Setter
    private Integer cantidad_disponible;

    @Getter
    @Setter
    private Integer stock_minimo;

    @Getter
    @Setter
    private String ubicacion;

    @Getter
    @Setter
    private LocalDateTime ultima_actualizacion;

    @Getter
    @Setter
    private Integer proveedor_id;

    @Getter
    @Setter
    private ProveedoresResponseDTO proveedor;

    @Getter
    @Setter
    private boolean alerta_stock;
    
}
