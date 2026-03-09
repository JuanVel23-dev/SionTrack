package com.siontrack.siontrack.services;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.siontrack.siontrack.DTO.Request.WebhookPayloadDTO;
import com.siontrack.siontrack.DTO.Request.WebhookPayloadDTO.Message;
import com.siontrack.siontrack.models.Clientes;
import com.siontrack.siontrack.models.enums.TipoConsentimiento;
import com.siontrack.siontrack.repository.ClienteRepository;

import jakarta.transaction.Transactional;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final ClienteRepository clienteRepository;
    private final WhatsAppService whatsAppService;

    public WebhookService(ClienteRepository clienteRepository, WhatsAppService whatsAppService) {
        this.clienteRepository = clienteRepository;
        this.whatsAppService = whatsAppService;
    }

    public void procesarPayload(WebhookPayloadDTO payload) {
        if (payload.getEntry() == null || payload.getEntry().isEmpty()) {
            log.debug("Payload vacío recibido");
            return;
        }

        payload.getEntry().stream()
                .filter(entry -> entry.getChanges() != null)
                .flatMap(entry -> entry.getChanges().stream())
                .filter(change -> change.getValue() != null && change.getValue().getMessages() != null)
                .flatMap(change -> change.getValue().getMessages().stream())
                .forEach(this::procesarMensaje);
    }

    private void procesarMensaje(Message message) {
        String telefono = message.getFrom();
        String respuesta = extraerRespuesta(message);

        log.info("📨 Tipo: {}, Teléfono: {}, Respuesta: [{}]", message.getType(), telefono, respuesta);

        if (!respuesta.isEmpty()) {
            procesarConsentimiento(telefono, respuesta);
        }
    }

    private String extraerRespuesta(Message message) {
        String tipo = message.getType();

        if ("text".equals(tipo) && message.getText() != null) {
            return Optional.ofNullable(message.getText().getBody()).orElse("");
        }

        if ("button".equals(tipo) && message.getButton() != null) {
            return Optional.ofNullable(message.getButton().getText()).orElse("");
        }

        if ("interactive".equals(tipo) && message.getInteractive() != null) {
            var interactive = message.getInteractive();

            if ("button_reply".equals(interactive.getType()) && interactive.getButtonReply() != null) {
                return Optional.ofNullable(interactive.getButtonReply().getTitle()).orElse("");
            }

            if ("list_reply".equals(interactive.getType()) && interactive.getListReply() != null) {
                return Optional.ofNullable(interactive.getListReply().getTitle()).orElse("");
            }
        }

        return "";
    }

    @Transactional
    protected void procesarConsentimiento(String telefonoMeta, String mensaje) {
        String respuestaNormalizada = mensaje.trim().toUpperCase();
        String telefonoLocal = telefonoMeta;

        log.info("🔍 Buscando cliente: {}", telefonoLocal);

        List<Clientes> clienteOpt = clienteRepository.buscarPorTelefonos_Telefono(telefonoLocal);

        if (clienteOpt.isEmpty()) {
            log.warn("❌ Cliente no encontrado: {}", telefonoLocal);
            return;
        }

        Clientes cliente = clienteOpt.get(0);

        if (Boolean.TRUE.equals(cliente.getConsentimientoProcesado())) {
            log.info("⚠️ {} ya respondió anteriormente", cliente.getNombre());
            return;
        }

        TipoConsentimiento tipo = clasificarRespuesta(respuestaNormalizada);
        aplicarConsentimiento(cliente, tipo,telefonoMeta);
    }

    private TipoConsentimiento clasificarRespuesta(String respuesta) {
        if (respuesta.equals("SI") || respuesta.equals("SÍ") || respuesta.contains("SI")) {
            return TipoConsentimiento.ACEPTO;
        }

        if (respuesta.equals("NO") || respuesta.contains("NO")) {
            return TipoConsentimiento.RECHAZO;
        }

        return TipoConsentimiento.DESCONOCIDO;
    }

    private void aplicarConsentimiento(Clientes cliente, TipoConsentimiento tipo, String telefonoCliente) {
        switch (tipo) {
            case ACEPTO -> {
                cliente.setRecibe_notificaciones(true);
                cliente.setConsentimientoProcesado(true);
                clienteRepository.save(cliente);
                log.info("✅ {} ACEPTÓ notificaciones", cliente.getNombre());
                whatsAppService.enviarMensajeTexto(
                        telefonoCliente,
                        "🎉 ¡Gracias por unirte!, Recibirás notificaciones sobre tus mantenimientos y beneficios exclusivos.");
            }
            case RECHAZO -> {
                cliente.setRecibe_notificaciones(false);
                cliente.setConsentimientoProcesado(true);
                clienteRepository.save(cliente);
                log.info("ℹ️ {} RECHAZÓ notificaciones", cliente.getNombre());

                whatsAppService.enviarMensajeTexto(
                        telefonoCliente,
                        "Entendido No recibirás notificaciones. Si cambias de opinión, contáctanos.");
            }
            case DESCONOCIDO -> {
                log.warn("⚠️ Respuesta no reconocida de {}", cliente.getNombre());
                whatsAppService.enviarMensajeTexto(
                        telefonoCliente,
                        "Has envíado una respuesta que no esta entre las opciones disponibles. Por favor intenta nuevamente. ");
            }
        }
    }
}
