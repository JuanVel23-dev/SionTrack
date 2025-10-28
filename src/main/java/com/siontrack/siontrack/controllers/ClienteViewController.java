package com.siontrack.siontrack.controllers;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller; // ¡Importante! No es RestController
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.time.LocalDate; // Importamos LocalDate

import com.siontrack.siontrack.DTO.Request.ClienteRequestDTO;
import com.siontrack.siontrack.DTO.Response.ClienteResponseDTO;
import com.siontrack.siontrack.models.Clientes;
import com.siontrack.siontrack.services.ClienteServicios;

@Controller
@RequestMapping("/web") // Base path "/web" kept as requested
public class ClienteViewController {

    private final ClienteServicios clienteServicios;
    private final ModelMapper modelMapper; // Inject ModelMapper

    // Constructor Injection
    public ClienteViewController(ClienteServicios clienteServicios, ModelMapper modelMapper) {
        this.clienteServicios = clienteServicios;
        this.modelMapper = modelMapper;
    }

    /**
     * READ (List) - GET /web/clientes
     * Muestra la lista de todos los clientes usando DTOs de Respuesta.
     */
    @GetMapping("/clientes")
    public String mostrarListaClientes(Model model) {
        // Service returns List<ClienteResponseDTO>
        List<ClienteResponseDTO> listaClientes = clienteServicios.obtenerListaClientes();
        model.addAttribute("clientes", listaClientes);
        return "clientes-lista"; // Renders /resources/templates/clientes-lista.html
    }

    /**
     * CREATE (Step 1: Show empty form) - GET /web/clientes/nuevo
     * Muestra el formulario para crear usando un Request DTO vacío.
     */
    @GetMapping("/clientes/nuevo")
    public String mostrarFormularioNuevoCliente(Model model) {
        // Use the Request DTO as the backing object for the form
        model.addAttribute("cliente", new ClienteRequestDTO());
        return "clientes-form"; // Renders /resources/templates/clientes-form.html
    }

    /**
     * UPDATE (Step 1: Show form with data) - GET /web/clientes/editar/{id}
     * Muestra el formulario para editar, cargando los datos en un Request DTO.
     */
    @GetMapping("/clientes/editar/{id}")
    public String mostrarFormularioEditarCliente(@PathVariable Integer id, Model model) {
        // 1. Get the Response DTO (contains current data)
        ClienteResponseDTO clienteExistenteDto = clienteServicios.obtenerClientePorId(id);

        // 2. Map ResponseDTO -> RequestDTO for the form
        // ModelMapper helps here, otherwise map manually
        ClienteRequestDTO clienteParaFormulario = modelMapper.map(clienteExistenteDto, ClienteRequestDTO.class);
        // Manual mapping example if not using ModelMapper:
        // ClienteRequestDTO clienteParaFormulario = new ClienteRequestDTO();
        // clienteParaFormulario.setNombre(clienteExistenteDto.getNombre());
        // clienteParaFormulario.setCedula_ruc(clienteExistenteDto.getCedulaRuc());
        // ... map other fields, potentially including lists if the form handles them

        model.addAttribute("cliente", clienteParaFormulario);
        model.addAttribute("clienteId", id); // Pass the ID separately for the save method
        return "clientes-form"; // Reuses the same form template
    }

    /**
     * CREATE & UPDATE (Step 2: Save data) - POST /web/clientes/guardar
     * Procesa el formulario enviado (vinculado al Request DTO).
     */
    @PostMapping("/clientes/guardar")
    public String guardarCliente(
            @RequestParam(value = "clienteId", required = false) Integer clienteId, // Get ID if editing
            @ModelAttribute("cliente") ClienteRequestDTO clienteDtoDelFormulario) {

        if (clienteId == null) {
            // --- NEW CLIENT ---
            clienteServicios.crearCliente(clienteDtoDelFormulario);
        } else {
            // --- UPDATE EXISTING CLIENT ---
            clienteServicios.actualizarCliente(clienteId, clienteDtoDelFormulario);
        }

        // Redirect back to the client list
        return "redirect:/web/clientes";
    }

    /**
     * DELETE - GET /web/clientes/eliminar/{id}
     * Elimina un cliente por su ID.
     */
    @GetMapping("/clientes/eliminar/{id}")
    public String eliminarCliente(@PathVariable Integer id) {
        clienteServicios.deleteCliente(id); // Assumes service has this method

        // Redirect back to the list
        return "redirect:/web/clientes";
    }
}