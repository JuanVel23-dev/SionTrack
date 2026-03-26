package com.siontrack.siontrack.services;

import com.siontrack.siontrack.DTO.Request.*;
import com.siontrack.siontrack.DTO.Response.ImportacionResponseDTO;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ImportacionService {

    private static final Logger log = LoggerFactory.getLogger(ImportacionService.class);

    private final ClienteServicios clienteServicios;
    private final ProductosServicios productosServicios;
    private final ProveedoresService proveedoresService;
    private final ServiciosService serviciosService;

    public ImportacionService(ClienteServicios clienteServicios,
                        ProductosServicios productosServicios,
                        ProveedoresService proveedoresService,
                        ServiciosService serviciosService) {
        this.clienteServicios = clienteServicios;
        this.productosServicios = productosServicios;
        this.proveedoresService = proveedoresService;
        this.serviciosService = serviciosService;
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

        for (int i = 0; i < filas.size(); i++) {
            Map<String, String> fila = filas.get(i);
            int numeroFila = i + 2;
            resultado.setRegistrosProcesados(resultado.getRegistrosProcesados() + 1);

            try {
                ClienteRequestDTO dto = new ClienteRequestDTO();
                dto.setNombre(get(fila, "nombre"));
                dto.setCedula_ruc(get(fila, "cedula_ruc"));
                dto.setTipo_cliente(get(fila, "tipo_cliente"));

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
                    vehiculoDto.setKilometraje_actual(get(fila, "kilometraje"));
                    dto.setVehiculos(List.of(vehiculoDto));
                }

                if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
                    resultado.agregarError(numeroFila, "Nombre es requerido");
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                    continue;
                }

                clienteServicios.crearCliente(dto);
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
                dto.setMarca(get(fila, "marca"));
                dto.setUnidad_medida(get(fila, "unidad_medida"));
                dto.setPrecio_compra(getBigDecimal(fila, "precio_compra"));
                dto.setPrecio_venta(getBigDecimal(fila, "precio_venta"));
                dto.setEstado(get(fila, "estado"));
                dto.setProveedor_id(getInteger(fila, "proveedor_id"));
                dto.setCantidad_disponible(getInteger(fila, "cantidad_disponible"));
                dto.setStock_minimo(getInteger(fila, "stock_minimo"));
                dto.setUbicacion(get(fila, "ubicacion"));

                if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
                    resultado.agregarError(numeroFila, "Nombre es requerido");
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                    continue;
                }

                if (dto.getProveedor_id() == null) {
                    resultado.agregarError(numeroFila, "proveedor_id es requerido");
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                    continue;
                }

                productosServicios.crearProducto(dto);
                resultado.setRegistrosExitosos(resultado.getRegistrosExitosos() + 1);

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

        for (int i = 0; i < filas.size(); i++) {
            Map<String, String> fila = filas.get(i);
            int numeroFila = i + 2;
            resultado.setRegistrosProcesados(resultado.getRegistrosProcesados() + 1);

            try {
                ProveedoresRequestDTO dto = new ProveedoresRequestDTO();
                dto.setNombre(get(fila, "nombre"));
                dto.setTelefono(get(fila, "telefono"));
                dto.setEmail(get(fila, "email"));
                dto.setDireccion(get(fila, "direccion"));
                dto.setNombre_contacto(get(fila, "nombre_contacto"));

                if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
                    resultado.agregarError(numeroFila, "Nombre es requerido");
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                    continue;
                }

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

        for (int i = 0; i < filas.size(); i++) {
            Map<String, String> fila = filas.get(i);
            int numeroFila = i + 2;
            resultado.setRegistrosProcesados(resultado.getRegistrosProcesados() + 1);

            try {
                ServicioRequestDTO dto = new ServicioRequestDTO();
                dto.setCliente_id(getInteger(fila, "cliente_id"));
                dto.setVehiculo_id(getInteger(fila, "vehiculo_id"));
                dto.setFecha_servicio(getLocalDate(fila, "fecha_servicio"));
                dto.setKilometraje_servicio(get(fila, "kilometraje_servicio"));
                dto.setTipo_servicio(get(fila, "tipo_servicio"));
                dto.setObservaciones(get(fila, "observaciones"));

                Integer productoId = getInteger(fila, "producto_id");
                BigDecimal cantidad = getBigDecimal(fila, "cantidad");
                String tipoItem = get(fila, "tipo_item");

                if (productoId != null && cantidad != null) {
                    DetalleServicioRequestDTO detalle = new DetalleServicioRequestDTO();
                    detalle.setProducto_id(productoId);
                    detalle.setCantidad(cantidad);
                    detalle.setTipoItem(tipoItem != null ? tipoItem : "PRODUCTO");
                    dto.setDetalles(List.of(detalle));
                }

                if (dto.getCliente_id() == null) {
                    resultado.agregarError(numeroFila, "cliente_id es requerido");
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                    continue;
                }

                if (dto.getVehiculo_id() == null) {
                    resultado.agregarError(numeroFila, "vehiculo_id es requerido");
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                    continue;
                }

                serviciosService.crearServicio(dto);
                resultado.setRegistrosExitosos(resultado.getRegistrosExitosos() + 1);

            } catch (Exception e) {
                resultado.agregarError(numeroFila, e.getMessage());
                resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
            }
        }

        return resultado;
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
                            ? productoActual.getCantidad_disponible() : 0;
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
            if (filaCabecera == null) return filas;

            List<String> cabeceras = new ArrayList<>();
            for (int j = 0; j < filaCabecera.getLastCellNum(); j++) {
                Cell cell = filaCabecera.getCell(j);
                String cabecera = cell != null ? normalizarCabecera(formatter.formatCellValue(cell)) : "";
                cabeceras.add(cabecera);
            }

            // Leer filas de datos desde la segunda fila
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || esFilaVacia(row)) continue;

                Map<String, String> fila = new LinkedHashMap<>();
                for (int j = 0; j < cabeceras.size(); j++) {
                    String cabecera = cabeceras.get(j);
                    if (cabecera.isEmpty()) continue;

                    Cell cell = row.getCell(j);
                    String valor = null;
                    if (cell != null) {
                        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
                            valor = NumberToTextConverter.toText(cell.getNumericCellValue()).trim();
                        } else {
                            valor = formatter.formatCellValue(cell).trim();
                        }
                        if (valor.isEmpty()) valor = null;
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
                if (linea.trim().isEmpty()) continue;
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
                    if (cabecera.isEmpty()) continue;
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
        if (cabecera == null) return "";
        return cabecera.trim().toLowerCase().replaceAll("\\s+", "_");
    }

    private String get(Map<String, String> fila, String cabecera) {
        String val = fila.get(cabecera.toLowerCase());
        return (val == null || val.isEmpty()) ? null : val;
    }

    private Integer getInteger(Map<String, String> fila, String cabecera) {
        String val = get(fila, cabecera);
        if (val == null) return null;
        try {
            return (int) Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal getBigDecimal(Map<String, String> fila, String cabecera) {
        String val = get(fila, cabecera);
        if (val == null) return null;
        try {
            return new BigDecimal(val.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate getLocalDate(Map<String, String> fila, String cabecera) {
        String val = get(fila, cabecera);
        if (val == null) return null;
        try {
            return LocalDate.parse(val);
        } catch (Exception e) {
            return null;
        }
    }
}
