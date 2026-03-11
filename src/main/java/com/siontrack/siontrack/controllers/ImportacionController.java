package com.siontrack.siontrack.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.siontrack.siontrack.DTO.Response.ImportacionResponseDTO;
import com.siontrack.siontrack.services.ImportacionService;

@RestController
@RequestMapping("/api/importar")
public class ImportacionController {

    private final ImportacionService importacionService;

    public ImportacionController(ImportacionService importacionService ) {
        this.importacionService = importacionService;
    }

    @PostMapping("/clientes")
    public ResponseEntity<ImportacionResponseDTO> importarClientes(@RequestParam("archivo") MultipartFile archivo) {
        return procesarArchivo(archivo, importacionService::importarClientes);
    }

    @PostMapping("/productos")
    public ResponseEntity<ImportacionResponseDTO> importarProductos(@RequestParam("archivo") MultipartFile archivo) {
        return procesarArchivo(archivo, importacionService::importarProductos);
    }

    @PostMapping("/proveedores")
    public ResponseEntity<ImportacionResponseDTO> importarProveedores(@RequestParam("archivo") MultipartFile archivo) {
        return procesarArchivo(archivo, importacionService::importarProveedores);
    }

    @PostMapping("/servicios")
    public ResponseEntity<ImportacionResponseDTO> importarServicios(@RequestParam("archivo") MultipartFile archivo) {
        return procesarArchivo(archivo, importacionService::importarServicios);
    }

    @PostMapping("/stock")
    public ResponseEntity<ImportacionResponseDTO> actualizarStock(@RequestParam("archivo") MultipartFile archivo) {
        return procesarArchivo(archivo, importacionService::actualizarStock);
    }

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
