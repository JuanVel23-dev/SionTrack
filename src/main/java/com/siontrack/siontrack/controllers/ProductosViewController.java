package com.siontrack.siontrack.controllers;

import org.modelmapper.ModelMapper; // Import ModelMapper
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*; // Use @RequestParam
import java.util.List;

import com.siontrack.siontrack.DTO.Request.ProductosRequestDTO;
import com.siontrack.siontrack.DTO.Response.ProductosResponseDTO;
// Entity and Service Imports
import com.siontrack.siontrack.services.ProductosServicios;
import com.siontrack.siontrack.services.ProveedoresService;

@Controller
@RequestMapping("/web")
public class ProductosViewController {

    @Autowired
    private ProductosServicios productosServicios;

    @Autowired
    private ProveedoresService proveedoresService;

    @Autowired
    private ModelMapper modelMapper; 

    /**
     * Helper method to load providers into the model.
     * Consider returning List<ProveedorResponseDTO> if available.
     */
    private void cargarProveedores(Model model) {
        // Assuming obtenerListaProveedores returns List<Proveedores> entity or List<ProveedorResponseDTO>
        List<?> listaProveedores = proveedoresService.obtenerListaProveedores(); // Use appropriate return type
        model.addAttribute("listaProveedores", listaProveedores);
    }

    /**
     * READ (List) - Uses ProductoResponseDTO
     */
    @GetMapping("/productos")
    public String mostrarListaProductos(Model model) {
        // Service now returns List<ProductoResponseDTO>
        List<ProductosResponseDTO> listaProductos = productosServicios.obtenerListaProductos();
        model.addAttribute("productos", listaProductos);
        return "productos-lista"; // Renders productos-lista.html
    }

    /**
     * CREATE (Step 1: Show empty form) - Uses ProductoRequestDTO
     */
    @GetMapping("/productos/nuevo")
    public String mostrarFormularioNuevoProducto(Model model) {
        // Form is backed by the Request DTO
        model.addAttribute("producto", new ProductosRequestDTO());
        cargarProveedores(model); // Load providers for the dropdown
        return "productos-form"; // Renders productos-form.html
    }

    /**
     * UPDATE (Step 1: Show form with data) - Maps ResponseDTO to RequestDTO
     */
    @GetMapping("/productos/editar/{id}")
    public String mostrarFormularioEditarProducto(@PathVariable Integer id, Model model) {
        // 1. Get the Response DTO (contains current data)
        ProductosResponseDTO productoExistenteDto = productosServicios.obtenerProductoByID(id);

        // 2. Map ResponseDTO -> RequestDTO for the form using ModelMapper
        ProductosRequestDTO productoParaFormulario = modelMapper.map(productoExistenteDto, ProductosRequestDTO.class);
        // Manually set supplierId if needed, as ModelMapper might not map nested IDs automatically
        if (productoExistenteDto.getProveedor_id() != null) {
             productoParaFormulario.setProveedor_id(productoExistenteDto.getProveedor_id());
        }
         // Map inventory fields from Response to Request DTO
         productoParaFormulario.setCantidad_disponible(productoExistenteDto.getCantidad_disponible());
         productoParaFormulario.setStock_minimo(productoExistenteDto.getStock_minimo());
         productoParaFormulario.setUbicacion(productoExistenteDto.getUbicacion());


        model.addAttribute("producto", productoParaFormulario);
        model.addAttribute("productoId", id); // Pass the ID for the save method
        cargarProveedores(model); // Load providers for the dropdown
        return "productos-form"; // Renders productos-form.html
    }

    /**
     * CREATE & UPDATE (Step 2: Save data) - Uses ProductoRequestDTO
     */
    @PostMapping("/productos/guardar")
    public String guardarProducto(
            @RequestParam(value = "productoId", required = false) Integer productoId, // Get ID if editing
            @ModelAttribute("producto") ProductosRequestDTO productoDtoDelFormulario) {

        if (productoId == null) {
            // --- NEW PRODUCT ---
            productosServicios.crearProducto(productoDtoDelFormulario);
        } else {
            // --- UPDATE EXISTING PRODUCT ---
            productosServicios.actualizarProducto(productoId, productoDtoDelFormulario);
        }

        return "redirect:/web/productos"; // Redirect back to the list
    }

    /**
     * DELETE
     */
    @GetMapping("/productos/eliminar/{id}")
    public String eliminarProducto(@PathVariable Integer id) {
        productosServicios.borrarProducto(id);
        return "redirect:/web/productos"; // Redirect back to the list
    }
}