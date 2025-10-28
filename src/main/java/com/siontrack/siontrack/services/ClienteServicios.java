package com.siontrack.siontrack.services;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siontrack.siontrack.DTO.Request.ClienteRequestDTO;
import com.siontrack.siontrack.DTO.Request.CorreosRequestDTO;
import com.siontrack.siontrack.DTO.Response.ClienteResponseDTO;
import com.siontrack.siontrack.models.Cliente_Correos;
import com.siontrack.siontrack.models.Cliente_Telefonos;
import com.siontrack.siontrack.models.Clientes;
import com.siontrack.siontrack.models.Vehiculos;
import com.siontrack.siontrack.repository.ClienteRepository;
import com.siontrack.siontrack.repository.TelefonosRepository;
import com.siontrack.siontrack.repository.VehiculosRepository;


@Service
public class ClienteServicios {

    @Autowired private ModelMapper modelMapper; 

    @Autowired
    private ClienteRepository clienteRepository;
    // Repositorios hijos (necesarios para guardar)
    @Autowired private TelefonosRepository telefonoRepository; 
    @Autowired private VehiculosRepository vehiculoRepository;
    // ... otros repositorios hijos ...

    // --- 1. CREAR CLIENTE (Simplified) ---
    @Transactional
    public ClienteResponseDTO crearCliente(ClienteRequestDTO dto) {
        
        // 1. Map RequestDTO -> Entity (ModelMapper handles simple fields)
        Clientes cliente = modelMapper.map(dto, Clientes.class);

        // 2. IMPORTANT: Manual Handling for Nested Lists (Mapping doesn't cascade automatically)
        // ModelMapper won't automatically create child entities. You still need this logic.
        
        // Save client first to get ID
        cliente = clienteRepository.save(cliente); 

        // Map and save Teléfonos
        if (dto.getTelefonos() != null) {
            final Clientes savedCliente = cliente; // Final variable for lambda
            List<Cliente_Telefonos> telefonos = dto.getTelefonos().stream()
                .map(tDto -> {
                    Cliente_Telefonos telefono = modelMapper.map(tDto, Cliente_Telefonos.class);
                    telefono.setClientes(savedCliente); // Set the parent link
                    return telefono;
                })
                .collect(Collectors.toList());
            telefonoRepository.saveAll(telefonos);
            cliente.setTelefonos(telefonos); // Update in-memory object
        }
        
        // Map and save Vehículos (similar logic)
        if (dto.getVehiculos() != null) {
             final Clientes savedCliente = cliente;
             List<Vehiculos> vehiculos = dto.getVehiculos().stream()
                .map(vDto -> {
                    Vehiculos vehiculo = modelMapper.map(vDto, Vehiculos.class);
                    vehiculo.setClientes(savedCliente);
                    return vehiculo;
                })
                .collect(Collectors.toList());
             vehiculoRepository.saveAll(vehiculos);
             cliente.setVehiculos(vehiculos);
        }
        
        // ... Repeat for Correos and Direcciones ...

        // 3. Map Entity -> ResponseDTO (ModelMapper handles this well)
        return modelMapper.map(cliente, ClienteResponseDTO.class);
    }

    // --- 2. OBTENER CLIENTE POR ID (Simplified) ---
    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerClientePorId(Integer id) {
        Clientes cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
            
        // One line replaces the entire constructor mapping logic!
        return modelMapper.map(cliente, ClienteResponseDTO.class); 
    }

    // --- 3. OBTENER LISTA DE CLIENTES (Simplified) ---
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> obtenerListaClientes() {
        return clienteRepository.findAll().stream()
            // Map each entity in the list to its DTO
            .map(cliente -> modelMapper.map(cliente, ClienteResponseDTO.class)) 
            .collect(Collectors.toList());
    }

   @Transactional
    public ClienteResponseDTO actualizarCliente(Integer clienteId, ClienteRequestDTO dto) {

        // 1. Obtener la entidad existente
        Clientes clienteExistente = clienteRepository.findById(clienteId)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + clienteId));

        // 2. Mapeo de Campos Simples con ModelMapper
        // ModelMapper ignora los campos que no coinciden (como las listas).
        // NOTA: Para campos opcionales, ModelMapper podría necesitar configuración o chequeos manuales.
        
        // Mapea y sobrescribe campos como nombre y tipo_cliente
        modelMapper.map(dto, clienteExistente);
        
        // El chequeo de 'cedula_ruc' se mantiene manualmente por seguridad/reglas específicas
        String nuevaCedulaRuc = dto.getCedula_ruc();
        if (nuevaCedulaRuc != null && !nuevaCedulaRuc.equals(clienteExistente.getCedula_ruc())) {
            clienteExistente.setCedula_ruc(nuevaCedulaRuc);
        }

        clienteExistente.setFecha_modificacion(LocalDate.now());

        // 3. Gestión de la Colección Anidada (Correos)
        
        // La lista entrante es de DTOs, la lista existente es de Entidades
        List<CorreosRequestDTO> nuevosCorreosDto = dto.getCorreos();
        List<Cliente_Correos> correosExistentes = clienteExistente.getCorreos();

        if (nuevosCorreosDto != null) {
            
            // a) Determinar qué correos existen y deben ser eliminados (orphanRemoval manual)
            // Se usa el nombre de campo "correo" del DTO para la comparación
            correosExistentes.removeIf(correoExistente -> nuevosCorreosDto.stream()
                .noneMatch(nuevoCorreoDto -> nuevoCorreoDto.getCorreo().equals(correoExistente.getCorreo())));

            // b) Determinar qué correos son nuevos y deben ser agregados
            for (CorreosRequestDTO nuevoCorreoDto : nuevosCorreosDto) {
                // Si el correo no existe en la lista de la BD
                if (correosExistentes.stream()
                        .noneMatch(correoExistente -> correoExistente.getCorreo().equals(nuevoCorreoDto.getCorreo()))) {

                    // 1. Mapear el DTO a la Entidad hija
                    Cliente_Correos nuevoCorreoEntidad = modelMapper.map(nuevoCorreoDto, Cliente_Correos.class);

                    // 2. Vincular con el padre y la colección
                    nuevoCorreoEntidad.setClientes(clienteExistente);
                    correosExistentes.add(nuevoCorreoEntidad);
                }
            }
            
            // NOTA: Con la configuración de Cascade, los cambios en 'correosExistentes' se guardarán.
            // Es buena práctica usar 'orphanRemoval=true' en la entidad padre si se quiere eliminar 
            // los elementos al removerlos de la colección. Aquí confiamos en la colección.

        } else {
            // c) Si el JSON no envía la lista, se eliminan todos los correos existentes
            correosExistentes.clear();
        }

        // 4. Guardar la entidad (Persiste los cambios en el padre y los hijos por Cascade)
        Clientes clienteActualizado = clienteRepository.save(clienteExistente);

        // 5. Mapear la Entidad resultante al Response DTO y devolver
        return modelMapper.map(clienteActualizado, ClienteResponseDTO.class);
    }

    @Transactional
    public void deleteCliente(Integer id) {
        // Check if client exists before deleting to avoid errors (optional, deleteById is safe)
        if (!clienteRepository.existsById(id)) {
            throw new RuntimeException("Cliente no encontrado con ID: " + id);
            // Or just return without error if deleting a non-existent ID is acceptable
        }
        clienteRepository.deleteById(id);
        // Cascading delete for related entities (Telefonos, Vehiculos, etc.)
        // will happen automatically if configured in the Clientes entity
        // (e.g., cascade = CascadeType.ALL, orphanRemoval = true)
    }
   
}
