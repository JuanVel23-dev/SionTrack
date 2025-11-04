package com.siontrack.siontrack.services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siontrack.siontrack.DTO.Request.ClienteRequestDTO;
import com.siontrack.siontrack.DTO.Request.CorreosRequestDTO;
import com.siontrack.siontrack.DTO.Request.DireccionesRequestDTO;
import com.siontrack.siontrack.DTO.Request.TelefonosRequestDTO;
import com.siontrack.siontrack.DTO.Response.ClienteResponseDTO;
import com.siontrack.siontrack.models.Cliente_Correos;
import com.siontrack.siontrack.models.Cliente_Direcciones;
import com.siontrack.siontrack.models.Cliente_Telefonos;
import com.siontrack.siontrack.models.Clientes;
import com.siontrack.siontrack.models.Vehiculos;
import com.siontrack.siontrack.repository.*;

@Service
public class ClienteServicios {

    private final DireccionesRepository direccionesRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private TelefonosRepository telefonoRepository;
    @Autowired
    private VehiculosRepository vehiculoRepository;

    @Autowired
    private CorreosRepository correosRepository;

    ClienteServicios(DireccionesRepository direccionesRepository) {
        this.direccionesRepository = direccionesRepository;
    }

    @Transactional
    public ClienteResponseDTO crearCliente(ClienteRequestDTO dto) {

        Clientes cliente = new Clientes();
        cliente.setNombre(dto.getNombre());
        cliente.setCedula_ruc(dto.getCedula_ruc());
        cliente.setTipo_cliente(dto.getTipo_cliente());

        cliente = clienteRepository.save(cliente);

        if (dto.getTelefonos() != null) {
            final Clientes savedCliente = cliente;
            List<Cliente_Telefonos> telefonos = dto.getTelefonos().stream()
                    .map(tDto -> {
                        Cliente_Telefonos telefono = modelMapper.map(tDto, Cliente_Telefonos.class);
                        telefono.setClientes(savedCliente); // Vincula al padre (que ya tiene ID)
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
                        vehiculo.setClientes(savedCliente); // Vincula al padre (que ya tiene ID)
                        return vehiculo;
                    })
                    .collect(Collectors.toList());

            vehiculoRepository.saveAll(vehiculos);
            cliente.setVehiculos(vehiculos);
        }

        if (dto.getCorreos() != null) {
            final Clientes savedCliente = cliente;
            List<Cliente_Correos> correos = dto.getCorreos().stream()
                    .map(correoDTO -> {
                        Cliente_Correos correo = modelMapper.map(correoDTO, Cliente_Correos.class);
                        correo.setClientes(savedCliente);
                        return correo;
                    })
                    .collect(Collectors.toList());

            correosRepository.saveAll(correos);
            cliente.setCorreos(correos);
        }

        if (dto.getDirecciones() != null) {
            final Clientes savedCliente = cliente;
            List<Cliente_Direcciones> direcciones = dto.getDirecciones().stream()
                    .map(direccionDTO -> {
                        Cliente_Direcciones direccion = modelMapper.map(direccionDTO, Cliente_Direcciones.class);
                        direccion.setClientes(savedCliente);
                        return direccion;
                    })
                    .collect(Collectors.toList());

            direccionesRepository.saveAll(direcciones);
            cliente.setDirecciones(direcciones);
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

            if (clienteRepository.existsByCedula_ruc(nuevaCedulaRuc)) {
                throw new RuntimeException(
                        "Error: La cédula/RUC '" + nuevaCedulaRuc + "' ya pertenece a otro cliente.");
            }

            clienteExistente.setCedula_ruc(nuevaCedulaRuc);
        }
        
        List<CorreosRequestDTO> nuevosCorreosDto = dto.getCorreos();
        List<Cliente_Correos> correosExistentes = clienteExistente.getCorreos();

        if (correosExistentes == null) {
            correosExistentes = new ArrayList<>();
            clienteExistente.setCorreos(correosExistentes);
        }

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

        List<TelefonosRequestDTO> nuevosTelefonosDto = dto.getTelefonos(); 
        List<Cliente_Telefonos> telefonosExistentes = clienteExistente.getTelefonos();

        if (telefonosExistentes == null) {
            telefonosExistentes = new ArrayList<>();
            clienteExistente.setTelefonos(telefonosExistentes);
        }

        if (nuevosTelefonosDto != null) {
            telefonosExistentes.removeIf(telefonoExistente -> nuevosTelefonosDto.stream()
                    .noneMatch(nuevoTelefonoDto -> nuevoTelefonoDto.getTelefono()
                            .equals(telefonoExistente.getTelefono())));

            for (TelefonosRequestDTO nuevoTelefonoDto : nuevosTelefonosDto) { 
                if (telefonosExistentes.stream()
                        .noneMatch(telefonoExistente -> telefonoExistente.getTelefono()
                                .equals(nuevoTelefonoDto.getTelefono()))) {
                    Cliente_Telefonos nuevoTelefonoEntidad = modelMapper.map(nuevoTelefonoDto, Cliente_Telefonos.class);
                    nuevoTelefonoEntidad.setClientes(clienteExistente);
                    telefonosExistentes.add(nuevoTelefonoEntidad);
                }
            }
        } else {
            telefonosExistentes.clear();
        }

        List<DireccionesRequestDTO> nuevasDireccionesDto = dto.getDirecciones(); 
        List<Cliente_Direcciones> direccionesExistentes = clienteExistente.getDirecciones();

        if (direccionesExistentes == null) {
            direccionesExistentes = new ArrayList<>();
            clienteExistente.setDirecciones(direccionesExistentes);
        }

        if (nuevasDireccionesDto != null) {
            direccionesExistentes.removeIf(direccionExistente -> nuevasDireccionesDto.stream()
                    .noneMatch(nuevaDireccionDto -> nuevaDireccionDto.getDireccion()
                            .equals(direccionExistente.getDireccion())));

            for (DireccionesRequestDTO nuevaDireccionDto : nuevasDireccionesDto) { 
                if (direccionesExistentes.stream()
                        .noneMatch(direccionExistente -> direccionExistente.getDireccion()
                                .equals(nuevaDireccionDto.getDireccion()))) {
                    Cliente_Direcciones nuevaDireccionEntidad = modelMapper.map(nuevaDireccionDto,
                            Cliente_Direcciones.class);
                    nuevaDireccionEntidad.setClientes(clienteExistente);
                    direccionesExistentes.add(nuevaDireccionEntidad);
                }
            }
        } else {
            direccionesExistentes.clear();
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
