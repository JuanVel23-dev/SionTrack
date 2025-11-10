package com.siontrack.siontrack.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.siontrack.siontrack.DTO.Request.VehiculosRequestDTO;
import com.siontrack.siontrack.services.ClienteServicios;

@Controller
@RequestMapping("/web/vehiculos") // Define el prefijo para este controlador
public class VehiculoViewController {

    private final ClienteServicios clienteServicios;

    // Inyección de dependencias (igual que en tus otros controladores)
    public VehiculoViewController(ClienteServicios clienteServicios) {
        this.clienteServicios = clienteServicios;
    }

    /**
     * Este método recibe el POST del formulario 'vehiculo-form.html'
     */
    @PostMapping("/guardar")
    public String guardarVehiculo(@ModelAttribute("vehiculo") VehiculosRequestDTO vehiculoDto,
                                  @RequestParam("clienteId") Integer clienteId,
                                  RedirectAttributes redirectAttributes) {
        
        try {
            // Llama al nuevo método de servicio que creamos
            clienteServicios.crearVehiculoParaCliente(vehiculoDto, clienteId);
            
            // Prepara un mensaje de éxito para mostrar en la siguiente página
            redirectAttributes.addFlashAttribute("successMessage", "Vehículo agregado exitosamente.");

        } catch (Exception e) {
            // Si algo sale mal, envía un mensaje de error
            redirectAttributes.addFlashAttribute("errorMessage", "Error al guardar el vehículo: " + e.getMessage());
        }
        
        // Redirige al usuario de vuelta a la lista de clientes
        return "redirect:/web/clientes";
    }
}
