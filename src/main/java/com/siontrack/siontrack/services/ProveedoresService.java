package com.siontrack.siontrack.services;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siontrack.siontrack.DTO.Request.ProveedoresRequestDTO;
import com.siontrack.siontrack.DTO.Response.ProveedoresResponseDTO;

import com.siontrack.siontrack.models.Proveedores;
import com.siontrack.siontrack.repository.ProveedoresRepository;

/**
 * Gestiona el ciclo de vida de los proveedores de productos.
 */
@Service
public class ProveedoresService {

    @Autowired
    private ProveedoresRepository proveedoresRepository;

    @Autowired
    private ModelMapper modelMapper;

    /**
     * Elimina todos los caracteres no numéricos de un número de teléfono.
     * Ejemplo: {@code "+57 318-325 2987"} → {@code "573183252987"}.
     */
    private String limpiarTelefono(String telefono) {
        if (telefono == null) return null;
        return telefono.replaceAll("[^0-9]", "");
    }

    /**
     * Devuelve la lista completa de proveedores sin paginación.
     *
     * @return lista de todos los proveedores mapeados a DTO
     */
    @Transactional(readOnly = true)
    public List<ProveedoresResponseDTO> obtenerListaProveedores() {
        return proveedoresRepository.findAll().stream()
                .map(proveedor -> modelMapper.map(proveedor, ProveedoresResponseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Devuelve los proveedores paginados, con búsqueda opcional por nombre.
     *
     * @param pageable  configuración de paginación y orden
     * @param busqueda  término de búsqueda (puede ser nulo o vacío para listar todos)
     * @return página de proveedores mapeados a DTO
     */
    @Transactional(readOnly = true)
    public Page<ProveedoresResponseDTO> obtenerListaProveedoresPaginado(Pageable pageable, String busqueda) {
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            return proveedoresRepository.buscarPaginado(busqueda.trim(), pageable)
                    .map(proveedor -> modelMapper.map(proveedor, ProveedoresResponseDTO.class));
        }
        return proveedoresRepository.findAllOrderByIdDesc(pageable)
                .map(proveedor -> modelMapper.map(proveedor, ProveedoresResponseDTO.class));
    }

    /**
     * Obtiene un proveedor por su ID.
     *
     * @param id ID del proveedor
     * @return DTO del proveedor
     * @throws RuntimeException si el proveedor no existe
     */
    @Transactional(readOnly = true)
    public ProveedoresResponseDTO obtenerProveedorId(Integer id) {
        Proveedores proveedor = proveedoresRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + id));
        return modelMapper.map(proveedor, ProveedoresResponseDTO.class);
    }

    /**
     * Crea un nuevo proveedor. El número de teléfono se normaliza antes de persistir.
     *
     * @param dto datos del proveedor a crear
     * @return DTO del proveedor creado
     */
    @Transactional
    public ProveedoresResponseDTO crearProveedor(ProveedoresRequestDTO dto) {
        dto.setTelefono(limpiarTelefono(dto.getTelefono()));

        Proveedores proveedor = modelMapper.map(dto, Proveedores.class);
        Proveedores savedProveedor = proveedoresRepository.save(proveedor);

        return modelMapper.map(savedProveedor, ProveedoresResponseDTO.class);
    }

    /**
     * Actualiza los datos de un proveedor existente.
     *
     * @param id  ID del proveedor a actualizar
     * @param dto datos nuevos del proveedor
     * @return DTO del proveedor actualizado
     * @throws RuntimeException si el proveedor no existe
     */
    @Transactional
    public ProveedoresResponseDTO actualizarProveedor(Integer id, ProveedoresRequestDTO dto) {

        Proveedores proveedorExistente = proveedoresRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + id));

        dto.setTelefono(limpiarTelefono(dto.getTelefono()));

        modelMapper.map(dto, proveedorExistente);
        proveedorExistente.setProveedor_id(id);

        Proveedores proveedorActualizado = proveedoresRepository.save(proveedorExistente);
        return modelMapper.map(proveedorActualizado, ProveedoresResponseDTO.class);
    }

    /**
     * Busca el ID de un proveedor por su nombre (insensible a mayúsculas/minúsculas).
     * Usado por {@link ImportacionService} para resolver el proveedor desde el nombre
     * indicado en el archivo Excel.
     *
     * @param nombre nombre del proveedor a buscar
     * @return ID del proveedor encontrado
     * @throws RuntimeException si no existe ningún proveedor con ese nombre
     */
    @Transactional(readOnly = true)
    public Integer buscarIdPorNombre(String nombre) {
        return proveedoresRepository.findByNombreIgnoreCase(nombre)
                .map(Proveedores::getProveedor_id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con nombre: " + nombre));
    }

    /**
     * Elimina un proveedor por su ID.
     *
     * @param id ID del proveedor a eliminar
     * @throws RuntimeException si el proveedor no existe
     */
    @Transactional
    public void borrarProveedor(Integer id) {
        if (!proveedoresRepository.existsById(id)) {
            throw new RuntimeException("Proveedor no encontrado con ID: " + id);
        }
        proveedoresRepository.deleteById(id);
    }
}
