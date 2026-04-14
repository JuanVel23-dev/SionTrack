package com.siontrack.siontrack.services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

/**
 * Gestiona el ciclo de vida de los clientes y sus datos de contacto asociados:
 * teléfonos, correos, direcciones y vehículos.
 *
 * <p>Al crear un cliente con teléfono, se envía automáticamente la solicitud de
 * consentimiento por WhatsApp. El resultado del envío se registra en el log pero
 * no bloquea la creación del cliente si el servicio de mensajería falla.
 *
 * <p>La actualización de colecciones de contacto (teléfonos, correos, direcciones)
 * usa una estrategia de sincronización: se eliminan los que ya no están en el DTO
 * y se agregan los nuevos, sin modificar los que coinciden.
 */
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

    /**
     * Elimina todos los caracteres no numéricos de un número de teléfono.
     * Ejemplo: {@code "+57 318-325 2987"} → {@code "573183252987"}.
     */
    private String limpiarTelefono(String telefono) {
        if (telefono == null) return null;
        return telefono.replaceAll("[^0-9]", "");
    }

    /**
     * Mapea {@link Clientes} → {@link ClienteResponseDTO} con resolución manual del campo
     * {@code recibe_notificaciones}.
     *
     * <p>ModelMapper en modo STRICT no puede mapear {@code getRecibeNotificaciones()} →
     * {@code setRecibe_notificaciones()} porque los nombres no coinciden
     * (camelCase vs snake_case), por lo que la asignación se hace manualmente.
     */
    private ClienteResponseDTO mapearClienteADTO(Clientes cliente) {
        ClienteResponseDTO dto = modelMapper.map(cliente, ClienteResponseDTO.class);
        dto.setRecibe_notificaciones(Boolean.TRUE.equals(cliente.getRecibeNotificaciones()));
        return dto;
    }

    /**
     * Crea un nuevo cliente con todos sus datos de contacto.
     *
     * <p>Si el DTO incluye al menos un teléfono, se intenta enviar la solicitud de
     * consentimiento por WhatsApp al primer número de la lista. El resultado se registra
     * en el log; cualquier fallo en el envío se ignora para no afectar la creación.
     *
     * @param dto datos del cliente a crear
     * @return DTO del cliente creado con su ID generado
     */
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
                    case ENVIADO       -> log.info("WhatsApp enviado a {}", cliente.getNombre());
                    case SIN_WHATSAPP  -> {
                        log.warn("{} no tiene WhatsApp", numeroPrincipal);
                        telefonoRepository.save(telefonos.get(0));
                    }
                    case NUMERO_INVALIDO -> {
                        log.warn("Número inválido: {}", numeroPrincipal);
                        telefonoRepository.save(telefonos.get(0));
                    }
                    default -> log.warn("No se pudo enviar WhatsApp: {}", resultado);
                }

            } catch (Exception e) {
                log.error("Cliente creado, pero falló el envío de WhatsApp: {}", e.getMessage());
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

    /**
     * Obtiene un cliente por su ID.
     *
     * @param id ID del cliente
     * @return DTO con los datos completos del cliente
     * @throws RuntimeException si el cliente no existe
     */
    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerClientePorId(Integer id) {
        Clientes cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        return mapearClienteADTO(cliente);
    }

    /**
     * Devuelve la lista completa de clientes sin paginación.
     *
     * @return lista de todos los clientes mapeados a DTO
     */
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> obtenerListaClientes() {
        return clienteRepository.findAll().stream()
                .map(this::mapearClienteADTO)
                .collect(Collectors.toList());
    }

    /**
     * Devuelve la lista de clientes paginada, con búsqueda opcional por nombre o cédula.
     *
     * @param pageable  configuración de paginación y orden
     * @param busqueda  término de búsqueda (puede ser nulo o vacío para listar todos)
     * @return página de clientes mapeados a DTO
     */
    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> obtenerListaClientesPaginado(Pageable pageable, String busqueda) {
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            return clienteRepository.buscarPaginado(busqueda.trim(), pageable)
                    .map(this::mapearClienteADTO);
        }
        return clienteRepository.findAllOrderByIdDesc(pageable)
                .map(this::mapearClienteADTO);
    }

    /**
     * Actualiza los datos de un cliente existente.
     *
     * <p>La sincronización de colecciones (correos, teléfonos, direcciones) elimina
     * los registros que ya no están en el DTO y agrega los nuevos, sin afectar los
     * que coinciden por valor. Las colecciones nulas en el DTO se interpretan como
     * "limpiar la colección".
     *
     * @param clienteId ID del cliente a actualizar
     * @param dto       datos nuevos del cliente
     * @return DTO del cliente actualizado
     * @throws RuntimeException si el cliente no existe o si la cédula/RUC ya pertenece a otro
     */
    @Transactional
    public ClienteResponseDTO actualizarCliente(Integer clienteId, ClienteRequestDTO dto) {

        Clientes clienteExistente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + clienteId));

        modelMapper.map(dto, clienteExistente);

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

    /**
     * Elimina un cliente y sus datos asociados.
     *
     * @param id ID del cliente a eliminar
     * @throws RuntimeException si el cliente no existe
     */
    @Transactional
    public void deleteCliente(Integer id) {
        if (!clienteRepository.existsById(id)) {
            throw new RuntimeException("Cliente no encontrado con ID: " + id);
        }
        clienteRepository.deleteById(id);
    }

    /**
     * Agrega un nuevo vehículo a un cliente existente.
     *
     * @param dto       datos del vehículo a crear
     * @param clienteId ID del cliente al que se asociará el vehículo
     * @throws RuntimeException si el cliente no existe
     */
    @Transactional
    public void crearVehiculoParaCliente(VehiculosRequestDTO dto, Integer clienteId) {
        Clientes clienteExistente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + clienteId));
        Vehiculos nuevoVehiculo = modelMapper.map(dto, Vehiculos.class);
        nuevoVehiculo.setClientes(clienteExistente);
        vehiculoRepository.save(nuevoVehiculo);
    }
}
