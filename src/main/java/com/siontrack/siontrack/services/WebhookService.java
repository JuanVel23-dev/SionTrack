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

/**
 * Procesa los eventos entrantes del webhook de la API de WhatsApp (Meta).
 *
 * <p>Flujo principal:
 * <ol>
 *   <li>{@link #procesarPayload} itera sobre las entradas del payload y delega
 *       cada mensaje a {@link #procesarMensaje}.</li>
 *   <li>{@link #procesarMensaje} extrae el texto de la respuesta según el tipo
 *       de mensaje (texto libre, botón o interactivo) y llama a
 *       {@link #procesarConsentimiento}.</li>
 *   <li>{@link #procesarConsentimiento} busca al cliente por teléfono, clasifica
 *       su respuesta y actualiza el campo {@code recibeNotificaciones}.</li>
 * </ol>
 *
 * <p>Solo se procesa el consentimiento una vez por cliente: si el campo
 * {@code consentimientoProcesado} ya está en {@code true}, se le informa
 * de un número de contacto de soporte.
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final ClienteRepository clienteRepository;
    private final WhatsAppService whatsAppService;

    public WebhookService(ClienteRepository clienteRepository, WhatsAppService whatsAppService) {
        this.clienteRepository = clienteRepository;
        this.whatsAppService = whatsAppService;
    }

    /**
     * Punto de entrada del webhook. Itera sobre todas las entradas del payload y
     * procesa cada mensaje individual.
     *
     * @param payload estructura del evento recibido desde la API de Meta
     */
    public void procesarPayload(WebhookPayloadDTO payload) {
        if (payload.getEntry() == null || payload.getEntry().isEmpty()) {
            log.info("Payload vacío recibido");
            return;
        }

        payload.getEntry().stream()
                .filter(entry -> entry.getChanges() != null)
                .flatMap(entry -> entry.getChanges().stream())
                .filter(change -> change.getValue() != null && change.getValue().getMessages() != null)
                .flatMap(change -> change.getValue().getMessages().stream())
                .forEach(this::procesarMensaje);
    }

    /**
     * Extrae el texto de la respuesta del cliente y lo envía a {@link #procesarConsentimiento}.
     * Se ignoran los mensajes cuya respuesta no se puede extraer (por ejemplo, imágenes o audios).
     */
    private void procesarMensaje(Message message) {
        String telefono = message.getFrom();
        String respuesta = extraerRespuesta(message);

        log.info("Tipo: {}, Teléfono: {}, Respuesta: [{}]", message.getType(), telefono, respuesta);

        if (!respuesta.isEmpty()) {
            procesarConsentimiento(telefono, respuesta);
        }
    }

    /**
     * Extrae el cuerpo de texto de un mensaje según su tipo.
     * Soporta mensajes de texto libre, respuestas de botón y mensajes interactivos
     * (button_reply y list_reply). Devuelve cadena vacía si el tipo no es soportado.
     *
     * @param message mensaje entrante del webhook
     * @return texto de la respuesta, o cadena vacía si no se pudo extraer
     */
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

    /**
     * Busca al cliente por su número de teléfono y aplica la decisión de consentimiento
     * según la respuesta recibida.
     *
     * <p>Si el cliente ya procesó su consentimiento previamente, se le informa
     * un número de soporte sin modificar su registro.
     *
     * @param telefonoMeta número de teléfono en formato E.164 recibido desde Meta
     * @param mensaje      texto de la respuesta del cliente
     */
    @Transactional
    protected void procesarConsentimiento(String telefonoMeta, String mensaje) {
        String respuestaNormalizada = mensaje.trim().toUpperCase();
        String telefonoLocal = telefonoMeta;

        log.info("Buscando cliente: {}", telefonoLocal);

        List<Clientes> clienteOpt = clienteRepository.buscarPorTelefonos_Telefono(telefonoLocal);

        if (clienteOpt.isEmpty()) {
            log.warn("Cliente no encontrado: {}", telefonoLocal);
            return;
        }

        Clientes cliente = clienteOpt.get(0);

        if (Boolean.TRUE.equals(cliente.getConsentimientoProcesado())) {
            log.info("{} ya respondió anteriormente", cliente.getNombre());
            whatsAppService.enviarMensajeTexto(
                    telefonoMeta, "Si tienes alguna duda, puedes contactar con un asesor a través de este número 👉 3192486297");
            return;
        }

        TipoConsentimiento tipo = clasificarRespuesta(respuestaNormalizada);
        aplicarConsentimiento(cliente, tipo, telefonoMeta);
    }

    /**
     * Clasifica la respuesta del cliente en {@link TipoConsentimiento#ACEPTO},
     * {@link TipoConsentimiento#RECHAZO} o {@link TipoConsentimiento#DESCONOCIDO}.
     *
     * @param respuesta texto normalizado a mayúsculas
     * @return clasificación del consentimiento
     */
    private TipoConsentimiento clasificarRespuesta(String respuesta) {
        if (respuesta.equals("SI") || respuesta.equals("SÍ") || respuesta.contains("SI")) {
            return TipoConsentimiento.ACEPTO;
        }

        if (respuesta.equals("NO") || respuesta.contains("NO")) {
            return TipoConsentimiento.RECHAZO;
        }

        return TipoConsentimiento.DESCONOCIDO;
    }

    /**
     * Persiste la decisión del cliente y envía el mensaje de confirmación correspondiente.
     *
     * @param cliente        entidad del cliente a actualizar
     * @param tipo           tipo de consentimiento clasificado
     * @param telefonoCliente número de teléfono al que se envía la confirmación
     */
    private void aplicarConsentimiento(Clientes cliente, TipoConsentimiento tipo, String telefonoCliente) {
        switch (tipo) {
            case ACEPTO -> {
                cliente.setRecibe_notificaciones(true);
                cliente.setConsentimientoProcesado(true);
                clienteRepository.save(cliente);
                log.info("{} ACEPTÓ notificaciones", cliente.getNombre());
                whatsAppService.enviarMensajeTexto(
                        telefonoCliente,
                        "🎉 ¡Gracias por unirte!, Recibirás notificaciones sobre tus mantenimientos y beneficios exclusivos. Si tienes alguna duda, puedes contactar con un asesor a través de este número 👉 3192486297 ");
            }
            case RECHAZO -> {
                cliente.setRecibe_notificaciones(false);
                cliente.setConsentimientoProcesado(true);
                clienteRepository.save(cliente);
                log.info("{} RECHAZÓ notificaciones", cliente.getNombre());
                whatsAppService.enviarMensajeTexto(
                        telefonoCliente,
                        "Entendido No recibirás notificaciones. Si cambias de opinión, contáctanos a través de este número 👉 3192486297:).");
            }
            case DESCONOCIDO -> {
                log.warn("Respuesta no reconocida de {}", cliente.getNombre());
                whatsAppService.enviarMensajeTexto(
                        telefonoCliente,
                        "Has envíado una respuesta que no esta entre las opciones disponibles. Por favor intenta nuevamente. ");
            }
        }
    }
}
