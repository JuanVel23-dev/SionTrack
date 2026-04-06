package com.siontrack.siontrack.controllers;

import org.modelmapper.ModelMapper; 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.siontrack.siontrack.DTO.Request.ProveedoresRequestDTO;
import com.siontrack.siontrack.DTO.Response.ProveedoresResponseDTO;
import com.siontrack.siontrack.services.ProveedoresService;

import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

@Controller
@RequestMapping("/web") 
public class ProveedoresViewController {

    private final ProveedoresService proveedoresService;
    private final ModelMapper modelMapper; 

    public ProveedoresViewController(ProveedoresService proveedoresService, ModelMapper modelMapper) {
        this.proveedoresService = proveedoresService;
        this.modelMapper = modelMapper;
    }

    @GetMapping("/proveedores")
    public String mostrarListaProveedores(
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Page<ProveedoresResponseDTO> pagina = proveedoresService.obtenerListaProveedoresPaginado(
                PageRequest.of(page, 50));
        model.addAttribute("proveedores", pagina.getContent());
        model.addAttribute("page", pagina);
        return "proveedores-lista";
    }

    @GetMapping("/proveedores/nuevo")
    public String mostrarFormularioNuevoProveedor(Model model) {
        model.addAttribute("proveedor", new ProveedoresRequestDTO());
        return "proveedores-form"; 
    }

    @GetMapping("/proveedores/editar/{id}")
    public String mostrarFormularioEditarProveedor(@PathVariable Integer id, Model model) { 
        ProveedoresResponseDTO proveedorExistenteDto = proveedoresService.obtenerProveedorId(id);
        ProveedoresRequestDTO proveedorParaFormulario = modelMapper.map(proveedorExistenteDto, ProveedoresRequestDTO.class);

        model.addAttribute("proveedor", proveedorParaFormulario);
        model.addAttribute("proveedorId", id); 
        return "proveedores-form"; 
    }

    @PostMapping("/proveedores/guardar")
    public String guardarProveedor(
            @RequestParam(value = "proveedorId", required = false) Integer proveedorId,
            @Valid @ModelAttribute("proveedor") ProveedoresRequestDTO proveedorDtoDelFormulario,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            if (proveedorId != null) {
                model.addAttribute("proveedorId", proveedorId);
            }
            StringBuilder errores = new StringBuilder();
            bindingResult.getFieldErrors().forEach(error -> {
                if (errores.length() > 0) errores.append(". ");
                errores.append(error.getDefaultMessage());
            });
            model.addAttribute("errorMessage", errores.length() > 0 ? errores.toString() : "Por favor corrige los errores en el formulario");
            return "proveedores-form";
        }

        try {
            if (proveedorId == null) {
                proveedoresService.crearProveedor(proveedorDtoDelFormulario);
                redirectAttributes.addFlashAttribute("successMessage", "Proveedor agregado exitosamente");
            } else {
                proveedoresService.actualizarProveedor(proveedorId, proveedorDtoDelFormulario);
                redirectAttributes.addFlashAttribute("successMessage", "Proveedor actualizado exitosamente");
            }
        } catch (Exception e) {
            if (proveedorId != null) {
                model.addAttribute("proveedorId", proveedorId);
            }
            model.addAttribute("errorMessage", e.getMessage());
            return "proveedores-form";
        }
        return "redirect:/web/proveedores";
    }

    @PostMapping("/proveedores/eliminar/{id}")
    public String eliminarProveedor(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        proveedoresService.borrarProveedor(id);
        redirectAttributes.addFlashAttribute("deleteMessage", "Proveedor eliminado exitosamente");
        return "redirect:/web/proveedores"; 
    }
}