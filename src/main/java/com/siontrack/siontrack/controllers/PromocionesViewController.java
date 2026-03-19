package com.siontrack.siontrack.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.siontrack.siontrack.DTO.Request.PromocionesRequestDTO;
import com.siontrack.siontrack.services.NotificacionesService;

@Controller
@RequestMapping("/web")
public class PromocionesViewController {

    @Autowired
    private NotificacionesService notificacionesService;

    /**
     * Lista con tabs: Promociones / Recordatorios
     */
    @GetMapping("/notificaciones")
    public String mostrarNotificaciones(Model model) {
        model.addAttribute("promociones", notificacionesService.obtenerPromocionesEnviadas());
        model.addAttribute("recordatorios", notificacionesService.obtenerRecordatorios());
        return "notificaciones-lista";
    }

    /**
     * Formulario para crear nueva promoción
     */
    @GetMapping("/notificaciones/promocion/nueva")
    public String mostrarFormularioPromocion(Model model) {
        model.addAttribute("promocion", new PromocionesRequestDTO());
        model.addAttribute("productosDisponibles", notificacionesService.obtenerProductosDisponibles());
        return "promociones-form";
    }

    /**
     * Enviar promoción
     */
    @PostMapping("/notificaciones/promocion/enviar")
    public String enviarPromocion(
            @ModelAttribute("promocion") PromocionesRequestDTO dto,
            RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> resultado = notificacionesService.enviarPromocion(dto);

            int enviados = (int) resultado.getOrDefault("enviados", 0);
            int fallidos = (int) resultado.getOrDefault("fallidos", 0);
            int sinTelefono = (int) resultado.getOrDefault("sinTelefono", 0);
            int sinConsentimiento = (int) resultado.getOrDefault("sinConsentimiento", 0);
            int clientes = (int) resultado.getOrDefault("clientesEncontrados", 0);
            String nombreProducto = (String) resultado.getOrDefault("producto", "");

            String mensaje = String.format(
                "Promoción enviada — %d de %d clientes notificados. Fallidos: %d, Sin teléfono: %d, Sin consentimiento: %d",
                enviados, clientes, fallidos, sinTelefono, sinConsentimiento
            );

            if (enviados > 0) {
                redirectAttributes.addFlashAttribute("successMessage", mensaje);
            } else if (clientes == 0) {
                redirectAttributes.addFlashAttribute("errorMessage",
                    "No se encontraron clientes asociados al producto \"" + nombreProducto + "\"");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", mensaje);
            }
        } catch (Exception e) {
            // Log del error real en consola para debugging
            System.err.println("❌ Error enviando promoción: " + e.getMessage());
            
            // Mensaje limpio para el usuario
            redirectAttributes.addFlashAttribute("errorMessage",
                "Ocurrió un error al enviar la promoción. Verifica la conexión e intenta nuevamente.");
        }
        return "redirect:/web/notificaciones";
    }
}