package com.siontrack.siontrack.controllers;

import com.siontrack.siontrack.services.ReporteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

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

/**
 * API REST para la descarga de reportes en PDF.
 * Cada endpoint genera el documento bajo demanda y lo devuelve como adjunto
 * con el nombre {@code SionTrack_{tipo}_{yyyyMMdd}.pdf}.
 */
@Tag(name = "Reportes", description = "Generación y descarga de reportes en formato PDF")
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private static final Logger log = LoggerFactory.getLogger(ReporteController.class);

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    /**
     * Genera el reporte de clientes registrados en el rango de fechas indicado.
     */
    @Operation(
        summary = "Reporte de clientes",
        description = "Genera un PDF con el listado de clientes registrados en el rango de fechas. "
                    + "Incluye nombre, cédula, tipo, teléfono, email y estado de notificaciones."
    )
    @ApiResponse(responseCode = "200", description = "PDF generado",
                 content = @Content(mediaType = "application/pdf"))
    @GetMapping("/clientes")
    public ResponseEntity<byte[]> reporteClientes(
            @Parameter(description = "Fecha de inicio (formato ISO: yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @Parameter(description = "Fecha de fin (formato ISO: yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            byte[] pdf = reporteService.generarReporteClientes(fechaInicio, fechaFin);
            return generarDescarga("clientes", pdf);
        } catch (Exception e) {
            log.error("Error al generar reporte de clientes: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Genera el reporte de productos comprados en el rango de fechas indicado.
     */
    @Operation(
        summary = "Reporte de productos",
        description = "Genera un PDF con el catálogo de productos comprados en el rango de fechas. "
                    + "Resalta en rojo los productos con stock bajo."
    )
    @ApiResponse(responseCode = "200", description = "PDF generado",
                 content = @Content(mediaType = "application/pdf"))
    @GetMapping("/productos")
    public ResponseEntity<byte[]> reporteProductos(
            @Parameter(description = "Fecha de inicio (formato ISO: yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @Parameter(description = "Fecha de fin (formato ISO: yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            byte[] pdf = reporteService.generarReporteProductos(fechaInicio, fechaFin);
            return generarDescarga("productos", pdf);
        } catch (Exception e) {
            log.error("Error al generar reporte de productos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Genera el reporte de proveedores con productos comprados en el rango de fechas.
     */
    @Operation(
        summary = "Reporte de proveedores",
        description = "Genera un PDF con los proveedores que tienen productos comprados en el rango de fechas. "
                    + "El filtro se aplica a través de la fecha de compra de los productos."
    )
    @ApiResponse(responseCode = "200", description = "PDF generado",
                 content = @Content(mediaType = "application/pdf"))
    @GetMapping("/proveedores")
    public ResponseEntity<byte[]> reporteProveedores(
            @Parameter(description = "Fecha de inicio (formato ISO: yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @Parameter(description = "Fecha de fin (formato ISO: yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            byte[] pdf = reporteService.generarReporteProveedores(fechaInicio, fechaFin);
            return generarDescarga("proveedores", pdf);
        } catch (Exception e) {
            log.error("Error al generar reporte de proveedores: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Genera el reporte de servicios prestados en el rango de fechas indicado.
     */
    @Operation(
        summary = "Reporte de servicios",
        description = "Genera un PDF con el historial de servicios del rango de fechas indicado."
    )
    @ApiResponse(responseCode = "200", description = "PDF generado",
                 content = @Content(mediaType = "application/pdf"))
    @GetMapping("/servicios")
    public ResponseEntity<byte[]> reporteServicios(
            @Parameter(description = "Fecha de inicio (formato ISO: yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @Parameter(description = "Fecha de fin (formato ISO: yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            byte[] pdf = reporteService.generarReporteServicios(fechaInicio, fechaFin);
            return generarDescarga("servicios", pdf);
        } catch (Exception e) {
            log.error("Error al generar reporte de servicios: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Genera el reporte de notificaciones del rango de fechas indicado.
     */
    @Operation(
        summary = "Reporte de notificaciones",
        description = "Genera un PDF con el historial de notificaciones (promociones y recordatorios) "
                    + "del rango de fechas indicado, presentadas en secciones separadas."
    )
    @ApiResponse(responseCode = "200", description = "PDF generado",
                 content = @Content(mediaType = "application/pdf"))
    @GetMapping("/notificaciones")
    public ResponseEntity<byte[]> reporteNotificaciones(
            @Parameter(description = "Fecha de inicio (formato ISO: yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @Parameter(description = "Fecha de fin (formato ISO: yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            byte[] pdf = reporteService.generarReporteNotificaciones(fechaInicio, fechaFin);
            return generarDescarga("notificaciones", pdf);
        } catch (Exception e) {
            log.error("Error al generar reporte de notificaciones: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Genera el reporte de productos más populares para el período indicado.
     */
    @Operation(
        summary = "Reporte de productos populares",
        description = "Genera un PDF con el ranking de los 50 productos más vendidos en el período. "
                    + "Períodos válidos: semana, mes, trimestre, anio, general."
    )
    @ApiResponse(responseCode = "200", description = "PDF generado",
                 content = @Content(mediaType = "application/pdf"))
    @GetMapping("/productos-populares")
    public ResponseEntity<byte[]> reporteProductosPopulares(
            @Parameter(description = "Período: semana | mes | trimestre | anio | general")
            @RequestParam(defaultValue = "general") String periodo) {
        try {
            byte[] pdf = reporteService.generarReporteProductosPopulares(periodo);
            return generarDescarga("populares_" + periodo, pdf);
        } catch (Exception e) {
            log.error("Error al generar reporte de productos populares: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Construye la respuesta HTTP con el PDF como adjunto descargable.
     * El nombre del archivo sigue el patrón {@code SionTrack_{tipo}_{yyyyMMdd}.pdf}.
     */
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
