package com.siontrack.siontrack.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siontrack.siontrack.DTO.Request.ProductosRequestDTO;
import com.siontrack.siontrack.DTO.Response.ProductosResponseDTO;

import com.siontrack.siontrack.models.Inventario;
import com.siontrack.siontrack.models.Productos;
import com.siontrack.siontrack.models.Proveedores;
import com.siontrack.siontrack.repository.ProductosRepository;
import com.siontrack.siontrack.repository.ProveedoresRepository;


@Service
public class ProductosServicios {

    @Autowired
    private ProductosRepository productosRepository;

    @Autowired
    private ProveedoresRepository proveedorRepository;

    // Inject ModelMapper
    @Autowired
    private ModelMapper modelMapper;

    /**
     * READ (List) - Returns a list of Product Response DTOs.
     */
    @Transactional(readOnly = true) // Good practice for read operations
    public List<ProductosResponseDTO> obtenerListaProductos() {
        return productosRepository.findAll().stream()
                .map(producto -> modelMapper.map(producto, ProductosResponseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * READ (Single) - Returns a single Product Response DTO by ID.
     */
    @Transactional(readOnly = true)
    public ProductosResponseDTO obtenerProductoByID(Integer id) {
        Productos producto = productosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id)); // Or a custom exception
        return modelMapper.map(producto, ProductosResponseDTO.class);
    }

    /**
     * CREATE - Creates a new Product and its Inventory from a Request DTO.
     */
    @Transactional
    public ProductosResponseDTO crearProducto(ProductosRequestDTO dto) {

        // 1. Fetch Related Entity (Proveedor) using ID from DTO
        Proveedores proveedor = proveedorRepository.findById(dto.getProveedor_id())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + dto.getProveedor_id()));

        // 2. Map DTO basics to Product Entity
        Productos producto = modelMapper.map(dto, Productos.class);

        // 3. Set the relationship (Owner side)
        producto.setProveedor(proveedor);

        // 4. Create and link Inventory (One-to-One)
        // Check if inventory data is provided in the DTO
        if (dto.getCantidad_disponible() != null) { // Use a key field like cantidadDisponible
             Inventario inventario = new Inventario();
             // Map inventory fields from DTO manually or configure ModelMapper
             inventario.setCantidad_disponible(dto.getCantidad_disponible());
             inventario.setStock_minimo(dto.getStock_minimo());
             inventario.setUbicacion(dto.getUbicacion());

             // Set bidirectional relationship
             inventario.setProducto(producto);
             producto.setInventario(inventario);
        }

        // 5. Bidirectional sync for Proveedor list (optional but good practice)
        proveedor.getProductos().add(producto);

        // 6. Save Product (Inventory will be saved automatically if CascadeType.ALL is set)
        Productos savedProducto = productosRepository.save(producto);

        // 7. Map saved Entity back to Response DTO
        return modelMapper.map(savedProducto, ProductosResponseDTO.class);
    }

     /**
     * UPDATE - Updates an existing Product and its Inventory from a Request DTO.
     */
    @Transactional
    public ProductosResponseDTO actualizarProducto(Integer id, ProductosRequestDTO dto) {

        // 1. Fetch Existing Product
        Productos productoExistente = productosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));

        // 2. Map DTO fields onto the existing entity
        // Configure ModelMapper to skip nulls if you want partial updates:
        // modelMapper.getConfiguration().setSkipNullEnabled(true);
        // Map simple fields (name, prices, category, etc.)
        modelMapper.map(dto, productoExistente);
        // Ensure ID is not overwritten if mapping DTO -> Entity directly

        // 3. Update Relationship: Proveedor (if ID is provided and different)
        if (dto.getProveedor_id() != null &&
            (productoExistente.getProveedor() == null || !dto.getProveedor_id().equals(productoExistente.getProveedor().getProveedor_id()))) {

            Proveedores nuevoProveedor = proveedorRepository.findById(dto.getProveedor_id())
                    .orElseThrow(() -> new RuntimeException("Proveedor no encontrado: " + dto.getProveedor_id()));

            // Update bidirectional links
            if (productoExistente.getProveedor() != null) {
                productoExistente.getProveedor().getProductos().remove(productoExistente);
            }
            nuevoProveedor.getProductos().add(productoExistente);
            productoExistente.setProveedor(nuevoProveedor); // Set new relationship
        }

        // 4. Update/Create Inventory
        Inventario inventario = productoExistente.getInventario();
        boolean inventarioDataProvided = dto.getCantidad_disponible() != null || dto.getStock_minimo() != null || dto.getUbicacion() != null;

        if (inventarioDataProvided) {
            // Create inventory if it doesn't exist
            if (inventario == null) {
                inventario = new Inventario();
                inventario.setProducto(productoExistente);
                productoExistente.setInventario(inventario);
            }
            // Update inventory fields if provided in DTO
            if (dto.getCantidad_disponible() != null) {
                inventario.setCantidad_disponible(dto.getCantidad_disponible());
            }
            if (dto.getStock_minimo() != null) {
                inventario.setStock_minimo(dto.getStock_minimo());
            }
            if (dto.getUbicacion() != null) {
                inventario.setUbicacion(dto.getUbicacion());
            }
        }

        // 5. Save Updated Product (and potentially Inventory)
        System.out.println("ID before save: " + productoExistente.getProducto_id());
        Productos productoActualizado = productosRepository.save(productoExistente);

        // 6. Map updated Entity back to Response DTO
        return modelMapper.map(productoActualizado, ProductosResponseDTO.class);
    }

    /**
     * DELETE - Deletes a Product by ID.
     */
    @Transactional
    public void borrarProducto(Integer id) {
        if (!productosRepository.existsById(id)) {
            throw new RuntimeException("Producto no encontrado con ID: " + id);
        }
        // Deletion cascades to Inventory if configured (CascadeType.ALL/REMOVE, orphanRemoval=true)
        productosRepository.deleteById(id);
    }

    // The 'saveProducto' method is likely redundant now, as 'crearProducto' and 'actualizarProducto'
    // handle the creation/update logic using DTOs. You can remove it unless it serves a specific
    // purpose related to direct form binding without DTOs.
    /*
    @Transactional
    public Productos saveProducto(Productos producto) {
        // ... previous logic ...
        return productosRepository.save(producto);
    }
    */
}