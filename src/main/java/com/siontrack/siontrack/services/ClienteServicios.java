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
    @Autowired private TelefonosRepository telefonoRepository; 
    @Autowired private VehiculosRepository vehiculoRepository;

    @Transactional
    public ClienteResponseDTO crearCliente(ClienteRequestDTO dto) {
        Clientes cliente = modelMapper.map(dto, Clientes.class);
        cliente = clienteRepository.save(cliente); 

        if (dto.getTelefonos() != null) {
            final Clientes savedCliente = cliente; 
            List<Cliente_Telefonos> telefonos = dto.getTelefonos().stream()
                .map(tDto -> {
                    Cliente_Telefonos telefono = modelMapper.map(tDto, Cliente_Telefonos.class);
                    telefono.setClientes(savedCliente); 
                    return telefono;
                })
                .collect(Collectors.toList());
            telefonoRepository.saveAll(telefonos);
            cliente.setTelefonos(telefonos); 
        }
        
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
        return modelMapper.map(cliente, ClienteResponseDTO.class);
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerClientePorId(Integer id) {
        Clientes cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        return modelMapper.map(cliente, ClienteResponseDTO.class); 
    }

    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> obtenerListaClientes() {
        return clienteRepository.findAll().stream()
            .map(cliente -> modelMapper.map(cliente, ClienteResponseDTO.class)) 
            .collect(Collectors.toList());
    }

   @Transactional
    public ClienteResponseDTO actualizarCliente(Integer clienteId, ClienteRequestDTO dto) {

        Clientes clienteExistente = clienteRepository.findById(clienteId)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + clienteId));

        modelMapper.map(dto, clienteExistente);
        
        String nuevaCedulaRuc = dto.getCedula_ruc();
        if (nuevaCedulaRuc != null && !nuevaCedulaRuc.equals(clienteExistente.getCedula_ruc())) {
            clienteExistente.setCedula_ruc(nuevaCedulaRuc);
        }

        clienteExistente.setFecha_modificacion(LocalDate.now());

        List<CorreosRequestDTO> nuevosCorreosDto = dto.getCorreos();
        List<Cliente_Correos> correosExistentes = clienteExistente.getCorreos();

        if (nuevosCorreosDto != null) {
            correosExistentes.removeIf(correoExistente -> nuevosCorreosDto.stream()
                .noneMatch(nuevoCorreoDto -> nuevoCorreoDto.getCorreo().equals(correoExistente.getCorreo())));

            for (CorreosRequestDTO nuevoCorreoDto : nuevosCorreosDto) {
                if (correosExistentes.stream()
                        .noneMatch(correoExistente -> correoExistente.getCorreo().equals(nuevoCorreoDto.getCorreo()))) {
                    Cliente_Correos nuevoCorreoEntidad = modelMapper.map(nuevoCorreoDto, Cliente_Correos.class);
                    nuevoCorreoEntidad.setClientes(clienteExistente);
                    correosExistentes.add(nuevoCorreoEntidad);
                }
            }

        } else {
            correosExistentes.clear();
        }
        Clientes clienteActualizado = clienteRepository.save(clienteExistente);
        return modelMapper.map(clienteActualizado, ClienteResponseDTO.class);
    }

    @Transactional
    public void deleteCliente(Integer id) {
        if (!clienteRepository.existsById(id)) {
            throw new RuntimeException("Cliente no encontrado con ID: " + id);
        }
        clienteRepository.deleteById(id);
    }
   
}
