package com.siontrack.siontrack.DTO.Request;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class PromocionesRequestDTO {

    @Getter @Setter
    private String nombreCliente;

    @Getter @Setter
    private Integer productoId;

    @Getter @Setter
    private String promocion;

    @Getter @Setter
    private String rangoFechas;

    @Getter @Setter
    private String precioOferta;

    /**
     * IDs de los clientes seleccionados manualmente en la vista previa.
     * Si está vacío o nulo, se envía a todos los clientes elegibles del producto.
     */
    @Getter @Setter
    private List<Integer> clientesSeleccionados;

}
