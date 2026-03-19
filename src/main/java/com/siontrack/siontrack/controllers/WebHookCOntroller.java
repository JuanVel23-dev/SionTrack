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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siontrack.siontrack.DTO.Request.WebhookPayloadDTO;
import com.siontrack.siontrack.configuration.WhatsAppConfig;
import com.siontrack.siontrack.services.WebhookService;

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

    @GetMapping
    public ResponseEntity<String> verificarWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) && config.getWebhook().getTokenverificacion().equals(token)) {
            log.info("✅ Webhook verificado");
            return ResponseEntity.ok(challenge);
        }

        log.info("❌ Verificación fallida");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping
    public ResponseEntity<String> recibirMensaje(@RequestBody WebhookPayloadDTO payload) {
        try {
            webhookService.procesarPayload(payload);
        } catch (Exception e) {
            log.info("❌ Error: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    /*
     * @PostMapping
     * public ResponseEntity<String> recibirMensaje(@RequestBody String rawPayload)
     * {
     * log.info("📥 Payload RAW recibido: {}", rawPayload);
     * 
     * try {
     * ObjectMapper mapper = new ObjectMapper();
     * WebhookPayloadDTO payload = mapper.readValue(rawPayload,
     * WebhookPayloadDTO.class);
     * webhookService.procesarPayload(payload);
     * } catch (Exception e) {
     * log.error("❌ Error procesando: {}", e.getMessage(), e);
     * }
     * return ResponseEntity.ok("EVENT_RECEIVED");
     * }
     */
}
