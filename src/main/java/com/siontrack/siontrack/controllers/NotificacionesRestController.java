package com.siontrack.siontrack.controllers;

import com.siontrack.siontrack.services.NotificacionesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionesRestController {

    private final NotificacionesService notificacionesService;

    public NotificacionesRestController(NotificacionesService notificacionesService) {
        this.notificacionesService = notificacionesService;
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<Map<String, Object>>> obtenerPendientes() {
        return ResponseEntity.ok(notificacionesService.obtenerClientesPendientesConsentimiento());
    }

    @PostMapping("/consentimiento-masivo")
    public ResponseEntity<Map<String, Object>> dispararConsentimientosMasivos(@RequestBody List<Integer> idsSeleccionados) {
        Map<String, Object> resultado = notificacionesService.enviarConsentimientoMasivo(idsSeleccionados);
        return ResponseEntity.ok(resultado);
    }
}