package com.siontrack.siontrack.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.siontrack.siontrack.DTO.Request.PromocionesRequestDTO;
import com.siontrack.siontrack.DTO.Response.ClientePreviewDTO;
import com.siontrack.siontrack.services.NotificacionesService;

@RestController
@RequestMapping("/api/promociones")
public class PromocionesControlller {
    private final NotificacionesService notificacionesService;

    public PromocionesControlller (NotificacionesService notificacionesService){
        this.notificacionesService = notificacionesService;
    }

    /**
     * Retorna la lista de clientes elegibles para una promoción del producto dado,
     * con su estado de contacto reciente. El frontend lo usa para mostrar la selección previa.
     */
    @GetMapping("/preview")
    public ResponseEntity<List<ClientePreviewDTO>> previewClientes(@RequestParam Integer productoId) {
        return ResponseEntity.ok(notificacionesService.obtenerPreviewClientes(productoId));
    }

    @PostMapping("/enviar")
    public ResponseEntity<Map<String, Object>> enviarPromocion(@RequestBody PromocionesRequestDTO dto) {
        Map<String, Object> resultado = notificacionesService.enviarPromocion(dto);
        return ResponseEntity.ok(resultado);
    }

    // Actualiza únicamente la fecha de envío de un recordatorio pendiente
    @PatchMapping("/recordatorio/{id}/fecha")
    public ResponseEntity<?> actualizarFechaRecordatorio(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        LocalDate nuevaFecha = LocalDate.parse(body.get("fecha"));
        notificacionesService.actualizarFechaProgramada(id, nuevaFecha);
        return ResponseEntity.ok(Map.of("mensaje", "Fecha actualizada correctamente"));
    }
}
