package com.siontrack.siontrack.DTO.Response;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO para la vista previa de clientes antes de enviar una promoción.
 * Incluye flags de elegibilidad y estado de contacto reciente.
 */
@Getter
@Setter
public class ClientePreviewDTO {

    private Integer clienteId;
    private String nombre;
    private String telefono;

    /** El cliente aceptó recibir notificaciones */
    private boolean tieneConsentimiento;

    /** El cliente tiene al menos un teléfono registrado */
    private boolean tieneTelefono;

    /**
     * Recibió una promoción (estado=enviado) en los últimos 30 días.
     * Si es true, se muestra desmarcado por defecto con una advertencia visual.
     */
    private boolean contactadoRecientemente;

    /**
     * Días transcurridos desde la última promoción enviada.
     * Null si nunca ha recibido ninguna.
     */
    private Integer diasDesdeUltimaPromocion;

    public ClientePreviewDTO(Integer clienteId, String nombre, String telefono,
                              boolean tieneConsentimiento, boolean tieneTelefono,
                              boolean contactadoRecientemente, Integer diasDesdeUltimaPromocion) {
        this.clienteId = clienteId;
        this.nombre = nombre;
        this.telefono = telefono;
        this.tieneConsentimiento = tieneConsentimiento;
        this.tieneTelefono = tieneTelefono;
        this.contactadoRecientemente = contactadoRecientemente;
        this.diasDesdeUltimaPromocion = diasDesdeUltimaPromocion;
    }
}
