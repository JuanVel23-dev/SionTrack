package com.siontrack.siontrack.DTO.Response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProductoPopularDTO {

    private Integer productoId;
    private String nombre;
    private String categoria;
    private Long totalVendido;

    public ProductoPopularDTO(Integer productoId, String nombre, String categoria, Number totalVendido) {
        this.productoId = productoId;
        this.nombre = nombre;
        this.categoria = categoria;
        this.totalVendido = totalVendido != null ? totalVendido.longValue() : 0L;
    }
}
