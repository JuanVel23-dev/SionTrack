package com.siontrack.siontrack.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.siontrack.siontrack.DTO.Request.ServicioRequestDTO;
import com.siontrack.siontrack.DTO.Response.ClienteResponseDTO;
import com.siontrack.siontrack.DTO.Response.ProductosResponseDTO;
import com.siontrack.siontrack.DTO.Response.ServicioResponseDTO;
import com.siontrack.siontrack.services.ClienteServicios;
import com.siontrack.siontrack.services.ProductosServicios;
import com.siontrack.siontrack.services.ServiciosService;

import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

@Controller
@RequestMapping("/web")
public class ServiciosViewController {

    @Autowired
    private ServiciosService serviciosService;

    @Autowired
    private ClienteServicios clienteServicios;

    @Autowired
    private ProductosServicios productosServicios;

    /**
     * Carga las listas necesarias para el formulario (clientes y productos)
     */
    private void cargarDatosFormulario(Model model) {
        List<ClienteResponseDTO> listaClientes = clienteServicios.obtenerListaClientes();
        List<ProductosResponseDTO> listaProductos = productosServicios.obtenerListaProductos();
        model.addAttribute("listaClientes", listaClientes);
        model.addAttribute("listaProductos", listaProductos);
    }

    @GetMapping("/servicios")
    public String mostrarListaServicios(
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Page<ServicioResponseDTO> pagina = serviciosService.obtenerTodosPaginado(
                PageRequest.of(page, 50));
        model.addAttribute("servicios", pagina.getContent());
        model.addAttribute("page", pagina);
        return "servicios-lista";
    }

    @GetMapping("/servicios/nuevo")
    public String mostrarFormularioNuevoServicio(Model model) {
        model.addAttribute("servicio", new ServicioRequestDTO());
        cargarDatosFormulario(model);
        return "servicios-form";
    }

    @PostMapping("/servicios/guardar")
    public String guardarServicio(
            @Valid @ModelAttribute("servicio") ServicioRequestDTO servicioDtoDelFormulario,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            cargarDatosFormulario(model);
            // Construir mensaje legible con los errores de validacion
            StringBuilder errores = new StringBuilder();
            bindingResult.getFieldErrors().forEach(error -> {
                if (errores.length() > 0) errores.append(". ");
                errores.append(error.getDefaultMessage());
            });
            model.addAttribute("errorMessage", errores.length() > 0 ? errores.toString() : "Por favor corrige los errores en el formulario");
            return "servicios-form";
        }

        try {
            serviciosService.crearServicio(servicioDtoDelFormulario);
            redirectAttributes.addFlashAttribute("successMessage", "Servicio creado exitosamente");
        } catch (Exception e) {
            cargarDatosFormulario(model);
            model.addAttribute("errorMessage", e.getMessage());
            return "servicios-form";
        }
        return "redirect:/web/servicios";
    }

    @PostMapping("/servicios/eliminar/{id}")
    public String eliminarServicio(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            serviciosService.eliminarServicio(id);
            redirectAttributes.addFlashAttribute("deleteMessage", "Servicio eliminado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al eliminar: " + e.getMessage());
        }
        return "redirect:/web/servicios";
    }
}