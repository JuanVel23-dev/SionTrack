package com.siontrack.siontrack.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertaStockDTO {

    private Integer productoId;
    private String nombre;
    private String categoria;
    private String marca;
    private Integer cantidadDisponible;
    private Integer stockMinimo;
    private String ubicacion;
    private String nivelAlerta; // "AGOTADO", "CRITICO", "BAJO"
    private Integer cantidadNecesaria;

    // Proveedor
    private Integer proveedorId;
    private String proveedorNombre;
    private String proveedorTelefono;
    private String proveedorEmail;
    private String proveedorDireccion;

    public int getPorcentajeStock() {
        if (stockMinimo == null || stockMinimo == 0) return 100;
        if (cantidadDisponible == null || cantidadDisponible <= 0) return 0;
        int porcentaje = (int) ((cantidadDisponible * 100.0) / (stockMinimo * 2));
        return Math.min(porcentaje, 100);
    }
}