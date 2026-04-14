package com.siontrack.siontrack.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siontrack.siontrack.configuration.WhatsAppConfig;
import com.siontrack.siontrack.models.enums.ResultadoEnvioMensaje;

/**
 * Cliente HTTP para la API de WhatsApp Business de Meta.
 *
 * <p>Soporta el envío de mensajes mediante plantillas aprobadas por Meta
 * (consentimiento, recordatorio de servicio y promociones) y mensajes de texto
 * libre (solo disponibles dentro de la ventana de 24 h de respuesta del usuario).
 *
 * <p>Todos los métodos de envío devuelven un {@link ResultadoEnvioMensaje} con el
 * resultado semántico de la operación, permitiendo al llamador registrar el estado
 * sin necesidad de capturar excepciones.
 */
@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private final WhatsAppConfig config;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public WhatsAppService(WhatsAppConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
    }

    /**
     * Envía la plantilla de solicitud de consentimiento a un número de teléfono.
     *
     * @param telefono      número de destino en formato E.164
     * @param nombreCliente nombre del cliente para personalizar la plantilla
     * @return resultado semántico del envío
     */
    public ResultadoEnvioMensaje enviarSolicitudConsentimiento(String telefono, String nombreCliente) {
        log.info("Enviando solicitud de consentimiento a: {} ({})", nombreCliente, telefono);

        // Se usa LinkedHashMap para preservar el orden de los parámetros de la plantilla
        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("nombre_cliente", validarTexto(nombreCliente, "Cliente"));

        return enviarPlantilla(
                telefono,
                config.getPlantillas().getConsentimiento(),
                config.getPlantillas().getIdioma(),
                parametros);
    }

    /**
     * Envía la plantilla de recordatorio de servicio a un número de teléfono.
     *
     * @param telefono      número de destino en formato E.164
     * @param nombreCliente nombre del cliente
     * @param placaVehiculo placa del vehículo asociado al servicio
     * @param kilometraje   kilometraje registrado en el último servicio
     * @return resultado semántico del envío
     */
    public ResultadoEnvioMensaje enviarRecordatorioServicio(String telefono, String nombreCliente,
            String placaVehiculo, String kilometraje) {
        log.info("Enviando recordatorio de servicio a: {}", nombreCliente);

        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("nombre_cliente", validarTexto(nombreCliente, "Cliente"));
        parametros.put("placa_vehiculo", validarTexto(placaVehiculo, "N/A"));
        parametros.put("kilometros_vehiculo", validarTexto(kilometraje, "N/A"));

        return enviarPlantilla(
                telefono,
                config.getPlantillas().getRecordatorio(),
                config.getPlantillas().getIdioma(),
                parametros);
    }

    /**
     * Envía la plantilla de promoción a un número de teléfono.
     *
     * @param telefono      número de destino en formato E.164
     * @param nombreCliente nombre del cliente
     * @param promocion     descripción de la promoción
     * @param precioOferta  precio de oferta del producto
     * @param rangoFechas   vigencia de la promoción
     * @return resultado semántico del envío
     */
    public ResultadoEnvioMensaje enviarMensajePromo(String telefono, String nombreCliente,
            String promocion, String precioOferta, String rangoFechas) {

        log.info("Enviando promoción a: {}", nombreCliente);

        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("nombre_cliente", validarTexto(nombreCliente, "estimado cliente"));
        parametros.put("promocion", validarTexto(promocion, "Revisión gratis"));
        parametros.put("precio_oferta", validarTexto(precioOferta, "$0"));
        parametros.put("rango_fechas", validarTexto(rangoFechas, "01/01/2026 al 01/01/2027"));

        return enviarPlantilla(telefono, config.getPlantillas().getPromocion(),
                config.getPlantillas().getIdioma(), parametros);
    }

    /**
     * Envía un mensaje de texto libre.
     * Solo disponible dentro de la ventana de 24 horas tras la última interacción del usuario.
     *
     * @param telefono número de destino en formato E.164
     * @param mensaje  cuerpo del mensaje de texto
     * @return resultado semántico del envío
     */
    public ResultadoEnvioMensaje enviarMensajeTexto(String telefono, String mensaje) {
        log.info("Enviando mensaje de texto a: {}", telefono);

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", telefono,
                "type", "text",
                "text", Map.of("body", mensaje));

        return ejecutarEnvio(body, telefono);
    }

    /**
     * Construye el payload de una plantilla y lo envía a la API de Meta.
     *
     * @param telefono        número de destino en formato E.164
     * @param nombrePlantilla nombre de la plantilla aprobada en Meta
     * @param codigoIdioma    código de idioma de la plantilla (p. ej. {@code "es"})
     * @param parametros      parámetros nombrados del cuerpo de la plantilla, en orden
     * @return resultado semántico del envío
     */
    public ResultadoEnvioMensaje enviarPlantilla(String telefono, String nombrePlantilla,
            String codigoIdioma, Map<String, String> parametros) {
        log.info("Enviando plantilla '{}' a: {}", nombrePlantilla, telefono);

        Map<String, Object> template = new HashMap<>();
        template.put("name", nombrePlantilla);
        template.put("language", Map.of("code", codigoIdioma));

        if (parametros != null && !parametros.isEmpty()) {
            template.put("components", List.of(construirComponenteBody(parametros)));
        }

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", telefono,
                "type", "template",
                "template", template);

        return ejecutarEnvio(body, telefono);
    }

    /**
     * Construye el componente {@code body} de una plantilla con sus parámetros nombrados.
     */
    private Map<String, Object> construirComponenteBody(Map<String, String> parametros) {
        List<Map<String, Object>> listaParametros = new ArrayList<>();

        for (Map.Entry<String, String> entry : parametros.entrySet()) {
            listaParametros.add(Map.of(
                    "type", "text",
                    "parameter_name", entry.getKey(),
                    "text", entry.getValue()));
        }

        return Map.of(
                "type", "body",
                "parameters", listaParametros);
    }

    /**
     * Realiza la llamada HTTP POST a la API de Meta y mapea el resultado a
     * {@link ResultadoEnvioMensaje}. Los errores HTTP del lado del cliente
     * se delegan a {@link #manejarErrorMeta}.
     *
     * @param body             payload JSON del mensaje
     * @param telefonoOriginal número de destino (usado en logs de error)
     * @return resultado semántico del envío
     */
    private ResultadoEnvioMensaje ejecutarEnvio(Map<String, Object> body, String telefonoOriginal) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getToken_acceso());

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            log.info("JSON enviado: {}", jsonBody);

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    config.getApiUrl(),
                    request,
                    String.class);

            log.info("Mensaje enviado exitosamente");
            log.debug("Respuesta de Meta: {}", response.getBody());
            return ResultadoEnvioMensaje.ENVIADO;

        } catch (HttpClientErrorException e) {
            return manejarErrorMeta(e, telefonoOriginal);
        } catch (Exception e) {
            log.error("Error enviando mensaje: {}", e.getMessage(), e);
            return ResultadoEnvioMensaje.ERROR_DESCONOCIDO;
        }
    }

    /**
     * Parsea la respuesta de error de Meta e infiere el {@link ResultadoEnvioMensaje}
     * según el código de error devuelto.
     *
     * <p>Códigos relevantes:
     * <ul>
     *   <li>{@code 131030} — el número no tiene WhatsApp</li>
     *   <li>{@code 131047} — número de teléfono inválido</li>
     *   <li>{@code 131026} — mensaje no entregado</li>
     *   <li>{@code 132000} / {@code 132001} — error en parámetros o plantilla no encontrada</li>
     * </ul>
     *
     * @param e               excepción HTTP con el cuerpo de error de Meta
     * @param telefono        número de destino (usado en logs)
     * @return resultado semántico del error
     */
    private ResultadoEnvioMensaje manejarErrorMeta(HttpClientErrorException e, String telefono) {
        try {
            String responseBody = e.getResponseBodyAsString();
            log.info("ERROR COMPLETO DE META: {}" + responseBody);
            JsonNode error = objectMapper.readTree(responseBody).get("error");

            int codigo = error.get("code").asInt();
            String mensaje = error.get("message").asText();

            log.warn("Error de Meta para {}: Código {}, Mensaje: {}", telefono, codigo, mensaje);

            return switch (codigo) {
                case 131030 -> {
                    log.warn("El número {} no tiene WhatsApp", telefono);
                    yield ResultadoEnvioMensaje.SIN_WHATSAPP;
                }
                case 131047 -> {
                    log.warn("Número inválido: {}", telefono);
                    yield ResultadoEnvioMensaje.NUMERO_INVALIDO;
                }
                case 131026 -> {
                    log.warn("No se pudo entregar mensaje a: {}", telefono);
                    yield ResultadoEnvioMensaje.NO_ENTREGADO;
                }
                case 132000 -> {
                    log.error("Error en parámetros de plantilla");
                    yield ResultadoEnvioMensaje.ERROR_PLANTILLA;
                }
                case 132001 -> {
                    log.error("Plantilla no encontrada");
                    yield ResultadoEnvioMensaje.ERROR_PLANTILLA;
                }
                default -> {
                    log.error("Error no manejado de Meta: {}", mensaje);
                    yield ResultadoEnvioMensaje.ERROR_DESCONOCIDO;
                }
            };

        } catch (Exception parseError) {
            log.error("Error parseando respuesta de Meta: {}", parseError.getMessage());
            return ResultadoEnvioMensaje.ERROR_DESCONOCIDO;
        }
    }

    /**
     * Devuelve el texto si no es nulo ni vacío; de lo contrario, devuelve el valor por defecto.
     */
    private String validarTexto(String texto, String valorDefecto) {
        return (texto == null || texto.trim().isEmpty()) ? valorDefecto : texto.trim();
    }
}
