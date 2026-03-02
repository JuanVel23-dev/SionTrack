package com.siontrack.siontrack.controllers;

import org.modelmapper.ModelMapper; 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

import com.siontrack.siontrack.DTO.Request.ProveedoresRequestDTO;
import com.siontrack.siontrack.DTO.Response.ProveedoresResponseDTO;
import com.siontrack.siontrack.services.ProveedoresService;

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
    public String mostrarListaProveedores(Model model) {
        List<ProveedoresResponseDTO> listaProveedores = proveedoresService.obtenerListaProveedores();
        model.addAttribute("proveedores", listaProveedores);
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
            @ModelAttribute("proveedor") ProveedoresRequestDTO proveedorDtoDelFormulario,
            RedirectAttributes redirectAttributes) {
        
        if (proveedorId == null) {
            proveedoresService.crearProveedor(proveedorDtoDelFormulario);
            redirectAttributes.addFlashAttribute("successMessage", "Proveedor agregado exitosamente");
        } else {
            proveedoresService.actualizarProveedor(proveedorId, proveedorDtoDelFormulario);
            redirectAttributes.addFlashAttribute("successMessage", "Proveedor actualizado exitosamente");
        }
        return "redirect:/web/proveedores"; 
    }

    @GetMapping("/proveedores/eliminar/{id}")
    public String eliminarProveedor(@PathVariable Integer id, RedirectAttributes redirectAttributes) { 
        proveedoresService.borrarProveedor(id);
        redirectAttributes.addFlashAttribute("deleteMessage", "Proveedor eliminado exitosamente");
        return "redirect:/web/proveedores"; 
    }
}