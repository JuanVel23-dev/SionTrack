package com.siontrack.siontrack.services;

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

    @Autowired
    private ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public List<ProductosResponseDTO> obtenerListaProductos() {
        return productosRepository.findAll().stream()
                .map(producto -> modelMapper.map(producto, ProductosResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductosResponseDTO obtenerProductoByID(Integer id) {
        Productos producto = productosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));
        return modelMapper.map(producto, ProductosResponseDTO.class);
    }

    @Transactional
    public ProductosResponseDTO crearProducto(ProductosRequestDTO dto) {
        Proveedores proveedor = proveedorRepository.findById(dto.getProveedor_id())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + dto.getProveedor_id()));

        Productos producto = modelMapper.map(dto, Productos.class);

        producto.setProveedor(proveedor);

        if (dto.getCantidad_disponible() != null) {
            Inventario inventario = new Inventario();
            inventario.setCantidad_disponible(dto.getCantidad_disponible());
            inventario.setStock_minimo(dto.getStock_minimo());
            inventario.setUbicacion(dto.getUbicacion());

            inventario.setProducto(producto);
            producto.setInventario(inventario);
        }
        proveedor.getProductos().add(producto);

        Productos savedProducto = productosRepository.save(producto);

        return modelMapper.map(savedProducto, ProductosResponseDTO.class);
    }

    @Transactional
    public ProductosResponseDTO actualizarProducto(Integer id, ProductosRequestDTO dto) {

        Productos productoExistente = productosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));

        modelMapper.map(dto, productoExistente);

        if (dto.getProveedor_id() != null &&
                (productoExistente.getProveedor() == null
                        || !dto.getProveedor_id().equals(productoExistente.getProveedor().getProveedor_id()))) {
            Proveedores nuevoProveedor = proveedorRepository.findById(dto.getProveedor_id())
                    .orElseThrow(() -> new RuntimeException("Proveedor no encontrado: " + dto.getProveedor_id()));

            if (productoExistente.getProveedor() != null) {
                productoExistente.getProveedor().getProductos().remove(productoExistente);
            }
            nuevoProveedor.getProductos().add(productoExistente);
            productoExistente.setProveedor(nuevoProveedor);
        }

        Inventario inventario = productoExistente.getInventario();
        boolean inventarioDataProvided = dto.getCantidad_disponible() != null || dto.getStock_minimo() != null
                || dto.getUbicacion() != null;

        if (inventarioDataProvided) {
            if (inventario == null) {
                inventario = new Inventario();
                inventario.setProducto(productoExistente);
                productoExistente.setInventario(inventario);
            }
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

        System.out.println("ID before save: " + productoExistente.getProducto_id());
        Productos productoActualizado = productosRepository.save(productoExistente);

        return modelMapper.map(productoActualizado, ProductosResponseDTO.class);
    }

    @Transactional
    public void borrarProducto(Integer id) {
        if (!productosRepository.existsById(id)) {
            throw new RuntimeException("Producto no encontrado con ID: " + id);
        }
        productosRepository.deleteById(id);
    }

}