package com.siontrack.siontrack.models.enums;

import java.util.Optional;

import lombok.Getter;

public enum TipoServicio {

    CAMBIO_ACEITE("aceite", 6, 2);

    @Getter
    private final String palabraClave;

    @Getter
    private final int frecuencia;

    @Getter
    private final int fechaMinima;

    TipoServicio(String palabraClave, int frecuencia, int fechaMinima) {
        this.palabraClave = palabraClave;
        this.frecuencia = frecuencia;
        this.fechaMinima = fechaMinima;
    }

    public static Optional<TipoServicio> detectar(String nombreProducto) {
        if (nombreProducto == null || nombreProducto.isBlank()) {
            return Optional.empty();
        }

        String nombreLower = nombreProducto.toLowerCase();

        // Debug
        System.out.println("🔍 Buscando en: " + nombreLower);
        System.out.println("🔍 Contiene 'aceite': " + nombreLower.contains("aceite"));

        if (nombreLower.contains(CAMBIO_ACEITE.palabraClave)) {
            return Optional.of(CAMBIO_ACEITE);
        }

        return Optional.empty();
    }

}
