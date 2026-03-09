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

    
    public ResultadoEnvioMensaje enviarSolicitudConsentimiento(String telefono, String nombreCliente) {
        log.info("📤 Enviando solicitud de consentimiento a: {} ({})", nombreCliente, telefono);

        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("nombre_cliente", validarTexto(nombreCliente, "Cliente"));

        return enviarPlantilla(
                telefono,
                config.getPlantillas().getConsentimiento(),
                config.getPlantillas().getIdioma(),
                parametros);
    }

    /**
     * Envía la plantilla de recordatorio de servicio
     */
    public ResultadoEnvioMensaje enviarRecordatorioServicio(String telefono, String nombreCliente,
            String placaVehiculo, String kilometraje) {
        log.info("📤 Enviando recordatorio de servicio a: {}", nombreCliente);

        // Usar LinkedHashMap para mantener el orden
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


    public ResultadoEnvioMensaje enviarMensajePromo (String telefono, String nombreCliente, String marcaVehiculo, String promocion, String precioOferta, String rangoFechas ){

        log.info("Enviando promocion a todos los vehiculos de marca {}", marcaVehiculo);

        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("nombre_cliente", validarTexto(nombreCliente, "estimado cliente"));
        parametros.put("marca_vehiculo", validarTexto(marcaVehiculo, "vehiculo"));
        parametros.put("promocion", validarTexto(promocion, "Revisión gratis"));
        parametros.put("precio_oferta", validarTexto(precioOferta, "$0"));
        parametros.put("rango_fechas", validarTexto(rangoFechas, "01/01/2026 al 01/01/2027"));

        return enviarPlantilla(telefono, config.getPlantillas().getPromocion(), config.getPlantillas().getIdioma(), parametros);
        
    
    }
    /**
     * Envía un mensaje de texto libre (solo dentro de ventana de 24h)
     */
    public ResultadoEnvioMensaje enviarMensajeTexto(String telefono, String mensaje) {
        log.info("📤 Enviando mensaje de texto a: {}", telefono);

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", telefono,
                "type", "text",
                "text", Map.of("body", mensaje));

        return ejecutarEnvio(body, telefono);
    }

    

    public ResultadoEnvioMensaje enviarPlantilla(String telefono, String nombrePlantilla,
            String codigoIdioma, Map<String, String> parametros) {
        log.info("📤 Enviando plantilla '{}' a: {}", nombrePlantilla, telefono);

        Map<String, Object> template = new HashMap<>();
        template.put("name", nombrePlantilla);
        template.put("language", Map.of("code", codigoIdioma));

        // Construir componentes solo si hay parámetros
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
     * Ejecuta el envío HTTP a la API de Meta
     */
    private ResultadoEnvioMensaje ejecutarEnvio(Map<String, Object> body, String telefonoOriginal) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getToken_acceso());

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            log.info("📤 JSON: {}", jsonBody);

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    config.getApiUrl(),
                    request,
                    String.class);

            log.info("✅ Mensaje enviado exitosamente");
            log.debug("✅ Respuesta de Meta: {}", response.getBody());
            return ResultadoEnvioMensaje.ENVIADO;

        } catch (HttpClientErrorException e) {
            return manejarErrorMeta(e, telefonoOriginal);
        } catch (Exception e) {
            log.error("❌ Error enviando mensaje: {}", e.getMessage(), e);
            return ResultadoEnvioMensaje.ERROR_DESCONOCIDO;
        }
    }

    /**
     * Maneja los errores específicos de la API de Meta
     */
    private ResultadoEnvioMensaje manejarErrorMeta(HttpClientErrorException e, String telefono) {
        try {
            String responseBody = e.getResponseBodyAsString();
            log.info("ERROR COMPLETO DE META: {}" + responseBody);
            JsonNode error = objectMapper.readTree(responseBody).get("error");

            int codigo = error.get("code").asInt();
            String mensaje = error.get("message").asText();

            log.warn("⚠️ Error de Meta para {}: Código {}, Mensaje: {}", telefono, codigo, mensaje);

            return switch (codigo) {
                case 131030 -> {
                    log.warn("📵 El número {} no tiene WhatsApp", telefono);
                    yield ResultadoEnvioMensaje.SIN_WHATSAPP;
                }
                case 131047 -> {
                    log.warn("🚫 Número inválido: {}", telefono);
                    yield ResultadoEnvioMensaje.NUMERO_INVALIDO;
                }
                case 131026 -> {
                    log.warn("📭 No se pudo entregar mensaje a: {}", telefono);
                    yield ResultadoEnvioMensaje.NO_ENTREGADO;
                }
                case 132000 -> {
                    log.error("❌ Error en parámetros de plantilla");
                    yield ResultadoEnvioMensaje.ERROR_PLANTILLA;
                }
                case 132001 -> {
                    log.error("❌ Plantilla no encontrada");
                    yield ResultadoEnvioMensaje.ERROR_PLANTILLA;
                }
                default -> {
                    log.error("❌ Error no manejado de Meta: {}", mensaje);
                    yield ResultadoEnvioMensaje.ERROR_DESCONOCIDO;
                }
            };

        } catch (Exception parseError) {
            log.error("❌ Error parseando respuesta de Meta: {}", parseError.getMessage());
            return ResultadoEnvioMensaje.ERROR_DESCONOCIDO;
        }
    }

    /**
     * Valida y retorna un texto, usando valor por defecto si es nulo o vacío
     */
    private String validarTexto(String texto, String valorDefecto) {
        return (texto == null || texto.trim().isEmpty()) ? valorDefecto : texto.trim();
    }

    /**
     * Formatea el teléfono agregando código de país si es necesario
     */
    
}
