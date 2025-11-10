package com.siontrack.siontrack.controllers;

import org.modelmapper.ModelMapper;

import org.springframework.stereotype.Controller; 
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.siontrack.siontrack.DTO.Request.VehiculosRequestDTO;

import java.util.List;

import com.siontrack.siontrack.DTO.Request.ClienteRequestDTO;
import com.siontrack.siontrack.DTO.Response.ClienteResponseDTO;
import com.siontrack.siontrack.services.ClienteServicios;

@Controller
@RequestMapping("/web") 
public class ClienteViewController {

    private final ClienteServicios clienteServicios;
    private final ModelMapper modelMapper; 

    // Constructor Injection
    public ClienteViewController(ClienteServicios clienteServicios, ModelMapper modelMapper) {
        this.clienteServicios = clienteServicios;
        this.modelMapper = modelMapper;
    }

    @GetMapping("/clientes")
    public String mostrarListaClientes(Model model) {
        List<ClienteResponseDTO> listaClientes = clienteServicios.obtenerListaClientes();
        model.addAttribute("clientes", listaClientes);
        return "clientes-lista"; 
    }

    @GetMapping("/clientes/nuevo")
    public String mostrarFormularioNuevoCliente(Model model) {
        model.addAttribute("cliente", new ClienteRequestDTO());
        return "clientes-form"; 
    }

    @GetMapping("/clientes/editar/{id}")
    public String mostrarFormularioEditarCliente(@PathVariable Integer id, Model model) {
        ClienteResponseDTO clienteExistenteDto = clienteServicios.obtenerClientePorId(id);
        ClienteRequestDTO clienteParaFormulario = modelMapper.map(clienteExistenteDto, ClienteRequestDTO.class);
        model.addAttribute("cliente", clienteParaFormulario);
        model.addAttribute("clienteId", id); 
        return "clientes-form"; 
    }

    @PostMapping("/clientes/guardar")
    public String guardarCliente(
            @RequestParam(value = "clienteId", required = false) Integer clienteId,
            @ModelAttribute("cliente") ClienteRequestDTO clienteDtoDelFormulario) {

        if (clienteId == null) {
            clienteServicios.crearCliente(clienteDtoDelFormulario);
        } else {
            clienteServicios.actualizarCliente(clienteId, clienteDtoDelFormulario);
        }
        return "redirect:/web/clientes";
    }

    @GetMapping("/clientes/eliminar/{id}")
    public String eliminarCliente(@PathVariable Integer id) {
        clienteServicios.deleteCliente(id); 
        return "redirect:/web/clientes";
    }

    @GetMapping("/clientes/{id}/vehiculos/nuevo")
public String mostrarFormularioNuevoVehiculo(@PathVariable("id") Integer clienteId, Model model) {

    // 1. Obtener el cliente para saber su nombre
    ClienteResponseDTO cliente = clienteServicios.obtenerClientePorId(clienteId);

    // 2. Crear el objeto DTO vacío para el formulario
    VehiculosRequestDTO vehiculoNuevo = new VehiculosRequestDTO();

    // 3. Añadir los datos al modelo para Thymeleaf
    model.addAttribute("vehiculo", vehiculoNuevo);
    model.addAttribute("clienteId", clienteId);
    model.addAttribute("clienteNombre", cliente.getNombre());

    // 4. Devolver el nombre del archivo HTML del formulario
    return "vehiculo-form";
}
}