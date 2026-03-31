package com.siontrack.siontrack.controllers;

import com.siontrack.siontrack.services.ReporteService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private static final Logger log = LoggerFactory.getLogger(ReporteController.class);

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/clientes")
    public ResponseEntity<byte[]> reporteClientes() {
        return generarDescarga("clientes", () -> reporteService.generarReporteClientes());
    }

    @GetMapping("/productos")
    public ResponseEntity<byte[]> reporteProductos() {
        return generarDescarga("productos", () -> reporteService.generarReporteProductos());
    }

    @GetMapping("/proveedores")
    public ResponseEntity<byte[]> reporteProveedores() {
        return generarDescarga("proveedores", () -> reporteService.generarReporteProveedores());
    }

    @GetMapping("/servicios")
    public ResponseEntity<byte[]> reporteServicios() {
        return generarDescarga("servicios", () -> reporteService.generarReporteServicios());
    }

    @GetMapping("/notificaciones")
    public ResponseEntity<byte[]> reporteNotificaciones() {
        return generarDescarga("notificaciones", () -> reporteService.generarReporteNotificaciones());
    }

    @GetMapping("/productos-populares")
    public ResponseEntity<byte[]> reporteProductosPopulares(
            @RequestParam(defaultValue = "general") String periodo) {
        String sufijo = "populares_" + periodo;
        return generarDescarga(sufijo, () -> reporteService.generarReporteProductosPopulares(periodo));
    }

    // Genera la respuesta HTTP con headers de descarga PDF
    private ResponseEntity<byte[]> generarDescarga(String tipo, PdfGenerator generador) {
        try {
            byte[] pdf = generador.generar();
            String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String nombre = "SionTrack_" + tipo + "_" + fecha + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdf.length)
                    .body(pdf);

        } catch (Exception e) {
            log.error("Error al generar reporte {}: {}", tipo, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @FunctionalInterface
    private interface PdfGenerator {
        byte[] generar() throws Exception;
    }
}
