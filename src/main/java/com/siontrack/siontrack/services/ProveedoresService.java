package com.siontrack.siontrack.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.siontrack.siontrack.models.Proveedores;
import com.siontrack.siontrack.repository.ProveedoresRepository;

import jakarta.transaction.Transactional;

@Service
public class ProveedoresService {

    @Autowired
    private ProveedoresRepository proveedoresRepository;

    public List<Proveedores> obtenerListaProveedores(){
        return proveedoresRepository.findAll();
    }

    public Optional<Proveedores> obtenerProveedorId(int id){
        return proveedoresRepository.findById(id);
    }

     public Proveedores guardarProveedor(Proveedores proveedor) {
        return proveedoresRepository.save(proveedor);
    }

    public void borrarProveedor(int id) {
        proveedoresRepository.deleteById(id);
    }

    @Transactional
    public Proveedores actualizarProveedor(Integer id, Map<String, Object> payload) {
        
        // 1. Buscar el proveedor existente
        Proveedores proveedor = proveedoresRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + id));

        // 2. Actualizar campos si están presentes en el JSON (Map)
        // Usamos .containsKey() para solo actualizar los campos enviados
        
        if (payload.containsKey("nombre")) {
            proveedor.setNombre((String) payload.get("nombre"));
        }
        if (payload.containsKey("telefono")) {
            proveedor.setTelefono((String) payload.get("telefono"));
        }
        if (payload.containsKey("email")) {
            proveedor.setEmail((String) payload.get("email"));
        }
        if (payload.containsKey("direccion")) {
            proveedor.setDireccion((String) payload.get("direccion"));
        }
        if (payload.containsKey("nombre_contacto")) {
            proveedor.setNombre_contacto((String) payload.get("nombre_contacto"));
        }

        // 3. Guardar los cambios
        // Como 'proveedor' es una entidad gestionada por JPA, 
        // save() actualizará la fila existente.
        return proveedoresRepository.save(proveedor);
    }
}
