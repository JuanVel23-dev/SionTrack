package com.siontrack.siontrack.services;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper; // Import ModelMapper
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Use Spring's Transactional

// DTO Imports
import com.siontrack.siontrack.DTO.Request.ProveedoresRequestDTO;
import com.siontrack.siontrack.DTO.Response.ProveedoresResponseDTO;

// Entity and Repository Imports
import com.siontrack.siontrack.models.Proveedores;
import com.siontrack.siontrack.repository.ProveedoresRepository;

// Optional: Custom exception for not found
// import com.siontrack.siontrack.exception.ResourceNotFoundException;

@Service
public class ProveedoresService {

    @Autowired
    private ProveedoresRepository proveedoresRepository;

    // Inject ModelMapper
    @Autowired
    private ModelMapper modelMapper;

    /**
     * READ (List) - Returns a list of Provider Response DTOs.
     */
    @Transactional(readOnly = true)
    public List<ProveedoresResponseDTO> obtenerListaProveedores() {
        return proveedoresRepository.findAll().stream()
                // Map each Proveedores entity to ProveedoresResponseDTO
                .map(proveedor -> modelMapper.map(proveedor, ProveedoresResponseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * READ (Single) - Returns a single Provider Response DTO by ID.
     */
    @Transactional(readOnly = true)
    public ProveedoresResponseDTO obtenerProveedorId(Integer id) { // Changed parameter name for clarity
        Proveedores proveedor = proveedoresRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + id)); // Replace with custom exception if preferred
        // Map the found entity to the Response DTO
        return modelMapper.map(proveedor, ProveedoresResponseDTO.class);
    }

    /**
     * CREATE - Creates a new Provider from a Request DTO.
     * Renamed from guardarProveedor for clarity.
     */
    @Transactional
    public ProveedoresResponseDTO crearProveedor(ProveedoresRequestDTO dto) {
        // 1. Map Request DTO -> Proveedores Entity
        Proveedores proveedor = modelMapper.map(dto, Proveedores.class);

        // 2. Save the new entity
        Proveedores savedProveedor = proveedoresRepository.save(proveedor);

        // 3. Map the saved Entity -> Response DTO
        return modelMapper.map(savedProveedor, ProveedoresResponseDTO.class);
    }

    /**
     * UPDATE - Updates an existing Provider using a Request DTO.
     * Now accepts DTO instead of Map.
     */
    @Transactional
    public ProveedoresResponseDTO actualizarProveedor(Integer id, ProveedoresRequestDTO dto) {

        // 1. Fetch the existing provider entity
        Proveedores proveedorExistente = proveedoresRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + id));

        // 2. Map fields from DTO onto the existing entity
        // ModelMapper handles copying non-null fields. Ensure ID is not overwritten.
        // Configure ModelMapper globally or specifically to skip nulls if partial updates are needed.
        // Also ensure ModelMapper is configured to skip the ID field for updates.
        modelMapper.map(dto, proveedorExistente);
        // Manually reset ID just in case mapper touched it (safer if skip config isn't guaranteed)
        proveedorExistente.setProveedor_id(id);

        // 3. Save the updated entity
        Proveedores proveedorActualizado = proveedoresRepository.save(proveedorExistente);

        // 4. Map the updated entity to the Response DTO
        return modelMapper.map(proveedorActualizado, ProveedoresResponseDTO.class);
    }

    /**
     * DELETE - Deletes a Provider by ID.
     * No changes needed here as it only uses the ID. Returns void.
     */
    @Transactional
    public void borrarProveedor(Integer id) { // Changed return type to void
        if (!proveedoresRepository.existsById(id)) {
            throw new RuntimeException("Proveedor no encontrado con ID: " + id);
        }
        // Be aware of Foreign Key constraints. If Productos reference this provider,
        // deletion might fail unless FK allows NULL or CascadeType is set appropriately.
        proveedoresRepository.deleteById(id);
    }
}