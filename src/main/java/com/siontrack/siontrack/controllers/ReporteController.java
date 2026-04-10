package com.siontrack.siontrack.controllers;

import com.siontrack.siontrack.services.ReporteService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
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
    public ResponseEntity<byte[]> reporteClientes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            byte[] pdf = reporteService.generarReporteClientes(fechaInicio, fechaFin);
            return generarDescarga("clientes", pdf);
        } catch (Exception e) {
            log.error("Error al generar reporte de clientes: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/productos")
    public ResponseEntity<byte[]> reporteProductos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            byte[] pdf = reporteService.generarReporteProductos(fechaInicio, fechaFin);
            return generarDescarga("productos", pdf);
        } catch (Exception e) {
            log.error("Error al generar reporte de productos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/proveedores")
    public ResponseEntity<byte[]> reporteProveedores(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            byte[] pdf = reporteService.generarReporteProveedores(fechaInicio, fechaFin);
            return generarDescarga("proveedores", pdf);
        } catch (Exception e) {
            log.error("Error al generar reporte de proveedores: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/servicios")
    public ResponseEntity<byte[]> reporteServicios(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            byte[] pdf = reporteService.generarReporteServicios(fechaInicio, fechaFin);
            return generarDescarga("servicios", pdf);
        } catch (Exception e) {
            log.error("Error al generar reporte de servicios: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/notificaciones")
    public ResponseEntity<byte[]> reporteNotificaciones(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            byte[] pdf = reporteService.generarReporteNotificaciones(fechaInicio, fechaFin);
            return generarDescarga("notificaciones", pdf);
        } catch (Exception e) {
            log.error("Error al generar reporte de notificaciones: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/productos-populares")
    public ResponseEntity<byte[]> reporteProductosPopulares(
            @RequestParam(defaultValue = "general") String periodo) {
        try {
            byte[] pdf = reporteService.generarReporteProductosPopulares(periodo);
            return generarDescarga("populares_" + periodo, pdf);
        } catch (Exception e) {
            log.error("Error al generar reporte de productos populares: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<byte[]> generarDescarga(String tipo, byte[] pdf) {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String nombre = "SionTrack_" + tipo + "_" + fecha + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}
