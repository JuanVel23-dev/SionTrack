package com.siontrack.siontrack.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;

import com.siontrack.siontrack.models.Proveedores;
import com.siontrack.siontrack.services.ProveedoresService; // Usamos el servicio de Proveedores

@Controller
@RequestMapping("/web")
public class ProveedoresViewController {

    @Autowired
    private ProveedoresService proveedoresService; // Inyectamos el servicio

    /**
     * READ (List)
     */
    @GetMapping("/proveedores")
    public String mostrarListaProveedores(Model model) {
        List<Proveedores> listaProveedores = proveedoresService.obtenerListaProveedores();
        model.addAttribute("proveedores", listaProveedores);
        return "proveedores-lista"; // Apunta a proveedores-lista.html
    }

    /**
     * CREATE (Paso 1: Mostrar formulario vacío)
     */
    @GetMapping("/proveedores/nuevo")
    public String mostrarFormularioNuevoProveedor(Model model) {
        model.addAttribute("proveedor", new Proveedores());
        return "proveedores-form"; // Apunta a proveedores-form.html
    }

    /**
     * UPDATE (Paso 1: Mostrar formulario con datos)
     */
    @GetMapping("/proveedores/editar/{id}")
    public String mostrarFormularioEditarProveedor(@PathVariable int id, Model model) {
        Proveedores proveedor = proveedoresService.obtenerProveedorId(id)
            .orElseThrow(() -> new IllegalArgumentException("ID de proveedor no válido: " + id));
        model.addAttribute("proveedor", proveedor);
        return "proveedores-form"; // Reutiliza el mismo formulario
    }

    /**
     * CREATE y UPDATE (Paso 2: Guardar los datos)
     */
    @PostMapping("/proveedores/guardar")
    public String guardarProveedor(@ModelAttribute("proveedor") Proveedores proveedor) {
        // El servicio ya tiene el método 'guardarProveedor' que maneja create y update
        proveedoresService.guardarProveedor(proveedor);
        return "redirect:/web/proveedores";
    }

    /**
     * DELETE
     */
    @GetMapping("/proveedores/eliminar/{id}")
    public String eliminarProveedor(@PathVariable int id) {
        proveedoresService.borrarProveedor(id);
        return "redirect:/web/proveedores";
    }
}