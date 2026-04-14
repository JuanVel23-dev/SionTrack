package com.siontrack.siontrack.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.siontrack.siontrack.DTO.Response.ImportacionResponseDTO;
import com.siontrack.siontrack.services.ImportacionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * API REST para la carga masiva de datos desde archivos Excel (.xlsx, .xls) o CSV.
 *
 * <p>Todos los endpoints siguen la misma convención: reciben un archivo como
 * {@code multipart/form-data} con el parámetro {@code archivo} y devuelven un
 * {@link ImportacionResponseDTO} con el resumen detallado del resultado.
 */
@Tag(name = "Importación", description = "Carga masiva de clientes, productos, proveedores, servicios y stock")
@RestController
@RequestMapping("/api/importar")
public class ImportacionController {

    private final ImportacionService importacionService;

    public ImportacionController(ImportacionService importacionService) {
        this.importacionService = importacionService;
    }

    /**
     * Importa clientes desde un archivo Excel o CSV.
     * Si el cliente ya existe (por cédula o nombre), se actualiza; de lo contrario se crea.
     */
    @Operation(
        summary = "Importar clientes",
        description = "Columnas esperadas: nombre*, cedula_ruc, tipo_cliente, telefono, correo, "
                    + "direccion, placa, kilometraje_actual. (*) obligatorio."
    )
    @PostMapping("/clientes")
    public ResponseEntity<ImportacionResponseDTO> importarClientes(
            @Parameter(description = "Archivo Excel (.xlsx/.xls) o CSV")
            @RequestParam("archivo") MultipartFile archivo) {
        return procesarArchivo(archivo, importacionService::importarClientes);
    }

    /**
     * Importa productos desde un archivo Excel o CSV con lógica upsert.
     */
    @Operation(
        summary = "Importar productos",
        description = "Columnas esperadas: nombre*, codigo_producto, categoria, precio_compra, "
                    + "precio_venta, fecha_compra, cantidad_disponible, stock_minimo, proveedor*. "
                    + "(*) obligatorio. Si el producto ya existe se actualiza."
    )
    @PostMapping("/productos")
    public ResponseEntity<ImportacionResponseDTO> importarProductos(
            @Parameter(description = "Archivo Excel (.xlsx/.xls) o CSV")
            @RequestParam("archivo") MultipartFile archivo) {
        return procesarArchivo(archivo, importacionService::importarProductos);
    }

    /**
     * Importa proveedores nuevos desde un archivo Excel o CSV.
     * Los proveedores ya existentes se rechazan (no se permite upsert).
     */
    @Operation(
        summary = "Importar proveedores",
        description = "Columnas esperadas: nombre*, telefono, email, direccion, nombre_contacto. "
                    + "(*) obligatorio. No admite actualización: los proveedores ya existentes se rechazan."
    )
    @PostMapping("/proveedores")
    public ResponseEntity<ImportacionResponseDTO> importarProveedores(
            @Parameter(description = "Archivo Excel (.xlsx/.xls) o CSV")
            @RequestParam("archivo") MultipartFile archivo) {
        return procesarArchivo(archivo, importacionService::importarProveedores);
    }

    /**
     * Importa servicios desde un archivo Excel o CSV con agrupación automática de detalles.
     * Varias filas con el mismo cliente, vehículo, fecha y tipo se agrupan en un único servicio.
     */
    @Operation(
        summary = "Importar servicios",
        description = "Columnas esperadas: cedula_ruc o cliente*, placa, fecha_servicio*, "
                    + "tipo_servicio, kilometraje_servicio, observaciones, codigo_producto o nombre_producto, "
                    + "cantidad, precio_unitario. (*) obligatorio. Filas con misma clave "
                    + "(cliente+vehículo+fecha+tipo) se agrupan en un solo servicio."
    )
    @PostMapping("/servicios")
    public ResponseEntity<ImportacionResponseDTO> importarServicios(
            @Parameter(description = "Archivo Excel (.xlsx/.xls) o CSV")
            @RequestParam("archivo") MultipartFile archivo) {
        return procesarArchivo(archivo, importacionService::importarServicios);
    }

    /**
     * Actualiza el stock de productos desde un archivo Excel o CSV.
     */
    @Operation(
        summary = "Actualizar stock",
        description = "Columnas esperadas: producto_id*, cantidad*, operacion. "
                    + "(*) obligatorio. operacion=AGREGAR suma al stock actual; "
                    + "cualquier otro valor establece la cantidad directamente."
    )
    @PostMapping("/stock")
    public ResponseEntity<ImportacionResponseDTO> actualizarStock(
            @Parameter(description = "Archivo Excel (.xlsx/.xls) o CSV")
            @RequestParam("archivo") MultipartFile archivo) {
        return procesarArchivo(archivo, importacionService::actualizarStock);
    }

    /**
     * Valida que el archivo no esté vacío y delega el procesamiento al servicio indicado.
     */
    private ResponseEntity<ImportacionResponseDTO> procesarArchivo(
            MultipartFile archivo,
            java.util.function.Function<MultipartFile, ImportacionResponseDTO> procesador) {

        if (archivo.isEmpty()) {
            ImportacionResponseDTO error = new ImportacionResponseDTO();
            error.agregarError(0, "El archivo está vacío");
            return ResponseEntity.badRequest().body(error);
        }

        ImportacionResponseDTO resultado = procesador.apply(archivo);
        return ResponseEntity.ok(resultado);
    }
}
