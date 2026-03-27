package com.siontrack.siontrack.services;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siontrack.siontrack.DTO.Request.ProveedoresRequestDTO;
import com.siontrack.siontrack.DTO.Response.ProveedoresResponseDTO;

import com.siontrack.siontrack.models.Proveedores;
import com.siontrack.siontrack.repository.ProveedoresRepository;

@Service
public class ProveedoresService {

    @Autowired
    private ProveedoresRepository proveedoresRepository;

    @Autowired
    private ModelMapper modelMapper;

    /**
     * Limpia un teléfono para almacenarlo en BD.
     * Entrada: "+57 3183252987" → Salida: "573183252987"
     */
    private String limpiarTelefono(String telefono) {
        if (telefono == null) return null;
        return telefono.replaceAll("[^0-9]", "");
    }

    @Transactional(readOnly = true)
    public List<ProveedoresResponseDTO> obtenerListaProveedores() {
        return proveedoresRepository.findAll().stream()
                .map(proveedor -> modelMapper.map(proveedor, ProveedoresResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProveedoresResponseDTO obtenerProveedorId(Integer id) {
        Proveedores proveedor = proveedoresRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + id));
        return modelMapper.map(proveedor, ProveedoresResponseDTO.class);
    }

    @Transactional
    public ProveedoresResponseDTO crearProveedor(ProveedoresRequestDTO dto) {
        dto.setTelefono(limpiarTelefono(dto.getTelefono()));

        Proveedores proveedor = modelMapper.map(dto, Proveedores.class);

        Proveedores savedProveedor = proveedoresRepository.save(proveedor);

        return modelMapper.map(savedProveedor, ProveedoresResponseDTO.class);
    }

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

    @Transactional(readOnly = true)
    public Integer buscarIdPorNombre(String nombre) {
        return proveedoresRepository.findByNombreIgnoreCase(nombre)
                .map(Proveedores::getProveedor_id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con nombre: " + nombre));
    }

    @Transactional
    public void borrarProveedor(Integer id) {
        if (!proveedoresRepository.existsById(id)) {
            throw new RuntimeException("Proveedor no encontrado con ID: " + id);
        }
        proveedoresRepository.deleteById(id);
    }
}