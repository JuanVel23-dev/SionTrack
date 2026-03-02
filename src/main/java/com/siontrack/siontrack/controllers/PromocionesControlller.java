package com.siontrack.siontrack.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.siontrack.siontrack.DTO.Request.PromocionesRequestDTO;
import com.siontrack.siontrack.services.NotificacionesService;

@RestController
@RequestMapping("/api/promociones")
public class PromocionesControlller {
    private final NotificacionesService notificacionesService;

    public PromocionesControlller (NotificacionesService notificacionesService){
        this.notificacionesService = notificacionesService;
    }

    @PostMapping("/enviar")
    public ResponseEntity<Map<String, Object>> enviarPromocion(@RequestBody PromocionesRequestDTO dto) {
        Map<String, Object> resultado = notificacionesService.enviarPromocion(dto);
        return ResponseEntity.ok(resultado);
    }
}
