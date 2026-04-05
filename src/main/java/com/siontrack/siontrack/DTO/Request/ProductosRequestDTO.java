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

    @NotBlank(message = "El código del producto es obligatorio")
    @Size(max = 50, message = "El código de producto no puede exceder 50 caracteres")
    @Getter
    @Setter
    private String codigo_producto;

    @NotBlank(message = "La categoría es obligatoria")
    @Size(max = 50, message = "La categoría no puede exceder 50 caracteres")
    @Getter
    @Setter
    private String categoria;

    @NotNull(message = "El precio de compra es obligatorio")
    @DecimalMin(value = "0.01", inclusive = true, message = "El precio de compra debe ser mayor a cero")
    @Getter
    @Setter
    private BigDecimal precio_compra;

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.01", inclusive = true, message = "El precio de venta debe ser mayor a cero")
    @Getter
    @Setter
    private BigDecimal precio_venta;

    @NotNull(message = "La fecha de compra es obligatoria")
    @Getter
    @Setter
    private LocalDate fecha_compra;

    @NotNull(message = "El proveedor es obligatorio")
    @Getter
    @Setter
    private Integer proveedor_id;

    @NotNull(message = "La cantidad disponible es obligatoria")
    @Min(value = 1, message = "La cantidad disponible debe ser al menos 1")
    @Getter
    @Setter
    private Integer cantidad_disponible;

    @NotNull(message = "El stock mínimo es obligatorio")
    @Min(value = 1, message = "El stock mínimo debe ser al menos 1")
    @Getter
    @Setter
    private Integer stock_minimo;

}
