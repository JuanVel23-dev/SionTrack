package com.siontrack.siontrack;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;

import com.siontrack.siontrack.services.WhatsAppService;

/**
 * VARE-01: Prueba de latencia del endpoint POST /api/webhook.
 *
 * Mide el tiempo de respuesta del webhook usando un Tomcat real embebido
 * y HTTP real sobre localhost. Esto incluye:
 *   - Deserialización del JSON
 *   - Filtros de Spring Security
 *   - Lógica del controller y el servicio
 *   - Consulta real a PostgreSQL
 *
 * Lo único mockeado es WhatsAppService para evitar llamadas reales a la API de Meta.
 *
 * Criterio de éxito (VA-RE-01): tiempo de respuesta < 500 ms.
 * (Meta reintenta el payload si no recibe respuesta en ese tiempo.)
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class LatenciaWebhookTest {

    /** Puerto real asignado por Spring al Tomcat embebido */
    @LocalServerPort
    private int puerto;

    /**
     * Cliente HTTP real — hace peticiones TCP reales a localhost.
     * A diferencia de MockMvc, este sí pasa por el stack de red completo.
     */
    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Mockeamos WhatsAppService para que el test no llame a la API de Meta.
     * El resto del stack (BD, repositorios, servicios) es 100 % real.
     */
    @MockBean
    private WhatsAppService whatsAppService;

    // ==================== PAYLOAD DE PRUEBA ====================

    /**
     * Construye un payload con la estructura exacta que Meta envía al webhook.
     * Se usa un teléfono que no existe en BD para que el servicio retorne
     * rápidamente sin modificar datos reales.
     */
    private String payloadDePrueba() {
        return """
                {
                  "entry": [{
                    "changes": [{
                      "value": {
                        "messages": [{
                          "from": "5700000000000",
                          "type": "text",
                          "text": { "body": "SI" }
                        }]
                      }
                    }]
                  }]
                }
                """;
    }

    private HttpEntity<String> peticionJson() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(payloadDePrueba(), headers);
    }

    private String urlWebhook() {
        return "http://localhost:" + puerto + "/api/webhook";
    }

    // ==================== PRUEBAS DE LATENCIA ====================

    @Test
    @DisplayName("VARE-01: El webhook debe responder con 200 OK y cuerpo EVENT_RECEIVED")
    void webhookDebeResponderConStatusCorrecto() {
        ResponseEntity<String> respuesta = restTemplate.postForEntity(
                urlWebhook(), peticionJson(), String.class);

        assertThat(respuesta.getStatusCode())
                .as("El webhook siempre debe retornar 200 OK (incluso con errores internos)")
                .isEqualTo(HttpStatus.OK);

        assertThat(respuesta.getBody())
                .as("El cuerpo de la respuesta debe ser EVENT_RECEIVED")
                .isEqualTo("EVENT_RECEIVED");
    }

    @Test
    @DisplayName("VARE-01: El webhook debe responder en menos de 500 ms (petición única)")
    void webhookDebeResponderEnMenos500ms() {
        // Calentar la JVM con una petición previa para evitar medir tiempo de compilación JIT
        restTemplate.postForEntity(urlWebhook(), peticionJson(), String.class);

        StopWatch cronometro = new StopWatch("VARE-01");
        cronometro.start("POST /api/webhook");

        ResponseEntity<String> respuesta = restTemplate.postForEntity(
                urlWebhook(), peticionJson(), String.class);

        cronometro.stop();

        long tiempoMs = cronometro.getTotalTimeMillis();
        System.out.printf("%n[VARE-01] Latencia medida: %d ms (limite: 500 ms)%n", tiempoMs);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tiempoMs)
                .as("La respuesta debe llegar en menos de 500 ms para evitar reintentos de Meta")
                .isLessThan(500L);
    }

    @RepeatedTest(value = 5, name = "VARE-01 — iteración {currentRepetition} de {totalRepetitions}")
    @DisplayName("VARE-01: El webhook debe mantenerse por debajo de 500 ms en múltiples peticiones")
    void webhookDebeMantenersePorDebajoDelLimiteEnCadaPeticion(RepetitionInfo info) {
        // La primera iteración sirve de calentamiento; se registra pero no se evalúa con límite estricto
        boolean esCalentamiento = info.getCurrentRepetition() == 1;

        StopWatch cronometro = new StopWatch();
        cronometro.start();

        ResponseEntity<String> respuesta = restTemplate.postForEntity(
                urlWebhook(), peticionJson(), String.class);

        cronometro.stop();

        long tiempoMs = cronometro.getTotalTimeMillis();
        System.out.printf("[VARE-01] Iteracion %d: %d ms%n",
                info.getCurrentRepetition(), tiempoMs);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);

        if (!esCalentamiento) {
            assertThat(tiempoMs)
                    .as("Iteracion %d: %d ms supera el límite de 500 ms",
                            info.getCurrentRepetition(), tiempoMs)
                    .isLessThan(500L);
        }
    }
}
