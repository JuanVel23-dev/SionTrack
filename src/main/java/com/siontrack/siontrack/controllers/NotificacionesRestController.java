package com.siontrack.siontrack.controllers;

import com.siontrack.siontrack.services.NotificacionesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * API REST para la gestión del consentimiento de notificaciones.
 */
@Tag(name = "Notificaciones", description = "Consulta de clientes pendientes y envío masivo de consentimiento")
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionesRestController {

    private final NotificacionesService notificacionesService;

    public NotificacionesRestController(NotificacionesService notificacionesService) {
        this.notificacionesService = notificacionesService;
    }

    /**
     * Devuelve la página de clientes que aún no han respondido la solicitud de consentimiento.
     * Permite filtrar por nombre o cédula.
     */
    @Operation(
        summary = "Obtener clientes pendientes de consentimiento",
        description = "Devuelve los clientes que no han dado o rechazado el consentimiento para "
                    + "recibir notificaciones por WhatsApp. Soporta búsqueda por nombre o cédula."
    )
    @GetMapping("/pendientes")
    public ResponseEntity<Page<Map<String, Object>>> obtenerPendientes(
            @Parameter(description = "Número de página (base 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Término de búsqueda por nombre o cédula")
            @RequestParam(required = false) String search) {
        Page<Map<String, Object>> pagina = notificacionesService.obtenerClientesPendientesPaginado(
                PageRequest.of(page, 50), search);
        return ResponseEntity.ok(pagina);
    }

    /**
     * Envía la solicitud de consentimiento por WhatsApp a los clientes seleccionados.
     */
    @Operation(
        summary = "Envío masivo de consentimiento",
        description = "Envía la plantilla de solicitud de consentimiento a los clientes cuyos IDs "
                    + "se incluyen en el cuerpo de la petición. Devuelve un resumen con los contadores "
                    + "de enviados, fallidos y sin teléfono."
    )
    @PostMapping("/consentimiento-masivo")
    public ResponseEntity<Map<String, Object>> dispararConsentimientosMasivos(
            @RequestBody List<Integer> idsSeleccionados) {
        Map<String, Object> resultado = notificacionesService.enviarConsentimientoMasivo(idsSeleccionados);
        return ResponseEntity.ok(resultado);
    }
}
