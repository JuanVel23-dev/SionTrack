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
import com.siontrack.siontrack.DTO.Request.VehiculosRequestDTO;
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
        
        List<Cliente_Correos> correosExistentes = clienteExistente.getCorreos();
    // CORRECCIÓN: Filtrar DTOs nulos o vacíos ANTES de procesar
    List<CorreosRequestDTO> nuevosCorreosDto = (dto.getCorreos() == null) ? new ArrayList<>() :
            dto.getCorreos().stream()
               .filter(c -> c.getCorreo() != null && !c.getCorreo().trim().isEmpty())
               .collect(Collectors.toList());

    if (correosExistentes == null) correosExistentes = new ArrayList<>();

    // a) Eliminar correos
    correosExistentes.removeIf(correoExistente -> nuevosCorreosDto.stream()
            .noneMatch(nuevoCorreoDto -> nuevoCorreoDto.getCorreo().equals(correoExistente.getCorreo())));

    // b) Añadir correos
    for (CorreosRequestDTO nuevoCorreoDto : nuevosCorreosDto) {
        if (correosExistentes.stream()
                .noneMatch(correoExistente -> correoExistente.getCorreo().equals(nuevoCorreoDto.getCorreo()))) {
            Cliente_Correos nuevoCorreoEntidad = modelMapper.map(nuevoCorreoDto, Cliente_Correos.class);
            nuevoCorreoEntidad.setClientes(clienteExistente);
            correosExistentes.add(nuevoCorreoEntidad);
        }
    }
    
    // 6. GESTIÓN DE TELÉFONOS
    List<Cliente_Telefonos> telefonosExistentes = clienteExistente.getTelefonos();
    // CORRECCIÓN: Filtrar DTOs nulos o vacíos ANTES de procesar
    List<TelefonosRequestDTO> nuevosTelefonosDto = (dto.getTelefonos() == null) ? new ArrayList<>() :
            dto.getTelefonos().stream()
               .filter(t -> t.getTelefono() != null && !t.getTelefono().trim().isEmpty())
               .collect(Collectors.toList());

    if (telefonosExistentes == null) telefonosExistentes = new ArrayList<>();

    // a) Eliminar teléfonos
    telefonosExistentes.removeIf(telefonoExistente -> nuevosTelefonosDto.stream()
            .noneMatch(nuevoTelefonoDto -> nuevoTelefonoDto.getTelefono().equals(telefonoExistente.getTelefono())));

    // b) Añadir teléfonos
    for (TelefonosRequestDTO nuevoTelefonoDto : nuevosTelefonosDto) {
        if (telefonosExistentes.stream()
                .noneMatch(telefonoExistente -> telefonoExistente.getTelefono().equals(nuevoTelefonoDto.getTelefono()))) {
            Cliente_Telefonos nuevoTelefonoEntidad = modelMapper.map(nuevoTelefonoDto, Cliente_Telefonos.class);
            nuevoTelefonoEntidad.setClientes(clienteExistente);
            telefonosExistentes.add(nuevoTelefonoEntidad);
        }
    }

    // 7. GESTIÓN DE DIRECCIONES
    List<Cliente_Direcciones> direccionesExistentes = clienteExistente.getDirecciones();
    // CORRECCIÓN: Filtrar DTOs nulos o vacíos ANTES de procesar
    List<DireccionesRequestDTO> nuevasDireccionesDto = (dto.getDirecciones() == null) ? new ArrayList<>() :
            dto.getDirecciones().stream()
               .filter(d -> d.getDireccion() != null && !d.getDireccion().trim().isEmpty())
               .collect(Collectors.toList());

    if (direccionesExistentes == null) direccionesExistentes = new ArrayList<>();
    
    // a) Eliminar direcciones
    direccionesExistentes.removeIf(direccionExistente -> nuevasDireccionesDto.stream()
            .noneMatch(nuevaDireccionDto -> nuevaDireccionDto.getDireccion().equals(direccionExistente.getDireccion())));

    // b) Añadir direcciones
    for (DireccionesRequestDTO nuevaDireccionDto : nuevasDireccionesDto) {
        if (direccionesExistentes.stream()
                .noneMatch(direccionExistente -> direccionExistente.getDireccion().equals(nuevaDireccionDto.getDireccion()))) {
            Cliente_Direcciones nuevaDireccionEntidad = modelMapper.map(nuevaDireccionDto, Cliente_Direcciones.class);
            nuevaDireccionEntidad.setClientes(clienteExistente);
            direccionesExistentes.add(nuevaDireccionEntidad);
        }
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

    // ... (justo después del método deleteCliente, dentro de la clase ClienteServicios)

    @Transactional
    public void crearVehiculoParaCliente(VehiculosRequestDTO dto, Integer clienteId) {
        
        // 1. Buscar al cliente que será el "padre" de este vehículo
        Clientes clienteExistente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + clienteId));

        // 2. Mapear los datos del formulario (DTO) a la entidad Vehiculo
        Vehiculos nuevoVehiculo = modelMapper.map(dto, Vehiculos.class);

        // 3. ¡El paso más importante! Establecer la relación
        nuevoVehiculo.setClientes(clienteExistente);
        
        // 4. Guardar la nueva entidad Vehiculo
        //    (JPA se encargará de asignar el 'cliente_id' automáticamente)
        vehiculoRepository.save(nuevoVehiculo);
    }
}
