package com.siontrack.siontrack.DTO.Request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class PromocionesRequestDTO {

    @Size(max = 100, message = "El nombre del cliente no puede exceder 100 caracteres")
    @Getter @Setter
    private String nombreCliente;

    @NotNull(message = "El producto es obligatorio")
    @Getter @Setter
    private Integer productoId;

    @NotBlank(message = "El texto de la promocion es obligatorio")
    @Size(max = 500, message = "La promocion no puede exceder 500 caracteres")
    @Getter @Setter
    private String promocion;

    @Size(max = 50, message = "El rango de fechas no puede exceder 50 caracteres")
    @Getter @Setter
    private String rangoFechas;

    @Size(max = 30, message = "El precio de oferta no puede exceder 30 caracteres")
    @Getter @Setter
    private String precioOferta;

    /**
     * IDs de los clientes seleccionados manualmente en la vista previa.
     * Si esta vacio o nulo, se envia a todos los clientes elegibles del producto.
     */
    @Getter @Setter
    private List<Integer> clientesSeleccionados;

}
