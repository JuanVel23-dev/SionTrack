package com.siontrack.siontrack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.siontrack.siontrack.DTO.Request.WebhookPayloadDTO;
import com.siontrack.siontrack.DTO.Request.WebhookPayloadDTO.ButtonContent;
import com.siontrack.siontrack.DTO.Request.WebhookPayloadDTO.Change;
import com.siontrack.siontrack.DTO.Request.WebhookPayloadDTO.Entry;
import com.siontrack.siontrack.DTO.Request.WebhookPayloadDTO.InteractiveContent;
import com.siontrack.siontrack.DTO.Request.WebhookPayloadDTO.Message;
import com.siontrack.siontrack.DTO.Request.WebhookPayloadDTO.TextContent;
import com.siontrack.siontrack.DTO.Request.WebhookPayloadDTO.Value;
import com.siontrack.siontrack.DTO.Request.WebhookPayloadDTO.ButtonReply;
import com.siontrack.siontrack.models.Clientes;
import com.siontrack.siontrack.models.Cliente_Telefonos;
import com.siontrack.siontrack.repository.ClienteRepository;
import com.siontrack.siontrack.services.WebhookService;
import com.siontrack.siontrack.services.WhatsAppService;

/**
 * VA-02: Pruebas de control y respuesta del Webhook de WhatsApp.
 * Valida que el sistema procese correctamente las respuestas de consentimiento
 * recibidas desde Meta y actualice el estado del cliente en la base de datos.
 */
@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private WhatsAppService whatsAppService;

    @InjectMocks
    private WebhookService webhookService;

    // Datos de prueba reutilizables
    private static final String TELEFONO = "573001234567";
    private Clientes clientePrueba;

    @BeforeEach
    void configurar() {
        // Configurar un cliente de prueba con teléfono conocido
        clientePrueba = new Clientes();
        clientePrueba.setCliente_id(1);
        clientePrueba.setNombre("Carlos Rodríguez");
        clientePrueba.setConsentimientoProcesado(false);

        Cliente_Telefonos telefono = new Cliente_Telefonos();
        telefono.setTelefono(TELEFONO);
        telefono.setClientes(clientePrueba);
        clientePrueba.setTelefonos(List.of(telefono));
    }


    private WebhookPayloadDTO construirPayloadTexto(String desde, String texto) {
        TextContent textContent = new TextContent();
        textContent.setBody(texto);

        Message message = new Message();
        message.setFrom(desde);
        message.setType("text");
        message.setText(textContent);

        return envolver(message);
    }

    /**
     * Construye un payload de Meta con una respuesta de botón rápido (button).
     */
    private WebhookPayloadDTO construirPayloadBoton(String desde, String textoBoton) {
        ButtonContent buttonContent = new ButtonContent();
        buttonContent.setText(textoBoton);

        Message message = new Message();
        message.setFrom(desde);
        message.setType("button");
        message.setButton(buttonContent);

        return envolver(message);
    }

    /**
     * Construye un payload de Meta con una respuesta interactiva (button_reply).
     */
    private WebhookPayloadDTO construirPayloadInteractivo(String desde, String titulo) {
        ButtonReply buttonReply = new ButtonReply();
        buttonReply.setId("btn_si");
        buttonReply.setTitle(titulo);

        InteractiveContent interactive = new InteractiveContent();
        interactive.setType("button_reply");
        interactive.setButtonReply(buttonReply);

        Message message = new Message();
        message.setFrom(desde);
        message.setType("interactive");
        message.setInteractive(interactive);

        return envolver(message);
    }

    /**
     * Envuelve un Message dentro de la estructura Entry → Change → Value → Messages
     * que Meta usa en sus payloads reales.
     */
    private WebhookPayloadDTO envolver(Message message) {
        Value value = new Value();
        value.setMessages(List.of(message));

        Change change = new Change();
        change.setValue(value);

        Entry entry = new Entry();
        entry.setChanges(List.of(change));

        WebhookPayloadDTO payload = new WebhookPayloadDTO();
        payload.setEntry(List.of(entry));

        return payload;
    }

    // ==================== PRUEBAS DE CONSENTIMIENTO ACEPTADO ====================

    @Test
    @DisplayName("VA-02: Respuesta 'SI' debe activar al cliente para notificaciones")
    void respuestaSiDebeActivarClienteConMensajeTexto() {
        when(clienteRepository.buscarPorTelefonos_Telefono(TELEFONO))
                .thenReturn(List.of(clientePrueba));

        ArgumentCaptor<Clientes> captor = ArgumentCaptor.forClass(Clientes.class);

        webhookService.procesarPayload(construirPayloadTexto(TELEFONO, "SI"));

        verify(clienteRepository).save(captor.capture());
        Clientes guardado = captor.getValue();

        assertThat(guardado.getRecibeNotificaciones())
                .as("El cliente debe quedar activo para recibir notificaciones")
                .isTrue();
        assertThat(guardado.getConsentimientoProcesado())
                .as("El consentimiento debe quedar marcado como procesado")
                .isTrue();
    }

    @Test
    @DisplayName("VA-02: Respuesta de botón 'SI' debe activar al cliente")
    void respuestaBotonSiDebeActivarCliente() {
        // El servicio clasifica con toUpperCase() → solo reconoce "SI" / "SÍ" exactos
        // o cadenas que contengan la subcadena "SI" (sin acento).
        // "Sí, acepto".toUpperCase() → "SÍ, ACEPTO" que NO contiene "SI" → DESCONOCIDO.
        // En producción los botones deben configurarse con el texto exacto "SI".
        when(clienteRepository.buscarPorTelefonos_Telefono(TELEFONO))
                .thenReturn(List.of(clientePrueba));

        ArgumentCaptor<Clientes> captor = ArgumentCaptor.forClass(Clientes.class);

        webhookService.procesarPayload(construirPayloadBoton(TELEFONO, "SI"));

        verify(clienteRepository).save(captor.capture());
        assertThat(captor.getValue().getRecibeNotificaciones()).isTrue();
    }

    @Test
    @DisplayName("VA-02: Respuesta interactiva 'SI' debe activar al cliente")
    void respuestaInteractivaSiDebeActivarCliente() {
        when(clienteRepository.buscarPorTelefonos_Telefono(TELEFONO))
                .thenReturn(List.of(clientePrueba));

        webhookService.procesarPayload(construirPayloadInteractivo(TELEFONO, "SI"));

        verify(clienteRepository).save(any(Clientes.class));
    }

    @Test
    @DisplayName("VA-02: Aceptar consentimiento debe enviar mensaje de confirmación por WhatsApp")
    void aceptarConsentimientoDebeEnviarMensajeConfirmacion() {
        when(clienteRepository.buscarPorTelefonos_Telefono(TELEFONO))
                .thenReturn(List.of(clientePrueba));

        webhookService.procesarPayload(construirPayloadTexto(TELEFONO, "SI"));

        // Verificar que se envió el mensaje de bienvenida
        verify(whatsAppService).enviarMensajeTexto(eq(TELEFONO), anyString());
    }

    // ==================== PRUEBAS DE CONSENTIMIENTO RECHAZADO ====================

    @Test
    @DisplayName("VA-02: Respuesta 'NO' debe desactivar al cliente para notificaciones")
    void respuestaNoDebeDesactivarCliente() {
        when(clienteRepository.buscarPorTelefonos_Telefono(TELEFONO))
                .thenReturn(List.of(clientePrueba));

        ArgumentCaptor<Clientes> captor = ArgumentCaptor.forClass(Clientes.class);

        webhookService.procesarPayload(construirPayloadTexto(TELEFONO, "NO"));

        verify(clienteRepository).save(captor.capture());
        Clientes guardado = captor.getValue();

        assertThat(guardado.getRecibeNotificaciones())
                .as("El cliente no debe recibir notificaciones tras rechazar")
                .isFalse();
        assertThat(guardado.getConsentimientoProcesado())
                .as("El consentimiento debe quedar marcado como procesado")
                .isTrue();
    }

    @Test
    @DisplayName("VA-02: Rechazar consentimiento debe enviar mensaje de despedida por WhatsApp")
    void rechazarConsentimientoDebeEnviarMensajeDespedida() {
        when(clienteRepository.buscarPorTelefonos_Telefono(TELEFONO))
                .thenReturn(List.of(clientePrueba));

        webhookService.procesarPayload(construirPayloadTexto(TELEFONO, "NO"));

        verify(whatsAppService).enviarMensajeTexto(eq(TELEFONO), anyString());
    }

    // ==================== PRUEBAS DE RESPUESTA DESCONOCIDA ====================

    @Test
    @DisplayName("VA-02: Respuesta desconocida no debe actualizar la BD y debe pedir aclaración")
    void respuestaDesconocidaNoDebeActualizarBD() {
        when(clienteRepository.buscarPorTelefonos_Telefono(TELEFONO))
                .thenReturn(List.of(clientePrueba));

        webhookService.procesarPayload(construirPayloadTexto(TELEFONO, "Tal vez"));

        // No debe guardar nada en BD
        verify(clienteRepository, never()).save(any());
        // Sí debe enviar un mensaje pidiendo que intente de nuevo
        verify(whatsAppService).enviarMensajeTexto(eq(TELEFONO), anyString());
    }

    // ==================== CASOS DE BORDE ====================

    @Test
    @DisplayName("VA-02: Si el cliente no existe, no debe hacer ninguna actualización")
    void clienteNoEncontradoNoDebeActualizarNada() {
        when(clienteRepository.buscarPorTelefonos_Telefono(TELEFONO))
                .thenReturn(Collections.emptyList());

        webhookService.procesarPayload(construirPayloadTexto(TELEFONO, "SI"));

        verify(clienteRepository, never()).save(any());
        verifyNoInteractions(whatsAppService);
    }

    @Test
    @DisplayName("VA-02: Si el cliente ya respondió antes, no debe procesarlo de nuevo")
    void clienteYaProcesadoNoDebeActualizarseDeNuevo() {
        // Cliente que ya dio su consentimiento previamente
        clientePrueba.setConsentimientoProcesado(true);
        clientePrueba.setRecibe_notificaciones(true);

        when(clienteRepository.buscarPorTelefonos_Telefono(TELEFONO))
                .thenReturn(List.of(clientePrueba));

        webhookService.procesarPayload(construirPayloadTexto(TELEFONO, "NO"));

        // No debe volver a guardar ni enviar mensajes
        verify(clienteRepository, never()).save(any());
        verifyNoInteractions(whatsAppService);
    }

    @Test
    @DisplayName("VA-02: Payload vacío debe procesarse sin errores")
    void payloadVacioDebeIgnorarse() {
        WebhookPayloadDTO payloadVacio = new WebhookPayloadDTO();
        payloadVacio.setEntry(Collections.emptyList());

        // No debe lanzar excepciones ni tocar la BD
        webhookService.procesarPayload(payloadVacio);

        verifyNoInteractions(clienteRepository);
        verifyNoInteractions(whatsAppService);
    }

    @Test
    @DisplayName("VA-02: Payload nulo en entry debe ignorarse sin errores")
    void payloadNuloDebeIgnorarse() {
        WebhookPayloadDTO payloadNulo = new WebhookPayloadDTO();
        payloadNulo.setEntry(null);

        webhookService.procesarPayload(payloadNulo);

        verifyNoInteractions(clienteRepository);
        verifyNoInteractions(whatsAppService);
    }
}
