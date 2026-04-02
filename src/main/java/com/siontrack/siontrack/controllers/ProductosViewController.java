package com.siontrack.siontrack.controllers;

import org.modelmapper.ModelMapper; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*; 
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

import com.siontrack.siontrack.DTO.Request.ProductosRequestDTO;
import com.siontrack.siontrack.DTO.Response.ProductosResponseDTO;

import com.siontrack.siontrack.services.ProductosServicios;
import com.siontrack.siontrack.services.ProveedoresService;

import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

@Controller
@RequestMapping("/web")
public class ProductosViewController {

    @Autowired
    private ProductosServicios productosServicios;

    @Autowired
    private ProveedoresService proveedoresService;

    @Autowired
    private ModelMapper modelMapper; 

    private void cargarProveedores(Model model) {
        List<?> listaProveedores = proveedoresService.obtenerListaProveedores(); 
        model.addAttribute("listaProveedores", listaProveedores);
    }

    @GetMapping("/productos")
    public String mostrarListaProductos(Model model) {
        List<ProductosResponseDTO> listaProductos = productosServicios.obtenerListaProductos();
        model.addAttribute("productos", listaProductos);
        return "productos-lista"; 
    }

    @GetMapping("/productos/nuevo")
    public String mostrarFormularioNuevoProducto(Model model) {
        model.addAttribute("producto", new ProductosRequestDTO());
        cargarProveedores(model); 
        return "productos-form"; 
    }

    @GetMapping("/productos/editar/{id}")
    public String mostrarFormularioEditarProducto(@PathVariable Integer id, Model model) {
        ProductosResponseDTO productoExistenteDto = productosServicios.obtenerProductoByID(id);
        ProductosRequestDTO productoParaFormulario = modelMapper.map(productoExistenteDto, ProductosRequestDTO.class);
        if (productoExistenteDto.getProveedor_id() != null) {
             productoParaFormulario.setProveedor_id(productoExistenteDto.getProveedor_id());
        }
         productoParaFormulario.setCantidad_disponible(productoExistenteDto.getCantidad_disponible());
         productoParaFormulario.setStock_minimo(productoExistenteDto.getStock_minimo());
         productoParaFormulario.setUbicacion(productoExistenteDto.getUbicacion());

        model.addAttribute("producto", productoParaFormulario);
        model.addAttribute("productoId", id);
        cargarProveedores(model); 
        return "productos-form"; 
    }

    @PostMapping("/productos/guardar")
    public String guardarProducto(
            @RequestParam(value = "productoId", required = false) Integer productoId,
            @Valid @ModelAttribute("producto") ProductosRequestDTO productoDtoDelFormulario,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            if (productoId != null) {
                model.addAttribute("productoId", productoId);
            }
            cargarProveedores(model);
            StringBuilder errores = new StringBuilder();
            bindingResult.getFieldErrors().forEach(error -> {
                if (errores.length() > 0) errores.append(". ");
                errores.append(error.getDefaultMessage());
            });
            model.addAttribute("errorMessage", errores.length() > 0 ? errores.toString() : "Por favor corrige los errores en el formulario");
            return "productos-form";
        }

        try {
            if (productoId == null) {
                productosServicios.crearProducto(productoDtoDelFormulario);
                redirectAttributes.addFlashAttribute("successMessage", "Producto agregado exitosamente");
            } else {
                productosServicios.actualizarProducto(productoId, productoDtoDelFormulario);
                redirectAttributes.addFlashAttribute("successMessage", "Producto actualizado exitosamente");
            }
        } catch (Exception e) {
            if (productoId != null) {
                model.addAttribute("productoId", productoId);
            }
            cargarProveedores(model);
            model.addAttribute("errorMessage", e.getMessage());
            return "productos-form";
        }

        return "redirect:/web/productos";
    }

    @PostMapping("/productos/eliminar/{id}")
    public String eliminarProducto(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        productosServicios.borrarProducto(id);
        redirectAttributes.addFlashAttribute("deleteMessage", "Producto eliminado exitosamente");
        return "redirect:/web/productos"; 
    }
}