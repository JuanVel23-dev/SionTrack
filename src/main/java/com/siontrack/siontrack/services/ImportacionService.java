package com.siontrack.siontrack.services;

import com.siontrack.siontrack.DTO.Request.*;
import com.siontrack.siontrack.DTO.Response.ImportacionResponseDTO;
import com.siontrack.siontrack.models.Clientes;
import com.siontrack.siontrack.models.Productos;
import com.siontrack.siontrack.models.Vehiculos;
import com.siontrack.siontrack.repository.ClienteRepository;
import com.siontrack.siontrack.repository.ProductosRepository;
import com.siontrack.siontrack.repository.ProveedoresRepository;
import com.siontrack.siontrack.repository.ServiciosRepository;
import com.siontrack.siontrack.repository.VehiculosRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ImportacionService {

    private final ProveedoresRepository proveedoresRepository;

    private static final Logger log = LoggerFactory.getLogger(ImportacionService.class);

    private final ClienteServicios clienteServicios;
    private final ProductosServicios productosServicios;
    private final ProveedoresService proveedoresService;
    private final ServiciosService serviciosService;
    private final ClienteRepository clienteRepository;
    private final VehiculosRepository vehiculosRepository;
    private final ProductosRepository productosRepository;
    private final ServiciosRepository serviciosRepository;

    public ImportacionService(ClienteServicios clienteServicios,
            ProductosServicios productosServicios,
            ProveedoresService proveedoresService,
            ServiciosService serviciosService,
            ClienteRepository clienteRepository,
            VehiculosRepository vehiculosRepository,
            ProductosRepository productosRepository,
            ServiciosRepository serviciosRepository, ProveedoresRepository proveedoresRepository) {
        this.clienteServicios = clienteServicios;
        this.productosServicios = productosServicios;
        this.proveedoresService = proveedoresService;
        this.serviciosService = serviciosService;
        this.clienteRepository = clienteRepository;
        this.vehiculosRepository = vehiculosRepository;
        this.productosRepository = productosRepository;
        this.serviciosRepository = serviciosRepository;
        this.proveedoresRepository = proveedoresRepository;
    }

    // ==================== IMPORTAR CLIENTES ====================

    public ImportacionResponseDTO importarClientes(MultipartFile archivo) {
        ImportacionResponseDTO resultado = new ImportacionResponseDTO();
        resultado.setTipoImportacion("CLIENTES");

        List<Map<String, String>> filas;
        try {
            filas = leerFilas(archivo);
        } catch (Exception e) {
            log.error("Error leyendo archivo: {}", e.getMessage());
            resultado.agregarError(0, "Error leyendo archivo: " + e.getMessage());
            return resultado;
        }

        // Rastrear cédulas/nombres ya procesados en este archivo para ignorar
        // duplicados internos
        Set<String> procesadosEnArchivo = new HashSet<>();

        for (int i = 0; i < filas.size(); i++) {
            Map<String, String> fila = filas.get(i);
            int numeroFila = i + 2;
            resultado.setRegistrosProcesados(resultado.getRegistrosProcesados() + 1);

            try {
                ClienteRequestDTO dto = new ClienteRequestDTO();
                dto.setNombre(get(fila, "nombre"));
                dto.setCedula_ruc(get(fila, "cedula_ruc"));
                dto.setTipo_cliente(get(fila, "tipo_cliente"));

                if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
                    resultado.agregarError(numeroFila, "Nombre es requerido");
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                    continue;
                }

                // Clave de identificación para detectar duplicados dentro del archivo
                String claveArchivo = dto.getCedula_ruc() != null
                        ? dto.getCedula_ruc().trim().toLowerCase()
                        : dto.getNombre().trim().toLowerCase();

                if (procesadosEnArchivo.contains(claveArchivo)) {
                    resultado.agregarError(numeroFila, "Duplicado en el archivo: " + claveArchivo + " (omitido)");
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                    continue;
                }
                procesadosEnArchivo.add(claveArchivo);

                String telefono = get(fila, "telefono");
                if (telefono != null) {
                    TelefonosRequestDTO telDto = new TelefonosRequestDTO();
                    telDto.setTelefono(telefono);
                    dto.setTelefonos(List.of(telDto));
                }

                String correo = get(fila, "correo");
                if (correo != null) {
                    CorreosRequestDTO correoDto = new CorreosRequestDTO();
                    correoDto.setCorreo(correo);
                    dto.setCorreos(List.of(correoDto));
                }

                String direccion = get(fila, "direccion");
                if (direccion != null) {
                    DireccionesRequestDTO dirDto = new DireccionesRequestDTO();
                    dirDto.setDireccion(direccion);
                    dto.setDirecciones(List.of(dirDto));
                }

                String placa = get(fila, "placa");
                if (placa != null) {
                    VehiculosRequestDTO vehiculoDto = new VehiculosRequestDTO();
                    vehiculoDto.setPlaca(placa);
                    String km = get(fila, "kilometraje_actual");
                    if (km == null)
                        km = get(fila, "kilometraje");
                    if (km == null)
                        km = "0";
                    vehiculoDto.setKilometraje_actual(km);
                    dto.setVehiculos(List.of(vehiculoDto));
                }

                // Buscar si el cliente ya existe en BD (por cédula primero, luego por nombre)
                com.siontrack.siontrack.models.Clientes existente = null;
                if (dto.getCedula_ruc() != null) {
                    existente = clienteRepository.findByCedulaRuc(dto.getCedula_ruc()).orElse(null);
                }
                if (existente == null) {
                    existente = clienteRepository.findByNombreIgnoreCase(dto.getNombre()).orElse(null);
                }

                if (existente != null) {
                    // Actualizar cliente existente
                    clienteServicios.actualizarCliente(existente.getCliente_id(), dto);
                    resultado.setRegistrosActualizados(resultado.getRegistrosActualizados() + 1);
                } else {
                    // Crear cliente nuevo
                    clienteServicios.crearCliente(dto);
                    resultado.setRegistrosCreados(resultado.getRegistrosCreados() + 1);
                }
                resultado.setRegistrosExitosos(resultado.getRegistrosExitosos() + 1);

            } catch (Exception e) {
                resultado.agregarError(numeroFila, e.getMessage());
                resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
            }
        }

        return resultado;
    }

    // ==================== IMPORTAR PRODUCTOS ====================

    public ImportacionResponseDTO importarProductos(MultipartFile archivo) {
        ImportacionResponseDTO resultado = new ImportacionResponseDTO();
        resultado.setTipoImportacion("PRODUCTOS");

        List<Map<String, String>> filas;
        try {
            filas = leerFilas(archivo);
        } catch (Exception e) {
            log.error("Error leyendo archivo: {}", e.getMessage());
            resultado.agregarError(0, "Error leyendo archivo: " + e.getMessage());
            return resultado;
        }

        for (int i = 0; i < filas.size(); i++) {
            Map<String, String> fila = filas.get(i);
            int numeroFila = i + 2;
            resultado.setRegistrosProcesados(resultado.getRegistrosProcesados() + 1);

            try {
                ProductosRequestDTO dto = new ProductosRequestDTO();
                dto.setNombre(get(fila, "nombre"));
                dto.setCodigo_producto(get(fila, "codigo_producto"));
                dto.setCategoria(get(fila, "categoria"));
                dto.setPrecio_compra(getBigDecimal(fila, "precio_compra"));
                dto.setPrecio_venta(getBigDecimal(fila, "precio_venta"));
                dto.setFecha_compra(getLocalDate(fila, "fecha_compra"));
                dto.setCantidad_disponible(getInteger(fila, "cantidad_disponible"));
                dto.setStock_minimo(getInteger(fila, "stock_minimo"));

                if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
                    resultado.agregarError(numeroFila, "Nombre es requerido");
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                    continue;
                }

                // Buscar proveedor por nombre en lugar de por ID
                String nombreProveedor = get(fila, "proveedor");
                if (nombreProveedor == null || nombreProveedor.trim().isEmpty()) {
                    resultado.agregarError(numeroFila, "El nombre del proveedor es requerido");
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                    continue;
                }
                dto.setProveedor_id(proveedoresService.buscarIdPorNombre(nombreProveedor));

                // Upsert: actualiza si existe, crea si no existe
                boolean fueActualizado = productosServicios.upsertProducto(dto);
                resultado.setRegistrosExitosos(resultado.getRegistrosExitosos() + 1);
                if (fueActualizado) {
                    resultado.setRegistrosActualizados(resultado.getRegistrosActualizados() + 1);
                } else {
                    resultado.setRegistrosCreados(resultado.getRegistrosCreados() + 1);
                }

            } catch (Exception e) {
                resultado.agregarError(numeroFila, e.getMessage());
                resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
            }
        }

        return resultado;
    }

    // ==================== IMPORTAR PROVEEDORES ====================

    public ImportacionResponseDTO importarProveedores(MultipartFile archivo) {
        ImportacionResponseDTO resultado = new ImportacionResponseDTO();
        resultado.setTipoImportacion("PROVEEDORES");

        List<Map<String, String>> filas;
        try {
            filas = leerFilas(archivo);
        } catch (Exception e) {
            log.error("Error leyendo archivo: {}", e.getMessage());
            resultado.agregarError(0, "Error leyendo archivo: " + e.getMessage());
            return resultado;
        }

        // Set para rastrear proveedores que vienen duplicados dentro del mismo Excel
        Set<String> nombresProcesadosEnArchivo = new HashSet<>();

        for (int i = 0; i < filas.size(); i++) {
            Map<String, String> fila = filas.get(i);
            int numeroFila = i + 2;
            resultado.setRegistrosProcesados(resultado.getRegistrosProcesados() + 1);

            try {
                ProveedoresRequestDTO dto = new ProveedoresRequestDTO();
                String nombre = get(fila, "nombre");

                dto.setNombre(nombre);
                dto.setTelefono(get(fila, "telefono"));
                dto.setEmail(get(fila, "email"));
                dto.setDireccion(get(fila, "direccion"));
                dto.setNombre_contacto(get(fila, "nombre_contacto"));

                // 1. Validar que el nombre no esté vacío
                if (nombre == null || nombre.trim().isEmpty()) {
                    resultado.agregarError(numeroFila, "Nombre es requerido");
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                    continue;
                }

                String nombreNormalizado = nombre.trim().toLowerCase();

                // 2. Validar duplicados dentro del mismo archivo Excel
                if (nombresProcesadosEnArchivo.contains(nombreNormalizado)) {
                    resultado.agregarError(numeroFila, "Proveedor duplicado dentro del archivo: " + nombre);
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                    continue;
                }

                // 3. Validar duplicados contra la base de datos
                // Necesitarás tener un método en tu service que verifique la existencia
                if (proveedoresRepository.existsByNombreIgnoreCase(nombre.trim())) {
                    resultado.agregarError(numeroFila, "El proveedor ya existe en la base de datos: " + nombre);
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                    continue;
                }

                // Si pasa todas las validaciones, lo agregamos al Set y lo guardamos
                nombresProcesadosEnArchivo.add(nombreNormalizado);
                proveedoresService.crearProveedor(dto);

                resultado.setRegistrosExitosos(resultado.getRegistrosExitosos() + 1);

            } catch (Exception e) {
                resultado.agregarError(numeroFila, e.getMessage());
                resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
            }
        }

        return resultado;
    }

    // ==================== IMPORTAR SERVICIOS ====================

    public ImportacionResponseDTO importarServicios(MultipartFile archivo) {
        ImportacionResponseDTO resultado = new ImportacionResponseDTO();
        resultado.setTipoImportacion("SERVICIOS");

        List<Map<String, String>> filas;
        try {
            filas = leerFilas(archivo);
        } catch (Exception e) {
            log.error("Error leyendo archivo: {}", e.getMessage());
            resultado.agregarError(0, "Error leyendo archivo: " + e.getMessage());
            return resultado;
        }

        // Mapa ordenado: clave del servicio → datos agrupados de todas sus filas
        Map<String, GrupoServicio> grupos = new LinkedHashMap<>();

        // Primera pasada: resolver entidades y agrupar filas por servicio
        for (int i = 0; i < filas.size(); i++) {
            Map<String, String> fila = filas.get(i);
            int numeroFila = i + 2;
            resultado.setRegistrosProcesados(resultado.getRegistrosProcesados() + 1);

            try {
                // Buscar cliente por cédula primero, luego por nombre
                Clientes cliente = null;
                String cedulaRuc = get(fila, "cedula_ruc");
                String nombreCliente = get(fila, "cliente");

                if (cedulaRuc != null) {
                    cliente = clienteRepository.findByCedulaRuc(cedulaRuc).orElse(null);
                }
                if (cliente == null && nombreCliente != null) {
                    cliente = clienteRepository.findByNombreIgnoreCase(nombreCliente).orElse(null);
                }
                if (cliente == null) {
                    resultado.agregarError(numeroFila,
                            "No se encontró cliente con cédula '" + cedulaRuc + "' o nombre '" + nombreCliente + "'");
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                    continue;
                }

                // Vehículo es opcional (puede ser una venta sin vehículo)
                final Clientes clienteFinal = cliente;
                String placa = get(fila, "placa");
                Integer vehiculoId = null;

                if (placa != null && !placa.isBlank()) {
                    Vehiculos vehiculo = vehiculosRepository.findByPlacaIgnoreCase(placa)
                            .stream()
                            .filter(v -> v.getClientes().getCliente_id() == clienteFinal.getCliente_id())
                            .findFirst()
                            .orElse(null);

                    if (vehiculo == null) {
                        // Crear el vehículo asociado al cliente
                        Vehiculos nuevoVehiculo = new Vehiculos();
                        nuevoVehiculo.setPlaca(placa);
                        String km = get(fila, "kilometraje");
                        nuevoVehiculo.setKilometraje_actual(km != null ? km : "0");
                        nuevoVehiculo.setClientes(clienteFinal);
                        vehiculo = vehiculosRepository.save(nuevoVehiculo);
                    }

                    vehiculoId = vehiculo.getVehiculo_id();
                }

                LocalDate fechaServicio = getLocalDate(fila, "fecha_servicio");
                String tipoServicio = normalizarTipoServicio(get(fila, "tipo_servicio"));

                // Clave de agrupación: mismo cliente + vehículo + fecha + tipo = un único
                // servicio
                final Integer vehiculoIdFinal = vehiculoId;
                String claveGrupo = cliente.getCliente_id() + "|" + vehiculoId + "|" + fechaServicio + "|"
                        + tipoServicio;

                // Obtener el grupo existente o crear uno nuevo con los datos de la primera fila
                GrupoServicio grupo = grupos.computeIfAbsent(claveGrupo, k -> {
                    GrupoServicio g = new GrupoServicio();
                    g.clienteId = clienteFinal.getCliente_id();
                    g.vehiculoId = vehiculoIdFinal;
                    g.fechaServicio = fechaServicio;
                    g.tipoServicio = tipoServicio;
                    g.kilometrajeServicio = get(fila, "kilometraje_servicio");
                    g.observaciones = get(fila, "observaciones");
                    return g;
                });
                grupo.filas.add(numeroFila);

                // Agregar detalle de producto si está presente en la fila.
                // La resolución del producto tiene su propio try-catch para que un fallo
                // aquí no descarte el grupo completo del servicio.
                String codigoProducto = get(fila, "codigo_producto");
                // Fallback: algunos Excel usan "nombre_producto" o "producto" en lugar de
                // código
                String nombreProducto = get(fila, "nombre_producto");
                if (nombreProducto == null)
                    nombreProducto = get(fila, "producto");

                // Cantidad: acepta decimales (ej: 0.5, 1.5 litros)
                BigDecimal cantidad = getBigDecimal(fila, "cantidad");

                // Precio unitario desde el Excel — se prueban varios nombres de columna
                // posibles
                BigDecimal precioUnitario = getBigDecimal(fila, "precio_unitario");
                if (precioUnitario == null)
                    precioUnitario = getBigDecimal(fila, "precio");
                if (precioUnitario == null)
                    precioUnitario = getBigDecimal(fila, "precio_unitario_congelado");
                if (precioUnitario == null)
                    precioUnitario = getBigDecimal(fila, "precio_venta");

                String tipoItem = get(fila, "tipo_item");

                boolean tieneIdentificador = codigoProducto != null || nombreProducto != null;

                if (tieneIdentificador && cantidad != null) {
                    final String codigoFinal = codigoProducto;
                    final String nombreFinal = nombreProducto;
                    final BigDecimal precioFinal = precioUnitario;
                    try {
                        // Busca primero por código, luego por nombre como alternativa
                        Productos producto = null;
                        if (codigoFinal != null) {
                            producto = productosRepository.findByCodigoProducto(codigoFinal).orElse(null);
                        }
                        if (producto == null && nombreFinal != null) {
                            producto = productosRepository.findByNombreIgnoreCase(nombreFinal).orElse(null);
                        }
                        if (producto == null) {
                            throw new RuntimeException("Producto no encontrado — código: '"
                                    + codigoFinal + "', nombre: '" + nombreFinal + "'");
                        }

                        DetalleServicioRequestDTO detalle = new DetalleServicioRequestDTO();
                        detalle.setProducto_id(producto.getProducto_id());
                        detalle.setCantidad(cantidad);
                        // Si el Excel trae precio, se usa ese; si no, crearServicio usará el de BD
                        detalle.setPrecio_unitario_congelado(precioFinal);
                        detalle.setTipoItem(tipoItem != null ? tipoItem : "PRODUCTO");
                        grupo.detalles.add(detalle);

                    } catch (Exception e) {
                        // El producto no se pudo resolver: se advierte en el reporte
                        // pero no se interrumpe el procesamiento del resto del grupo
                        resultado.agregarAdvertencia(numeroFila,
                                "Producto no agregado al servicio: " + e.getMessage());
                    }
                }

            } catch (Exception e) {
                resultado.agregarError(numeroFila, e.getMessage());
                resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
            }
        }

        // Segunda pasada: crear un servicio por cada grupo agrupado
        for (GrupoServicio grupo : grupos.values()) {
            try {
                // Verificar si el servicio ya existe en base de datos
                if (grupo.fechaServicio != null && grupo.tipoServicio != null &&
                        serviciosRepository.existsDuplicado(grupo.clienteId, grupo.vehiculoId, grupo.fechaServicio,
                                grupo.tipoServicio)) {
                    resultado.agregarAdvertencia(grupo.filas.get(0),
                            "Servicio ya registrado (filas " + grupo.filas
                                    + "): mismo cliente, vehículo, fecha y tipo — omitido");
                    resultado.setRegistrosOmitidos(resultado.getRegistrosOmitidos() + 1);
                    continue;
                }

                ServicioRequestDTO dto = new ServicioRequestDTO();
                dto.setCliente_id(grupo.clienteId);
                dto.setVehiculo_id(grupo.vehiculoId);
                dto.setFecha_servicio(grupo.fechaServicio);
                dto.setKilometraje_servicio(grupo.kilometrajeServicio);
                dto.setTipo_servicio(grupo.tipoServicio);
                dto.setObservaciones(grupo.observaciones);
                dto.setDetalles(grupo.detalles.isEmpty() ? null : grupo.detalles);

                serviciosService.crearServicio(dto);
                resultado.setRegistrosExitosos(resultado.getRegistrosExitosos() + 1);
                resultado.setRegistrosCreados(resultado.getRegistrosCreados() + 1);

            } catch (Exception e) {
                resultado.agregarError(grupo.filas.get(0),
                        "Error en filas " + grupo.filas + ": " + e.getMessage());
                resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + grupo.filas.size());
            }
        }

        return resultado;
    }

    // Clase auxiliar para agrupar las filas del Excel que pertenecen al mismo
    // servicio
    private static class GrupoServicio {
        int clienteId;
        Integer vehiculoId;
        LocalDate fechaServicio;
        String kilometrajeServicio;
        String tipoServicio;
        String observaciones;
        List<DetalleServicioRequestDTO> detalles = new ArrayList<>();
        List<Integer> filas = new ArrayList<>();
    }

    // ==================== ACTUALIZAR STOCK ====================

    public ImportacionResponseDTO actualizarStock(MultipartFile archivo) {
        ImportacionResponseDTO resultado = new ImportacionResponseDTO();
        resultado.setTipoImportacion("ACTUALIZACION_STOCK");

        List<Map<String, String>> filas;
        try {
            filas = leerFilas(archivo);
        } catch (Exception e) {
            log.error("Error leyendo archivo: {}", e.getMessage());
            resultado.agregarError(0, "Error leyendo archivo: " + e.getMessage());
            return resultado;
        }

        for (int i = 0; i < filas.size(); i++) {
            Map<String, String> fila = filas.get(i);
            int numeroFila = i + 2;
            resultado.setRegistrosProcesados(resultado.getRegistrosProcesados() + 1);

            try {
                Integer productoId = getInteger(fila, "producto_id");
                Integer cantidadNueva = getInteger(fila, "cantidad");
                String operacion = get(fila, "operacion"); // "AGREGAR" o "ESTABLECER"

                if (productoId == null) {
                    resultado.agregarError(numeroFila, "producto_id es requerido");
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                    continue;
                }

                if (cantidadNueva == null) {
                    resultado.agregarError(numeroFila, "cantidad es requerida");
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                    continue;
                }

                Integer cantidadFinal;
                if ("AGREGAR".equalsIgnoreCase(operacion)) {
                    var productoActual = productosServicios.obtenerProductoByID(productoId);
                    int cantidadActual = productoActual.getCantidad_disponible() != null
                            ? productoActual.getCantidad_disponible()
                            : 0;
                    cantidadFinal = cantidadActual + cantidadNueva;
                } else {
                    cantidadFinal = cantidadNueva;
                }

                productosServicios.actualizarSoloStock(productoId, cantidadFinal);
                resultado.setRegistrosExitosos(resultado.getRegistrosExitosos() + 1);

            } catch (Exception e) {
                resultado.agregarError(numeroFila, e.getMessage());
                resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
            }
        }

        return resultado;
    }

    // ==================== LECTURA DE ARCHIVO ====================

    private List<Map<String, String>> leerFilas(MultipartFile archivo) throws Exception {
        String nombre = archivo.getOriginalFilename();
        if (nombre != null && nombre.toLowerCase().endsWith(".csv")) {
            return leerFilasCSV(archivo.getInputStream());
        } else {
            return leerFilasExcel(archivo.getInputStream());
        }
    }

    private List<Map<String, String>> leerFilasExcel(InputStream is) throws Exception {
        List<Map<String, String>> filas = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            // Leer cabeceras de la primera fila
            Row filaCabecera = sheet.getRow(0);
            if (filaCabecera == null)
                return filas;

            List<String> cabeceras = new ArrayList<>();
            for (int j = 0; j < filaCabecera.getLastCellNum(); j++) {
                Cell cell = filaCabecera.getCell(j);
                String cabecera = cell != null ? normalizarCabecera(formatter.formatCellValue(cell)) : "";
                cabeceras.add(cabecera);
            }

            // Leer filas de datos desde la segunda fila
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || esFilaVacia(row))
                    continue;

                Map<String, String> fila = new LinkedHashMap<>();
                for (int j = 0; j < cabeceras.size(); j++) {
                    String cabecera = cabeceras.get(j);
                    if (cabecera.isEmpty())
                        continue;

                    Cell cell = row.getCell(j);
                    String valor = null;
                    if (cell != null) {
                        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                            // Extraer fecha directamente en formato ISO para evitar problemas de
                            // localización
                            valor = DateUtil.getLocalDateTime(cell.getNumericCellValue()).toLocalDate().toString();
                        } else if (cell.getCellType() == CellType.NUMERIC) {
                            valor = NumberToTextConverter.toText(cell.getNumericCellValue()).trim();
                        } else {
                            valor = formatter.formatCellValue(cell).trim();
                        }
                        if (valor.isEmpty())
                            valor = null;
                    }
                    fila.put(cabecera, valor);
                }
                filas.add(fila);
            }
        }

        return filas;
    }

    private List<Map<String, String>> leerFilasCSV(InputStream is) throws Exception {
        List<Map<String, String>> filas = new ArrayList<>();
        List<String> cabeceras = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.trim().isEmpty())
                    continue;
                String[] campos = parsearLineaCSV(linea);

                if (cabeceras == null) {
                    cabeceras = new ArrayList<>();
                    for (String campo : campos) {
                        cabeceras.add(normalizarCabecera(campo));
                    }
                    continue;
                }

                Map<String, String> fila = new LinkedHashMap<>();
                for (int j = 0; j < cabeceras.size(); j++) {
                    String cabecera = cabeceras.get(j);
                    if (cabecera.isEmpty())
                        continue;
                    String valor = j < campos.length ? campos[j].trim() : null;
                    fila.put(cabecera, (valor == null || valor.isEmpty()) ? null : valor);
                }
                filas.add(fila);
            }
        }

        return filas;
    }

    private String[] parsearLineaCSV(String linea) {
        List<String> campos = new ArrayList<>();
        boolean enComillas = false;
        StringBuilder campo = new StringBuilder();

        for (char c : linea.toCharArray()) {
            if (c == '"') {
                enComillas = !enComillas;
            } else if (c == ',' && !enComillas) {
                campos.add(campo.toString());
                campo.setLength(0);
            } else {
                campo.append(c);
            }
        }
        campos.add(campo.toString());

        return campos.toArray(new String[0]);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private boolean esFilaVacia(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private String normalizarCabecera(String cabecera) {
        if (cabecera == null)
            return "";
        return cabecera.trim().toLowerCase().replaceAll("\\s+", "_");
    }

    private String get(Map<String, String> fila, String cabecera) {
        String val = fila.get(cabecera.toLowerCase());
        return (val == null || val.isEmpty()) ? null : val;
    }

    private Integer getInteger(Map<String, String> fila, String cabecera) {
        String val = get(fila, cabecera);
        if (val == null)
            return null;
        try {
            return (int) Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal getBigDecimal(Map<String, String> fila, String cabecera) {
        String val = get(fila, cabecera);
        if (val == null)
            return null;
        try {
            return new BigDecimal(val.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE, // 2026-03-26
            DateTimeFormatter.ofPattern("dd/MM/yyyy"), // 26/03/2026
            DateTimeFormatter.ofPattern("MM/dd/yyyy"), // 03/26/2026
            DateTimeFormatter.ofPattern("dd-MM-yyyy"), // 26-03-2026
            DateTimeFormatter.ofPattern("yyyy/MM/dd") // 2026/03/26
    );

    /**
     * Normaliza el tipo de servicio leído del Excel al valor exacto esperado.
     * Acepta variantes como "Producto", "producto", "Mano de Obra", "mano_de_obra",
     * etc.
     */
    private String normalizarTipoServicio(String valor) {
        if (valor == null)
            return "PRODUCTO";
        String v = valor.trim().toUpperCase().replace(" ", "_").replace("-", "_");
        if (v.contains("MANO") || v.equals("MANO_DE_OBRA"))
            return "MANO_DE_OBRA";
        return "PRODUCTO";
    }

    private LocalDate getLocalDate(Map<String, String> fila, String cabecera) {
        String val = get(fila, cabecera);
        if (val == null)
            return null;
        for (DateTimeFormatter fmt : FORMATOS_FECHA) {
            try {
                return LocalDate.parse(val, fmt);
            } catch (DateTimeParseException e) {
                // Intentar con el siguiente formato
            }
        }
        return null;
    }
}
