package com.siontrack.siontrack.services;

import com.siontrack.siontrack.DTO.Request.*;
import com.siontrack.siontrack.DTO.Response.ImportacionResponseDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

        try (InputStream is = archivo.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int filaInicio = 1;

            for (int i = filaInicio; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || esFilaVacia(row)) continue;

                resultado.setRegistrosProcesados(resultado.getRegistrosProcesados() + 1);

                try {
                    ClienteRequestDTO dto = new ClienteRequestDTO();
                    dto.setNombre(getCellString(row, 0));
                    dto.setCedula_ruc(getCellString(row, 1));
                    dto.setTipo_cliente(getCellString(row, 2));

                    // Teléfono
                    String telefono = getCellString(row, 3);
                    if (telefono != null && !telefono.trim().isEmpty()) {
                        TelefonosRequestDTO telDto = new TelefonosRequestDTO();
                        telDto.setTelefono(telefono);
                        dto.setTelefonos(List.of(telDto));
                    }

                    // Correo
                    String correo = getCellString(row, 4);
                    if (correo != null && !correo.trim().isEmpty()) {
                        CorreosRequestDTO correoDto = new CorreosRequestDTO();
                        correoDto.setCorreo(correo);
                        dto.setCorreos(List.of(correoDto));
                    }

                    // Dirección
                    String direccion = getCellString(row, 5);
                    if (direccion != null && !direccion.trim().isEmpty()) {
                        DireccionesRequestDTO dirDto = new DireccionesRequestDTO();
                        dirDto.setDireccion(direccion);
                        dto.setDirecciones(List.of(dirDto));
                    }

                    // Validación
                    if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
                        resultado.agregarError(i + 1, "Nombre es requerido");
                        resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                        continue;
                    }

                    clienteServicios.crearCliente(dto);
                    resultado.setRegistrosExitosos(resultado.getRegistrosExitosos() + 1);

                } catch (Exception e) {
                    resultado.agregarError(i + 1, e.getMessage());
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                }
            }

        } catch (Exception e) {
            log.error("Error leyendo archivo Excel: {}", e.getMessage());
            resultado.agregarError(0, "Error leyendo archivo: " + e.getMessage());
        }

        return resultado;
    }

    // ==================== IMPORTAR PRODUCTOS ====================

    public ImportacionResponseDTO importarProductos(MultipartFile archivo) {
        ImportacionResponseDTO resultado = new ImportacionResponseDTO();
        resultado.setTipoImportacion("PRODUCTOS");

        try (InputStream is = archivo.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int filaInicio = 1;

            for (int i = filaInicio; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || esFilaVacia(row)) continue;

                resultado.setRegistrosProcesados(resultado.getRegistrosProcesados() + 1);

                try {
                    ProductosRequestDTO dto = new ProductosRequestDTO();
                    dto.setNombre(getCellString(row, 0));
                    dto.setCodigo_producto(getCellString(row, 1));
                    dto.setCategoria(getCellString(row, 2));
                    dto.setMarca(getCellString(row, 3));
                    dto.setUnidad_medida(getCellString(row, 4));
                    dto.setPrecio_compra(getCellBigDecimal(row, 5));
                    dto.setPrecio_venta(getCellBigDecimal(row, 6));
                    dto.setEstado(getCellString(row, 7));
                    dto.setProveedor_id(getCellInteger(row, 8));
                    dto.setCantidad_disponible(getCellInteger(row, 9));
                    dto.setStock_minimo(getCellInteger(row, 10));
                    dto.setUbicacion(getCellString(row, 11));

                    // Validaciones
                    if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
                        resultado.agregarError(i + 1, "Nombre es requerido");
                        resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                        continue;
                    }

                    if (dto.getProveedor_id() == null) {
                        resultado.agregarError(i + 1, "Proveedor ID es requerido");
                        resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                        continue;
                    }

                    productosServicios.crearProducto(dto);
                    resultado.setRegistrosExitosos(resultado.getRegistrosExitosos() + 1);

                } catch (Exception e) {
                    resultado.agregarError(i + 1, e.getMessage());
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                }
            }

        } catch (Exception e) {
            log.error("Error leyendo archivo Excel: {}", e.getMessage());
            resultado.agregarError(0, "Error leyendo archivo: " + e.getMessage());
        }

        return resultado;
    }

    // ==================== IMPORTAR PROVEEDORES ====================

    public ImportacionResponseDTO importarProveedores(MultipartFile archivo) {
        ImportacionResponseDTO resultado = new ImportacionResponseDTO();
        resultado.setTipoImportacion("PROVEEDORES");

        try (InputStream is = archivo.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int filaInicio = 1;

            for (int i = filaInicio; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || esFilaVacia(row)) continue;

                resultado.setRegistrosProcesados(resultado.getRegistrosProcesados() + 1);

                try {
                    ProveedoresRequestDTO dto = new ProveedoresRequestDTO();
                    dto.setNombre(getCellString(row, 0));
                    dto.setTelefono(getCellString(row, 1));
                    dto.setEmail(getCellString(row, 2));
                    dto.setDireccion(getCellString(row, 3));
                    dto.setNombre_contacto(getCellString(row, 4));

                    // Validación
                    if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
                        resultado.agregarError(i + 1, "Nombre es requerido");
                        resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                        continue;
                    }

                    proveedoresService.crearProveedor(dto);
                    resultado.setRegistrosExitosos(resultado.getRegistrosExitosos() + 1);

                } catch (Exception e) {
                    resultado.agregarError(i + 1, e.getMessage());
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                }
            }

        } catch (Exception e) {
            log.error("Error leyendo archivo Excel: {}", e.getMessage());
            resultado.agregarError(0, "Error leyendo archivo: " + e.getMessage());
        }

        return resultado;
    }

    // ==================== IMPORTAR SERVICIOS ====================

    public ImportacionResponseDTO importarServicios(MultipartFile archivo) {
        ImportacionResponseDTO resultado = new ImportacionResponseDTO();
        resultado.setTipoImportacion("SERVICIOS");

        try (InputStream is = archivo.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int filaInicio = 1;

            for (int i = filaInicio; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || esFilaVacia(row)) continue;

                resultado.setRegistrosProcesados(resultado.getRegistrosProcesados() + 1);

                try {
                    ServicioRequestDTO dto = new ServicioRequestDTO();
                    dto.setCliente_id(getCellInteger(row, 0));
                    dto.setVehiculo_id(getCellInteger(row, 1));
                    dto.setFecha_servicio(getCellLocalDate(row, 2));
                    dto.setKilometraje_servicio(getCellString(row, 3));
                    dto.setEstado(getCellString(row, 4));
                    dto.setObservaciones(getCellString(row, 5));

                    // Detalles del servicio (si hay)
                    Integer productoId = getCellInteger(row, 6);
                    BigDecimal cantidad = getCellBigDecimal(row, 7);
                    String tipoItem = getCellString(row, 8);

                    if (productoId != null && cantidad != null) {
                        DetalleServicioRequestDTO detalle = new DetalleServicioRequestDTO();
                        detalle.setProducto_id(productoId);
                        detalle.setCantidad(cantidad);
                        detalle.setTipoItem(tipoItem != null ? tipoItem : "PRODUCTO");
                        dto.setDetalles(List.of(detalle));
                    }

                    // Validaciones
                    if (dto.getCliente_id() == null) {
                        resultado.agregarError(i + 1, "Cliente ID es requerido");
                        resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                        continue;
                    }

                    if (dto.getVehiculo_id() == null) {
                        resultado.agregarError(i + 1, "Vehículo ID es requerido");
                        resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                        continue;
                    }

                    serviciosService.crearServicio(dto);
                    resultado.setRegistrosExitosos(resultado.getRegistrosExitosos() + 1);

                } catch (Exception e) {
                    resultado.agregarError(i + 1, e.getMessage());
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                }
            }

        } catch (Exception e) {
            log.error("Error leyendo archivo Excel: {}", e.getMessage());
            resultado.agregarError(0, "Error leyendo archivo: " + e.getMessage());
        }

        return resultado;
    }

    // ==================== ACTUALIZAR STOCK ====================

    public ImportacionResponseDTO actualizarStock(MultipartFile archivo) {
        ImportacionResponseDTO resultado = new ImportacionResponseDTO();
        resultado.setTipoImportacion("ACTUALIZACION_STOCK");

        try (InputStream is = archivo.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int filaInicio = 1;

            for (int i = filaInicio; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || esFilaVacia(row)) continue;

                resultado.setRegistrosProcesados(resultado.getRegistrosProcesados() + 1);

                try {
                    Integer productoId = getCellInteger(row, 0);
                    Integer cantidadNueva = getCellInteger(row, 1);
                    String operacion = getCellString(row, 2); // "AGREGAR" o "ESTABLECER"

                    if (productoId == null) {
                        resultado.agregarError(i + 1, "ID de producto es requerido");
                        resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                        continue;
                    }

                    // Obtener producto actual
                    var productoActual = productosServicios.obtenerProductoByID(productoId);

                    // Calcular nueva cantidad
                    Integer cantidadFinal;
                    if ("AGREGAR".equalsIgnoreCase(operacion)) {
                        int cantidadActual = productoActual.getCantidad_disponible() != null 
                                ? productoActual.getCantidad_disponible() : 0;
                        cantidadFinal = cantidadActual + cantidadNueva;
                    } else {
                        cantidadFinal = cantidadNueva;
                    }

                    // Actualizar usando el servicio existente
                    ProductosRequestDTO dto = new ProductosRequestDTO();
                    dto.setCantidad_disponible(cantidadFinal);

                    productosServicios.actualizarProducto(productoId, dto);
                    resultado.setRegistrosExitosos(resultado.getRegistrosExitosos() + 1);

                } catch (Exception e) {
                    resultado.agregarError(i + 1, e.getMessage());
                    resultado.setRegistrosFallidos(resultado.getRegistrosFallidos() + 1);
                }
            }

        } catch (Exception e) {
            log.error("Error leyendo archivo Excel: {}", e.getMessage());
            resultado.agregarError(0, "Error leyendo archivo: " + e.getMessage());
        }

        return resultado;
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

    private String getCellString(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private Integer getCellInteger(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    private BigDecimal getCellBigDecimal(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING -> {
                try {
                    yield new BigDecimal(cell.getStringCellValue().trim().replace(",", "."));
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    private LocalDate getCellLocalDate(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;

        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            } else if (cell.getCellType() == CellType.STRING) {
                return LocalDate.parse(cell.getStringCellValue().trim());
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}