package com.siontrack.siontrack.DTO.Request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class ProductosRequestDTO {

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    @Getter
    @Setter
    private String nombre;

    @Size(max = 50, message = "El codigo de producto no puede exceder 50 caracteres")
    @Getter
    @Setter
    private String codigo_producto;

    @Size(max = 50, message = "La categoria no puede exceder 50 caracteres")
    @Getter
    @Setter
    private String categoria;

    @Size(max = 50, message = "La marca no puede exceder 50 caracteres")
    @Getter
    @Setter
    private String marca;

    @Size(max = 30, message = "La unidad de medida no puede exceder 30 caracteres")
    @Getter
    @Setter
    private String unidad_medida;

    @NotNull(message = "El precio de compra es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo")
    @Getter
    @Setter
    private BigDecimal precio_compra;

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio de venta no puede ser negativo")
    @Getter
    @Setter
    private BigDecimal precio_venta;

    @Getter
    @Setter
    private LocalDate fecha_compra;

    @Getter
    @Setter
    private Integer proveedor_id;

    @Min(value = 0, message = "La cantidad disponible no puede ser negativa")
    @Getter
    @Setter
    private Integer cantidad_disponible;

    @Min(value = 0, message = "El stock minimo no puede ser negativo")
    @Getter
    @Setter
    private Integer stock_minimo;

    @Size(max = 100, message = "La ubicacion no puede exceder 100 caracteres")
    @Getter
    @Setter
    private String ubicacion;

}
