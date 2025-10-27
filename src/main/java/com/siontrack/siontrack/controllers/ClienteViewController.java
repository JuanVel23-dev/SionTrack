package com.siontrack.siontrack.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller; // ¡Importante! No es RestController
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;
import java.time.LocalDate; // Importamos LocalDate

import com.siontrack.siontrack.models.Clientes;
import com.siontrack.siontrack.services.ClienteServicios;

@Controller
@RequestMapping("/web")
public class ClienteViewController {

    @Autowired
    private ClienteServicios clienteServicios;

    /**
     * READ (List)
     * Muestra la lista de todos los clientes.
     */
    @GetMapping("/clientes")
    public String mostrarListaClientes(Model model) {
        List<Clientes> listaClientes = clienteServicios.obtenerListaClientes();
        model.addAttribute("clientes", listaClientes);
        return "clientes-lista"; // Muestra /resources/templates/clientes-lista.html
    }

    /**
     * CREATE (Paso 1: Mostrar formulario vacío)
     * Muestra el formulario para crear un nuevo cliente.
     */
    @GetMapping("/clientes/nuevo")
    public String mostrarFormularioNuevoCliente(Model model) {
        // Creamos un objeto cliente vacío para vincularlo al formulario
        Clientes cliente = new Clientes();
        model.addAttribute("cliente", cliente);
        return "clientes-form"; // Muestra /resources/templates/clientes-form.html
    }

    /**
     * UPDATE (Paso 1: Mostrar formulario con datos)
     * Muestra el formulario para editar un cliente existente.
     */
    @GetMapping("/clientes/editar/{id}")
    public String mostrarFormularioEditarCliente(@PathVariable int id, Model model) {
        // Buscamos el cliente por ID
        Clientes cliente = clienteServicios.getClienteById(id)
            .orElseThrow(() -> new IllegalArgumentException("ID de cliente no válido: " + id));
        
        // Pasamos el cliente encontrado al modelo para rellenar el formulario
        model.addAttribute("cliente", cliente);
        return "clientes-form"; // Reutiliza el mismo formulario
    }

    /**
     * CREATE y UPDATE (Paso 2: Guardar los datos)
     * Procesa el formulario enviado (tanto para crear como para actualizar).
     */
    @PostMapping("/clientes/guardar")
    public String guardarCliente(@ModelAttribute("cliente") Clientes clienteDelFormulario) {
        
        if (clienteDelFormulario.getCliente_id() == 0) {
            // --- ES UN CLIENTE NUEVO ---
            // El @PrePersist en tu modelo `Clientes` se encargará de `fecha_registro`.
            clienteServicios.saveCliente(clienteDelFormulario);
        } else {
            // --- ES UNA ACTUALIZACIÓN ---
            
            // 1. Obtenemos el cliente COMPLETO de la BD (con sus listas)
            Clientes clienteExistente = clienteServicios.getClienteById(clienteDelFormulario.getCliente_id())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

            // 2. Actualizamos SOLO los campos del formulario.
            //    ¡Importante! Dejamos las listas (telefonos, correos) que 
            //    trajo `clienteExistente` para no borrarlas.
            clienteExistente.setNombre(clienteDelFormulario.getNombre());
            clienteExistente.setCedula_ruc(clienteDelFormulario.getCedula_ruc());
            clienteExistente.setTipo_cliente(clienteDelFormulario.getTipo_cliente());
            
            // 3. Asignamos la fecha de modificación manualmente
            clienteExistente.setFecha_modificacion(LocalDate.now());

            // 4. Guardamos el cliente existente ya modificado
            clienteServicios.saveCliente(clienteExistente);
        }
        
        // Redirigimos al usuario de vuelta a la lista de clientes
        return "redirect:/web/clientes";
    }

    /**
     * DELETE
     * Elimina un cliente por su ID.
     */
    @GetMapping("/clientes/eliminar/{id}")
    public String eliminarCliente(@PathVariable int id) {
        clienteServicios.deleteCliente(id);
        
        // Redirigimos al usuario de vuelta a la lista
        return "redirect:/web/clientes";
    }
}