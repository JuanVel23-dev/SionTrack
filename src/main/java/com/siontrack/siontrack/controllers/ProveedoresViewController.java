package com.siontrack.siontrack.controllers;

import org.modelmapper.ModelMapper; // Import ModelMapper
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*; // Use @RequestParam
import java.util.List;

// DTO Imports
import com.siontrack.siontrack.DTO.Request.ProveedoresRequestDTO;
import com.siontrack.siontrack.DTO.Response.ProveedoresResponseDTO;

// Service Import
import com.siontrack.siontrack.services.ProveedoresService;

@Controller
@RequestMapping("/web") // Keeping the base path
public class ProveedoresViewController {

    private final ProveedoresService proveedoresService;
    private final ModelMapper modelMapper; // Inject ModelMapper

    // Constructor Injection
    public ProveedoresViewController(ProveedoresService proveedoresService, ModelMapper modelMapper) {
        this.proveedoresService = proveedoresService;
        this.modelMapper = modelMapper;
    }

    /**
     * READ (List) - Uses ProveedoresResponseDTO
     */
    @GetMapping("/proveedores")
    public String mostrarListaProveedores(Model model) {
        // Service now returns List<ProveedoresResponseDTO>
        List<ProveedoresResponseDTO> listaProveedores = proveedoresService.obtenerListaProveedores();
        model.addAttribute("proveedores", listaProveedores);
        return "proveedores-lista"; // Renders proveedores-lista.html
    }

    /**
     * CREATE (Step 1: Show empty form) - Uses ProveedoresRequestDTO
     */
    @GetMapping("/proveedores/nuevo")
    public String mostrarFormularioNuevoProveedor(Model model) {
        // Form is backed by the Request DTO
        model.addAttribute("proveedor", new ProveedoresRequestDTO());
        return "proveedores-form"; // Renders proveedores-form.html
    }

    /**
     * UPDATE (Step 1: Show form with data) - Maps ResponseDTO to RequestDTO
     */
    @GetMapping("/proveedores/editar/{id}")
    public String mostrarFormularioEditarProveedor(@PathVariable Integer id, Model model) { // Use Integer for ID
        // 1. Get the Response DTO (contains current data)
        ProveedoresResponseDTO proveedorExistenteDto = proveedoresService.obtenerProveedorId(id);

        // 2. Map ResponseDTO -> RequestDTO for the form using ModelMapper
        ProveedoresRequestDTO proveedorParaFormulario = modelMapper.map(proveedorExistenteDto, ProveedoresRequestDTO.class);

        model.addAttribute("proveedor", proveedorParaFormulario);
        model.addAttribute("proveedorId", id); // Pass the ID separately for the save method
        return "proveedores-form"; // Reuses the same form template
    }

    /**
     * CREATE & UPDATE (Step 2: Save data) - Uses ProveedoresRequestDTO
     */
    @PostMapping("/proveedores/guardar")
    public String guardarProveedor(
            @RequestParam(value = "proveedorId", required = false) Integer proveedorId, // Get ID if editing
            @ModelAttribute("proveedor") ProveedoresRequestDTO proveedorDtoDelFormulario) {

        if (proveedorId == null) {
            // --- NEW PROVIDER ---
            proveedoresService.crearProveedor(proveedorDtoDelFormulario);
        } else {
            // --- UPDATE EXISTING PROVIDER ---
            proveedoresService.actualizarProveedor(proveedorId, proveedorDtoDelFormulario);
        }

        return "redirect:/web/proveedores"; // Redirect back to the provider list
    }

    /**
     * DELETE
     */
    @GetMapping("/proveedores/eliminar/{id}")
    public String eliminarProveedor(@PathVariable Integer id) { // Use Integer for ID
        proveedoresService.borrarProveedor(id);
        return "redirect:/web/proveedores"; // Redirect back to the list
    }
}