package com.siontrack.siontrack.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.siontrack.siontrack.repository.ClienteRepository;

@RestController
@RequestMapping("/api/webhook")
public class WebHookCOntroller {

    private final String TOKEN_VERIFICACION= "token_provisional_siontrack";

    @Autowired
    private ClienteRepository clientesRepository; // Necesario para buscar al cliente

    @GetMapping 
    public ResponseEntity<String> verificarWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) && TOKEN_VERIFICACION.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }


    @PostMapping
    public ResponseEntity<String> recibirMensaje(@RequestBody Map<String, Object> payload) {
        try {
            System.out.println("📩 Payload recibido de Meta: " + payload);

            // 1. Navegar por la estructura anidada de Meta
            List<Map<String, Object>> entry = (List<Map<String, Object>>) payload.get("entry");
            if (entry == null || entry.isEmpty()) return ResponseEntity.ok("Evento vacío");

            List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get(0).get("changes");
            if (changes == null || changes.isEmpty()) return ResponseEntity.ok("Sin cambios");

            Map<String, Object> value = (Map<String, Object>) changes.get(0).get("value");
            
            // 2. Verificar si es un mensaje entrante (no un estado de entrega)
            if (value.containsKey("messages")) {
                List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");
                
                for (Map<String, Object> message : messages) {
                    // EXTRAER DATOS CLAVE
                    String telefono = (String) message.get("from"); // Ej: "573001234567"
                    String tipoMensaje = (String) message.get("type");

                    // Solo nos interesa si es TEXTO
                    if ("text".equals(tipoMensaje)) {
                        Map<String, Object> textObj = (Map<String, Object>) message.get("text");
                        String cuerpoMensaje = (String) textObj.get("body");
                        
                        System.out.println("📱 Mensaje de: " + telefono + " | Dice: " + cuerpoMensaje);

                        // 3. PROCESAR EL CONSENTIMIENTO
                        procesarConsentimiento(telefono, cuerpoMensaje);
                    }
                }
            }
            
            // Meta espera siempre un 200 OK, si no, te reenvía el mensaje infinitamente
            return ResponseEntity.ok("EVENT_RECEIVED");

        } catch (Exception e) {
            e.printStackTrace();
            // Aún si falla tu lógica, responde OK a Meta para que no se bloquee el webhook
            return ResponseEntity.ok("ERROR_HANDLED");
        }
    }

    private void procesarConsentimiento(String telefonoMeta, String mensaje) {
    String respuesta = mensaje.trim().toUpperCase();

    if (respuesta.equals("SI") || respuesta.equals("SÍ")) {
        
        // CORRECCIÓN: Usamos un operador ternario para definir el valor FINAL de una sola vez.
        // Si mide más de 10, cortamos. Si no, lo dejamos igual.
        // Al asignarse una sola vez, es "effectively final" y Java deja usarla en el lambda.
        final String telefonoFinal = (telefonoMeta.length() > 10) 
            ? telefonoMeta.substring(telefonoMeta.length() - 10) 
            : telefonoMeta;

        System.out.println("🔍 Buscando en BD: " + telefonoFinal);

        // Usamos 'telefonoFinal' que es segura para lambdas
        clientesRepository.findByTelefonos_Telefono(telefonoFinal).ifPresentOrElse(
            cliente -> {
                cliente.setRecibe_notificaciones(true);
                clientesRepository.save(cliente);
                System.out.println("✅ ¡Éxito! Cliente " + cliente.getNombre() + " actualizado.");
            },
            () -> {
                // Ahora sí permite usar la variable aquí adentro
                System.out.println("❌ No encontrado. Meta envió: " + telefonoMeta + " -> Buscamos: " + telefonoFinal);
            }
        );
    }
}
}
