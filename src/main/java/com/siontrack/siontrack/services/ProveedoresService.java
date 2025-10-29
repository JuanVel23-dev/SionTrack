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
        Proveedores proveedor = modelMapper.map(dto, Proveedores.class);

        Proveedores savedProveedor = proveedoresRepository.save(proveedor);

        return modelMapper.map(savedProveedor, ProveedoresResponseDTO.class);
    }

    @Transactional
    public ProveedoresResponseDTO actualizarProveedor(Integer id, ProveedoresRequestDTO dto) {

        Proveedores proveedorExistente = proveedoresRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + id));

        modelMapper.map(dto, proveedorExistente);
        proveedorExistente.setProveedor_id(id);

        Proveedores proveedorActualizado = proveedoresRepository.save(proveedorExistente);

        return modelMapper.map(proveedorActualizado, ProveedoresResponseDTO.class);
    }

    @Transactional
    public void borrarProveedor(Integer id) {
        if (!proveedoresRepository.existsById(id)) {
            throw new RuntimeException("Proveedor no encontrado con ID: " + id);
        }
        proveedoresRepository.deleteById(id);
    }
}