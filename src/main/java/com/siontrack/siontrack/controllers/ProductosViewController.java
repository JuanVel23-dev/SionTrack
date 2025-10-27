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

import com.siontrack.siontrack.models.Inventario;
import com.siontrack.siontrack.models.Productos;
import com.siontrack.siontrack.models.Proveedores;
import com.siontrack.siontrack.services.ProductosServicios;
import com.siontrack.siontrack.services.ProveedoresService; // Necesitamos este servicio

@Controller
@RequestMapping("/web")
public class ProductosViewController {

    @Autowired
    private ProductosServicios productosServicios;

    @Autowired
    private ProveedoresService proveedoresService; // Inyectamos el servicio de Proveedores

    /**
     * Método para cargar la lista de proveedores en el modelo.
     * Lo usaremos en los formularios 'nuevo' y 'editar'.
     */
    private void cargarProveedores(Model model) {
        List<Proveedores> listaProveedores = proveedoresService.obtenerListaProveedores();
        model.addAttribute("listaProveedores", listaProveedores);
    }

    /**
     * READ (List)
     */
    @GetMapping("/productos")
    public String mostrarListaProductos(Model model) {
        List<Productos> listaProductos = productosServicios.obtenerListaProductos();
        model.addAttribute("productos", listaProductos);
        return "productos-lista";
    }

    /**
     * CREATE (Paso 1: Mostrar formulario vacío)
     */
    @GetMapping("/productos/nuevo")
    public String mostrarFormularioNuevoProducto(Model model) {
        // Creamos un producto y un inventario nuevos
        Productos producto = new Productos();
        producto.setInventario(new Inventario()); // ¡Importante! Creamos un inventario vacío
        
        model.addAttribute("producto", producto);
        cargarProveedores(model); // Cargamos la lista de proveedores para el dropdown
        return "productos-form";
    }

    /**
     * UPDATE (Paso 1: Mostrar formulario con datos)
     */
    @GetMapping("/productos/editar/{id}")
    public String mostrarFormularioEditarProducto(@PathVariable int id, Model model) {
        Productos producto = productosServicios.obtenerProductoByID(id)
            .orElseThrow(() -> new IllegalArgumentException("ID de producto no válido: " + id));
        
        model.addAttribute("producto", producto);
        cargarProveedores(model); // Cargamos la lista de proveedores
        return "productos-form";
    }

    /**
     * CREATE y UPDATE (Paso 2: Guardar los datos)
     */
    @PostMapping("/productos/guardar")
    public String guardarProducto(@ModelAttribute("producto") Productos producto) {
        // Usamos nuestro nuevo método de servicio
        productosServicios.saveProducto(producto);
        return "redirect:/web/productos";
    }

    /**
     * DELETE
     */
    @GetMapping("/productos/eliminar/{id}")
    public String eliminarProducto(@PathVariable int id) {
        productosServicios.borrarProducto(id);
        return "redirect:/web/productos";
    }
}
