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
import com.siontrack.siontrack.models.enums.ResultadoEnvioMensaje;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private WhatsAppService whatsAppService;
    @Autowired
    private CorreosRepository correosRepository;

    private static final Logger log = LoggerFactory.getLogger(ClienteServicios.class);

    ClienteServicios(DireccionesRepository direccionesRepository) {
        this.direccionesRepository = direccionesRepository;
    }

    private String limpiarTelefono(String telefono) {
        if (telefono == null) return null;
        return telefono.replaceAll("[^0-9]", "");
    }

    /**
     * Mapea Clientes → ClienteResponseDTO con resolución manual de recibe_notificaciones.
     * ModelMapper STRICT no puede mapear getRecibeNotificaciones() → setRecibe_notificaciones()
     * porque los nombres no coinciden (camelCase vs snake_case).
     */
    private ClienteResponseDTO mapearClienteADTO(Clientes cliente) {
        ClienteResponseDTO dto = modelMapper.map(cliente, ClienteResponseDTO.class);
        dto.setRecibe_notificaciones(Boolean.TRUE.equals(cliente.getRecibeNotificaciones()));
        return dto;
    }

    @Transactional
    public ClienteResponseDTO crearCliente(ClienteRequestDTO dto) {

        Clientes cliente = new Clientes();
        cliente.setNombre(dto.getNombre());
        cliente.setCedula_ruc(dto.getCedula_ruc());
        cliente.setTipo_cliente(dto.getTipo_cliente());
        cliente.setRecibe_notificaciones(false);
        cliente.setConsentimientoProcesado(false);

        cliente = clienteRepository.save(cliente);

        if (dto.getTelefonos() != null && !dto.getTelefonos().isEmpty()) {
            final Clientes savedCliente = cliente;
            List<Cliente_Telefonos> telefonos = dto.getTelefonos().stream()
                    .map(tDto -> {
                        Cliente_Telefonos telefono = modelMapper.map(tDto, Cliente_Telefonos.class);
                        telefono.setTelefono(limpiarTelefono(telefono.getTelefono()));
                        telefono.setClientes(savedCliente);
                        return telefono;
                    })
                    .collect(Collectors.toList());

            telefonoRepository.saveAll(telefonos);
            cliente.setTelefonos(telefonos);

            try {
                String numeroPrincipal = telefonos.get(0).getTelefono();

                ResultadoEnvioMensaje resultado = whatsAppService
                        .enviarSolicitudConsentimiento(numeroPrincipal, cliente.getNombre());

                switch (resultado) {
                    case ENVIADO -> log.info("✅ WhatsApp enviado a {}", cliente.getNombre());
                    case SIN_WHATSAPP -> {
                        log.warn("📵 {} no tiene WhatsApp", numeroPrincipal);
                        telefonoRepository.save(telefonos.get(0));
                    }
                    case NUMERO_INVALIDO -> {
                        log.warn("🚫 Número inválido: {}", numeroPrincipal);
                        telefonoRepository.save(telefonos.get(0));
                    }
                    default -> log.warn("⚠️ No se pudo enviar WhatsApp: {}", resultado);
                }

            } catch (Exception e) {
                log.error("⚠️ Cliente creado, pero falló el envío de WhatsApp: {}", e.getMessage());
            }
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

        return mapearClienteADTO(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerClientePorId(Integer id) {
        Clientes cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        return mapearClienteADTO(cliente);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> obtenerListaClientes() {
        return clienteRepository.findAll().stream()
                .map(this::mapearClienteADTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClienteResponseDTO actualizarCliente(Integer clienteId, ClienteRequestDTO dto) {

        Clientes clienteExistente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + clienteId));

        modelMapper.map(dto, clienteExistente);

        // Manejar recibe_notificaciones manualmente
        if (dto.getRecibe_notificaciones() != null) {
            clienteExistente.setRecibe_notificaciones(dto.getRecibe_notificaciones());
        } else {
            clienteExistente.setRecibe_notificaciones(false);
        }

        String nuevaCedulaRuc = dto.getCedula_ruc();
        if (nuevaCedulaRuc != null && !nuevaCedulaRuc.equals(clienteExistente.getCedula_ruc())) {
            if (clienteRepository.existsByCedula_ruc(nuevaCedulaRuc)) {
                throw new RuntimeException(
                        "Error: La cédula/RUC '" + nuevaCedulaRuc + "' ya pertenece a otro cliente.");
            }
            clienteExistente.setCedula_ruc(nuevaCedulaRuc);
        }

        // CORREOS
        List<Cliente_Correos> correosExistentes = clienteExistente.getCorreos();
        List<CorreosRequestDTO> nuevosCorreosDto = (dto.getCorreos() == null) ? new ArrayList<>()
                : dto.getCorreos().stream()
                        .filter(c -> c.getCorreo() != null && !c.getCorreo().trim().isEmpty())
                        .collect(Collectors.toList());
        if (correosExistentes == null) correosExistentes = new ArrayList<>();

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

        // TELÉFONOS
        List<Cliente_Telefonos> telefonosExistentes = clienteExistente.getTelefonos();
        List<TelefonosRequestDTO> nuevosTelefonosDto = (dto.getTelefonos() == null) ? new ArrayList<>()
                : dto.getTelefonos().stream()
                        .filter(t -> t.getTelefono() != null && !t.getTelefono().trim().isEmpty())
                        .collect(Collectors.toList());
        if (telefonosExistentes == null) telefonosExistentes = new ArrayList<>();

        nuevosTelefonosDto.forEach(t -> t.setTelefono(limpiarTelefono(t.getTelefono())));

        telefonosExistentes.removeIf(telefonoExistente -> nuevosTelefonosDto.stream()
                .noneMatch(nuevoTelefonoDto -> nuevoTelefonoDto.getTelefono().equals(telefonoExistente.getTelefono())));

        for (TelefonosRequestDTO nuevoTelefonoDto : nuevosTelefonosDto) {
            if (telefonosExistentes.stream()
                    .noneMatch(telefonoExistente -> telefonoExistente.getTelefono()
                            .equals(nuevoTelefonoDto.getTelefono()))) {
                Cliente_Telefonos nuevoTelefonoEntidad = modelMapper.map(nuevoTelefonoDto, Cliente_Telefonos.class);
                nuevoTelefonoEntidad.setClientes(clienteExistente);
                telefonosExistentes.add(nuevoTelefonoEntidad);
            }
        }

        // DIRECCIONES
        List<Cliente_Direcciones> direccionesExistentes = clienteExistente.getDirecciones();
        List<DireccionesRequestDTO> nuevasDireccionesDto = (dto.getDirecciones() == null) ? new ArrayList<>()
                : dto.getDirecciones().stream()
                        .filter(d -> d.getDireccion() != null && !d.getDireccion().trim().isEmpty())
                        .collect(Collectors.toList());
        if (direccionesExistentes == null) direccionesExistentes = new ArrayList<>();

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

        Clientes clienteActualizado = clienteRepository.save(clienteExistente);
        return mapearClienteADTO(clienteActualizado);
    }

    @Transactional
    public void deleteCliente(Integer id) {
        if (!clienteRepository.existsById(id)) {
            throw new RuntimeException("Cliente no encontrado con ID: " + id);
        }
        clienteRepository.deleteById(id);
    }

    @Transactional
    public void crearVehiculoParaCliente(VehiculosRequestDTO dto, Integer clienteId) {
        Clientes clienteExistente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + clienteId));
        Vehiculos nuevoVehiculo = modelMapper.map(dto, Vehiculos.class);
        nuevoVehiculo.setClientes(clienteExistente);
        vehiculoRepository.save(nuevoVehiculo);
    }
}