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
    private String nivelAlerta;      // "AGOTADO", "CRITICO", "BAJO", "ADVERTENCIA"
    private Integer cantidadNecesaria;

    // Popularidad
    private boolean esPopular;       // true si está en el top 5 de productos más vendidos
    private Integer rankingPopular;  // 1-5 si es popular, null si no
    private Long totalVendido;       // cantidad total vendida (para contexto)

    // Prioridad combinada: popularidad + nivel de stock
    // Valores más altos = más urgente
    private Integer prioridadCompuesta;

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

    /**
     * Retorna la etiqueta de urgencia para el frontend.
     * Combina nivel de stock + popularidad para dar contexto.
     */
    public String getEtiquetaUrgencia() {
        if (esPopular && ("AGOTADO".equals(nivelAlerta) || "CRITICO".equals(nivelAlerta))) {
            return "URGENTE";
        }
        if (esPopular && "BAJO".equals(nivelAlerta)) {
            return "PRIORITARIO";
        }
        return nivelAlerta;
    }
}