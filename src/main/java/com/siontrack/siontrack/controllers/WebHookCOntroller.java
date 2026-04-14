package com.siontrack.siontrack.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.siontrack.siontrack.DTO.Request.WebhookPayloadDTO;
import com.siontrack.siontrack.configuration.WhatsAppConfig;
import com.siontrack.siontrack.services.WebhookService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador del webhook de la API de WhatsApp Business (Meta).
 *
 * <p>Expone dos endpoints en {@code /api/webhook}:
 * <ul>
 *   <li>{@code GET} — verificación inicial del webhook requerida por Meta al configurarlo.</li>
 *   <li>{@code POST} — recepción de eventos (mensajes entrantes de clientes).</li>
 * </ul>
 *
 * <p>Ambos endpoints están excluidos de la protección CSRF porque Meta no puede
 * incluir el token, y son de acceso público para que Meta pueda alcanzarlos.
 */
@Tag(name = "Webhook", description = "Endpoints para la integración con la API de WhatsApp Business (Meta)")
@RestController
@RequestMapping("/api/webhook")
public class WebHookController {

    private static final Logger log = LoggerFactory.getLogger(WebHookController.class);

    private final WhatsAppConfig config;
    private final WebhookService webhookService;

    public WebHookController(WhatsAppConfig config, WebhookService webhookService) {
        this.config = config;
        this.webhookService = webhookService;
    }

    /**
     * Verificación del webhook requerida por Meta al registrar la URL.
     * Devuelve el valor de {@code hub.challenge} si el token coincide con el configurado.
     *
     * @param mode      debe ser {@code "subscribe"}
     * @param token     token de verificación configurado en Meta
     * @param challenge valor que Meta espera recibir de vuelta
     * @return el challenge si la verificación es exitosa, {@code 403} si falla
     */
    @Operation(
        summary = "Verificar webhook (Meta)",
        description = "Endpoint de verificación requerido por Meta al registrar la URL del webhook. "
                    + "Responde con el challenge solo si el token coincide con el configurado."
    )
    @GetMapping
    public ResponseEntity<String> verificarWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) && config.getWebhook().getTokenverificacion().equals(token)) {
            log.info("Webhook verificado");
            return ResponseEntity.ok(challenge);
        }

        log.info("Verificación fallida");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * Recibe los eventos del webhook de Meta (mensajes entrantes).
     * Siempre responde {@code 200 EVENT_RECEIVED} para que Meta no reintente el envío,
     * incluso si ocurre un error interno al procesar el payload.
     *
     * @param payload estructura del evento enviado por Meta
     * @return {@code "EVENT_RECEIVED"} siempre
     */
    @Operation(
        summary = "Recibir evento del webhook",
        description = "Procesa los mensajes entrantes enviados por Meta (respuestas de consentimiento, etc.). "
                    + "Siempre responde 200 para evitar reintentos de Meta."
    )
    @PostMapping
    public ResponseEntity<String> recibirMensaje(@RequestBody WebhookPayloadDTO payload) {
        try {
            webhookService.procesarPayload(payload);
        } catch (Exception e) {
            log.info("Error: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}
