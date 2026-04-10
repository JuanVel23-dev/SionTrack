package com.siontrack.siontrack.services;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.io.font.constants.StandardFonts;

import com.siontrack.siontrack.DTO.Response.*;
import com.siontrack.siontrack.models.*;
import com.siontrack.siontrack.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReporteService {

    private final ClienteRepository clienteRepository;
    private final ProductosRepository productosRepository;
    private final ServiciosRepository serviciosRepository;
    private final NotificacionesRepository notificacionesRepository;
    private final ProveedoresRepository proveedoresRepository;
    private final ProductosServicios productosServicios;

    // Paleta minimalista
    private static final DeviceRgb BLANCO = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb GRIS_98 = new DeviceRgb(250, 250, 250);
    private static final DeviceRgb GRIS_95 = new DeviceRgb(243, 244, 246);
    private static final DeviceRgb GRIS_BORDE = new DeviceRgb(229, 231, 235);
    private static final DeviceRgb GRIS_TEXTO = new DeviceRgb(55, 65, 81);
    private static final DeviceRgb GRIS_SUTIL = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb GRIS_CLARO = new DeviceRgb(156, 163, 175);
    private static final DeviceRgb NEGRO_TITULO = new DeviceRgb(17, 24, 39);
    private static final DeviceRgb ACENTO = new DeviceRgb(202, 164, 38);
    private static final DeviceRgb ACENTO_OSCURO = new DeviceRgb(161, 131, 30);
    private static final DeviceRgb VERDE = new DeviceRgb(22, 163, 74);
    private static final DeviceRgb ROJO = new DeviceRgb(220, 38, 38);
    private static final DeviceRgb AZUL = new DeviceRgb(37, 99, 235);
    private static final DeviceRgb NARANJA = new DeviceRgb(234, 88, 12);

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ReporteService(ClienteRepository clienteRepository,
                          ProductosRepository productosRepository,
                          ServiciosRepository serviciosRepository,
                          NotificacionesRepository notificacionesRepository,
                          ProveedoresRepository proveedoresRepository,
                          ProductosServicios productosServicios) {
        this.clienteRepository = clienteRepository;
        this.productosRepository = productosRepository;
        this.serviciosRepository = serviciosRepository;
        this.notificacionesRepository = notificacionesRepository;
        this.proveedoresRepository = proveedoresRepository;
        this.productosServicios = productosServicios;
    }

    // =============================================
    // REPORTES
    // =============================================

    @Transactional(readOnly = true)
    public byte[] generarReporteClientes(LocalDate fechaInicio, LocalDate fechaFin) throws Exception {
        // Dos queries separadas para evitar MultipleBagFetchException (telefonos + correos son List)
        // Hibernate fusiona ambas colecciones vía caché L1 dentro de la misma transacción
        List<Clientes> clientes = clienteRepository.findParaReporteConTelefonosPorFechas(fechaInicio, fechaFin);
        clienteRepository.findParaReporteConCorreosPorFechas(fechaInicio, fechaFin);

        PdfFont fontBase = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4.rotate());
        doc.setMargins(40, 40, 50, 40);
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new PiePaginaHandler(fontBase));

        String descripcion = "Clientes registrados del " + fechaInicio.format(FMT_FECHA) + " al " + fechaFin.format(FMT_FECHA);
        agregarEncabezado(doc, "Clientes", descripcion, clientes.size(), fontBase, fontBold);

        String[] enc = {"Nombre", "Cédula / NIT", "Tipo", "Teléfono", "Email", "Notificaciones"};
        float[] anc = {3f, 2f, 1.3f, 2f, 2.5f, 1.5f};
        Table tabla = crearTabla(anc, enc, fontBold);

        for (int i = 0; i < clientes.size(); i++) {
            Clientes c = clientes.get(i);
            boolean par = i % 2 == 0;
            agregarCelda(tabla, safe(c.getNombre()), par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, safe(c.getCedula_ruc()), par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, formatTipoCliente(c.getTipo_cliente()), par, TextAlignment.CENTER, fontBase);
            agregarCelda(tabla, extraerTelefonoEntidad(c.getTelefonos()), par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, extraerCorreoEntidad(c.getCorreos()), par, TextAlignment.LEFT, fontBase);
            boolean recibe = Boolean.TRUE.equals(c.getRecibeNotificaciones());
            agregarCeldaColor(tabla, recibe ? "Activo" : "Inactivo", par,
                    recibe ? VERDE : GRIS_CLARO, fontBase);
        }

        doc.add(tabla);
        doc.close();
        return baos.toByteArray();
    }

    @Transactional(readOnly = true)
    public byte[] generarReporteProductos(LocalDate fechaInicio, LocalDate fechaFin) throws Exception {
        List<Productos> productos = productosRepository.findAllParaReportePorFechas(fechaInicio, fechaFin);

        PdfFont fontBase = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4.rotate());
        doc.setMargins(40, 40, 50, 40);
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new PiePaginaHandler(fontBase));

        String descripcion = "Productos comprados del " + fechaInicio.format(FMT_FECHA) + " al " + fechaFin.format(FMT_FECHA);
        agregarEncabezado(doc, "Productos", descripcion, productos.size(), fontBase, fontBold);

        String[] enc = {"Código", "Nombre", "Categoría", "Proveedor", "Precio Venta", "Stock"};
        float[] anc = {1.5f, 3f, 2f, 2f, 1.5f, 1f};
        Table tabla = crearTabla(anc, enc, fontBold);

        for (int i = 0; i < productos.size(); i++) {
            Productos prod = productos.get(i);
            boolean par = i % 2 == 0;
            agregarCelda(tabla, safe(prod.getCodigoProducto()), par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, safe(prod.getNombre()), par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, safe(prod.getCategoria()), par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, prod.getProveedor() != null ? prod.getProveedor().getNombre() : "—", par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, formatMoneda(prod.getPrecio_venta()), par, TextAlignment.RIGHT, fontBase);

            boolean alertaStock = false;
            String stockTexto = "—";
            if (prod.getInventario() != null) {
                Integer cant = prod.getInventario().getCantidad_disponible();
                Integer minimo = prod.getInventario().getStock_minimo();
                stockTexto = cant != null ? String.valueOf(cant) : "—";
                alertaStock = minimo != null && cant != null && cant <= minimo;
            }
            agregarCeldaColor(tabla, stockTexto, par, alertaStock ? ROJO : GRIS_TEXTO, fontBase);
        }

        doc.add(tabla);
        doc.close();
        return baos.toByteArray();
    }

    @Transactional(readOnly = true)
    public byte[] generarReporteProveedores(LocalDate fechaInicio, LocalDate fechaFin) throws Exception {
        // Proveedores no tiene campo de fecha propio; se filtran por productos comprados en el rango
        List<Proveedores> proveedores = proveedoresRepository.findProveedoresConProductosEnRango(fechaInicio, fechaFin);

        PdfFont fontBase = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4.rotate());
        doc.setMargins(40, 40, 50, 40);
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new PiePaginaHandler(fontBase));

        String descripcion = "Proveedores con productos del " + fechaInicio.format(FMT_FECHA) + " al " + fechaFin.format(FMT_FECHA);
        agregarEncabezado(doc, "Proveedores", descripcion, proveedores.size(), fontBase, fontBold);

        String[] enc = {"ID", "Nombre", "Teléfono", "Email", "Contacto"};
        float[] anc = {0.7f, 3f, 2f, 3f, 2.5f};
        Table tabla = crearTabla(anc, enc, fontBold);

        for (int i = 0; i < proveedores.size(); i++) {
            Proveedores p = proveedores.get(i);
            boolean par = i % 2 == 0;
            agregarCelda(tabla, String.valueOf(p.getProveedor_id()), par, TextAlignment.CENTER, fontBase);
            agregarCelda(tabla, safe(p.getNombre()), par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, safe(p.getTelefono()), par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, safe(p.getEmail()), par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, safe(p.getNombre_contacto()), par, TextAlignment.LEFT, fontBase);
        }

        doc.add(tabla);
        doc.close();
        return baos.toByteArray();
    }

    @Transactional(readOnly = true)
    public byte[] generarReporteServicios(LocalDate fechaInicio, LocalDate fechaFin) throws Exception {
        List<Servicios> servicios = serviciosRepository.findAllParaReportePorFechas(fechaInicio, fechaFin);

        PdfFont fontBase = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4.rotate());
        doc.setMargins(40, 40, 50, 40);
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new PiePaginaHandler(fontBase));

        String descripcion = "Servicios del " + fechaInicio.format(FMT_FECHA) + " al " + fechaFin.format(FMT_FECHA);
        agregarEncabezado(doc, "Servicios", descripcion, servicios.size(), fontBase, fontBold);

        String[] enc = {"Fecha", "Cliente", "Vehículo", "Kilometraje", "Total", "Tipo"};
        float[] anc = {1.5f, 2.5f, 1.8f, 1.3f, 1.5f, 1.5f};
        Table tabla = crearTabla(anc, enc, fontBold);

        for (int i = 0; i < servicios.size(); i++) {
            Servicios s = servicios.get(i);
            boolean par = i % 2 == 0;
            agregarCelda(tabla, formatFecha(s.getFecha_servicio()), par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, s.getClientes() != null ? s.getClientes().getNombre() : "—", par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, s.getVehiculos() != null ? s.getVehiculos().getPlaca() : "—", par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, safe(s.getKilometraje_servicio()), par, TextAlignment.CENTER, fontBase);
            agregarCelda(tabla, formatMoneda(s.getTotal()), par, TextAlignment.RIGHT, fontBase);
            agregarCelda(tabla, safe(s.getTipo_servicio()), par, TextAlignment.CENTER, fontBase);
        }

        doc.add(tabla);
        doc.close();
        return baos.toByteArray();
    }

    @Transactional(readOnly = true)
    public byte[] generarReporteNotificaciones(LocalDate fechaInicio, LocalDate fechaFin) throws Exception {
        LocalDateTime desde = fechaInicio.atStartOfDay();
        LocalDateTime hasta = fechaFin.plusDays(1).atStartOfDay();

        List<Notificaciones> promociones = notificacionesRepository.findPromocionesParaReportePorFechas(desde, hasta);
        List<Notificaciones> recordatorios = notificacionesRepository.findRecordatoriosParaReportePorFechas(desde, hasta);

        PdfFont fontBase = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4.rotate());
        doc.setMargins(40, 40, 50, 40);
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new PiePaginaHandler(fontBase));

        String descripcion = "Notificaciones del " + fechaInicio.format(FMT_FECHA) + " al " + fechaFin.format(FMT_FECHA);
        agregarEncabezado(doc, "Notificaciones", descripcion,
                promociones.size() + recordatorios.size(), fontBase, fontBold);

        // Seccion: Promociones
        if (!promociones.isEmpty()) {
            agregarTituloSeccion(doc, "Promociones", promociones.size(), fontBold);

            String[] encP = {"Cliente", "Promoción", "Estado", "Resultado", "Fecha Envío"};
            float[] ancP = {2.5f, 4f, 1.2f, 1.5f, 1.5f};
            Table t = crearTabla(ancP, encP, fontBold);

            for (int i = 0; i < promociones.size(); i++) {
                Notificaciones n = promociones.get(i);
                boolean par = i % 2 == 0;
                agregarCelda(t, n.getClientes() != null ? n.getClientes().getNombre() : "—", par, TextAlignment.LEFT, fontBase);
                agregarCelda(t, safe(n.getMensaje_enviado()), par, TextAlignment.LEFT, fontBase);
                agregarCeldaEstado(t, n.getEstado(), par, fontBase);
                agregarCelda(t, safe(n.getResultadoEnvio()), par, TextAlignment.CENTER, fontBase);
                agregarCelda(t, n.getFecha_envio() != null ? n.getFecha_envio().toLocalDateTime().format(FMT_FECHA) : "—",
                        par, TextAlignment.CENTER, fontBase);
            }
            doc.add(t);
        }

        // Seccion: Recordatorios
        if (!recordatorios.isEmpty()) {
            agregarTituloSeccion(doc, "Recordatorios", recordatorios.size(), fontBold);

            String[] encR = {"Cliente", "Servicio", "Kilometraje", "Estado", "Programado"};
            float[] ancR = {3f, 2.5f, 1.5f, 1.2f, 1.5f};
            Table t = crearTabla(ancR, encR, fontBold);

            for (int i = 0; i < recordatorios.size(); i++) {
                Notificaciones n = recordatorios.get(i);
                boolean par = i % 2 == 0;
                agregarCelda(t, n.getClientes() != null ? n.getClientes().getNombre() : "—", par, TextAlignment.LEFT, fontBase);
                agregarCelda(t, safe(n.getNombreServicio()), par, TextAlignment.LEFT, fontBase);
                agregarCelda(t, safe(n.getKilometrajeServicio()), par, TextAlignment.CENTER, fontBase);
                agregarCeldaEstado(t, n.getEstado(), par, fontBase);
                agregarCelda(t, n.getFecha_programada() != null
                        ? n.getFecha_programada().toLocalDateTime().format(FMT_FECHA) : "—",
                        par, TextAlignment.CENTER, fontBase);
            }
            doc.add(t);
        }

        doc.close();
        return baos.toByteArray();
    }

    @Transactional(readOnly = true)
    public byte[] generarReporteProductosPopulares(String periodo) throws Exception {
        List<ProductoPopularDTO> populares = productosServicios.obtenerListaPopulares(50, periodo);

        String periodoTexto = switch (periodo.toLowerCase()) {
            case "semana" -> "Última Semana";
            case "mes" -> "Último Mes";
            case "trimestre" -> "Último Trimestre";
            case "anio" -> "Último Año";
            default -> "Histórico General";
        };

        PdfFont fontBase = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(40, 40, 50, 40);
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new PiePaginaHandler(fontBase));

        agregarEncabezado(doc, "Productos Populares",
                "Período: " + periodoTexto, populares.size(), fontBase, fontBold);

        if (populares.isEmpty()) {
            doc.add(new Paragraph("No se encontraron productos vendidos en este período.")
                    .setFont(fontBase).setFontSize(10).setFontColor(GRIS_SUTIL)
                    .setMarginTop(30).setTextAlignment(TextAlignment.CENTER));
        } else {
            // Top 3 destacado
            if (populares.size() >= 3) {
                Table top = new Table(UnitValue.createPercentArray(new float[]{1f, 1f, 1f}))
                        .useAllAvailableWidth().setMarginBottom(20);

                String[] medallas = {"1er Lugar", "2do Lugar", "3er Lugar"};
                DeviceRgb[] colores = {ACENTO, AZUL, VERDE};

                for (int i = 0; i < 3; i++) {
                    ProductoPopularDTO p = populares.get(i);
                    Cell c = new Cell().setBorder(Border.NO_BORDER).setPadding(8);

                    Table inner = new Table(1).useAllAvailableWidth();
                    Cell innerCell = new Cell().setBorder(new SolidBorder(GRIS_BORDE, 0.5f))
                            .setBorderTop(new SolidBorder(colores[i], 2.5f))
                            .setBackgroundColor(BLANCO)
                            .setPadding(14)
                            .setTextAlignment(TextAlignment.CENTER);

                    innerCell.add(new Paragraph(medallas[i]).setFont(fontBase).setFontSize(8)
                            .setFontColor(colores[i]).setMarginBottom(3));
                    innerCell.add(new Paragraph(p.getNombre()).setFont(fontBold).setFontSize(11)
                            .setFontColor(NEGRO_TITULO).setMarginBottom(2));
                    innerCell.add(new Paragraph(safe(p.getCategoria())).setFont(fontBase).setFontSize(8)
                            .setFontColor(GRIS_SUTIL).setMarginBottom(6));
                    innerCell.add(new Paragraph(String.valueOf(p.getTotalVendido()) + " unidades")
                            .setFont(fontBold).setFontSize(14).setFontColor(colores[i]));

                    inner.addCell(innerCell);
                    c.add(inner);
                    top.addCell(c);
                }
                doc.add(top);
            }

            // Tabla completa
            agregarTituloSeccion(doc, "Ranking Completo", populares.size(), fontBold);

            String[] enc = {"#", "Producto", "Categoría", "Unidades Vendidas"};
            float[] anc = {0.5f, 4f, 2.5f, 2f};
            Table tabla = crearTabla(anc, enc, fontBold);

            for (int i = 0; i < populares.size(); i++) {
                ProductoPopularDTO p = populares.get(i);
                boolean par = i % 2 == 0;

                DeviceRgb colorPos = i < 3 ? ACENTO_OSCURO : GRIS_TEXTO;
                agregarCeldaColor(tabla, String.valueOf(i + 1), par, colorPos, fontBase);
                agregarCelda(tabla, p.getNombre(), par, TextAlignment.LEFT, fontBase);
                agregarCelda(tabla, safe(p.getCategoria()), par, TextAlignment.LEFT, fontBase);
                agregarCeldaColor(tabla, String.valueOf(p.getTotalVendido()), par, ACENTO_OSCURO, fontBase);
            }
            doc.add(tabla);
        }

        doc.close();
        return baos.toByteArray();
    }

    // =============================================
    // CONSTRUCCIÓN DEL DOCUMENTO
    // =============================================

    private void agregarEncabezado(Document doc, String titulo, String descripcion,
                                   int totalRegistros, PdfFont fontBase, PdfFont fontBold) {
        // Línea dorada superior
        Table linea = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        linea.addCell(new Cell().setHeight(2.5f).setBackgroundColor(ACENTO).setBorder(Border.NO_BORDER));
        doc.add(linea);

        // Header layout
        Table header = new Table(UnitValue.createPercentArray(new float[]{1f, 1f}))
                .useAllAvailableWidth().setMarginTop(14).setMarginBottom(6);

        Cell izq = new Cell().setBorder(Border.NO_BORDER);
        izq.add(new Paragraph("SIONTRACK")
                .setFont(fontBold).setFontSize(9).setFontColor(ACENTO)
                .setCharacterSpacing(3).setMarginBottom(1));
        izq.add(new Paragraph(titulo)
                .setFont(fontBold).setFontSize(18).setFontColor(NEGRO_TITULO)
                .setMarginBottom(0));
        header.addCell(izq);

        Cell der = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT)
                .setVerticalAlignment(VerticalAlignment.BOTTOM);
        der.add(new Paragraph("Grupo Sion S.A.S")
                .setFont(fontBase).setFontSize(8).setFontColor(GRIS_SUTIL));
        der.add(new Paragraph(LocalDateTime.now().format(FMT_FECHA_HORA))
                .setFont(fontBase).setFontSize(8).setFontColor(GRIS_CLARO));
        header.addCell(der);

        doc.add(header);

        // Línea separadora fina
        Table sep = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        sep.addCell(new Cell().setHeight(0.5f).setBackgroundColor(GRIS_BORDE).setBorder(Border.NO_BORDER));
        doc.add(sep);

        // Subtítulo
        Table sub = new Table(UnitValue.createPercentArray(new float[]{1f, 1f}))
                .useAllAvailableWidth().setMarginTop(8).setMarginBottom(14);

        Cell subIzq = new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(descripcion).setFont(fontBase).setFontSize(9).setFontColor(GRIS_SUTIL));
        Cell subDer = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph(totalRegistros + " registros").setFont(fontBase).setFontSize(9).setFontColor(GRIS_CLARO));
        sub.addCell(subIzq);
        sub.addCell(subDer);
        doc.add(sub);
    }

    private void agregarTituloSeccion(Document doc, String titulo, int cantidad, PdfFont fontBold) {
        Table sec = new Table(UnitValue.createPercentArray(new float[]{0.03f, 1f}))
                .useAllAvailableWidth().setMarginTop(20).setMarginBottom(8);

        sec.addCell(new Cell().setBackgroundColor(ACENTO).setBorder(Border.NO_BORDER).setWidth(3));
        sec.addCell(new Cell().setBorder(Border.NO_BORDER).setPaddingLeft(8)
                .add(new Paragraph(titulo + "  (" + cantidad + ")")
                        .setFont(fontBold).setFontSize(11).setFontColor(NEGRO_TITULO)));

        doc.add(sec);
    }

    // =============================================
    // TABLA Y CELDAS
    // =============================================

    private Table crearTabla(float[] anchos, String[] encabezados, PdfFont fontBold) {
        Table tabla = new Table(UnitValue.createPercentArray(anchos)).useAllAvailableWidth();

        for (String enc : encabezados) {
            Cell cell = new Cell()
                    .add(new Paragraph(enc).setFont(fontBold).setFontSize(8).setFontColor(GRIS_SUTIL))
                    .setBackgroundColor(GRIS_98)
                    .setBorderTop(new SolidBorder(GRIS_BORDE, 0.5f))
                    .setBorderBottom(new SolidBorder(GRIS_BORDE, 0.5f))
                    .setBorderLeft(Border.NO_BORDER)
                    .setBorderRight(Border.NO_BORDER)
                    .setPaddingTop(7).setPaddingBottom(7)
                    .setPaddingLeft(8).setPaddingRight(8)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            tabla.addHeaderCell(cell);
        }
        return tabla;
    }

    private void agregarCelda(Table tabla, String texto, boolean par,
                              TextAlignment alineacion, PdfFont fontBase) {
        Cell cell = new Cell()
                .add(new Paragraph(safe(texto)).setFont(fontBase).setFontSize(8.5f).setFontColor(GRIS_TEXTO))
                .setBackgroundColor(par ? BLANCO : GRIS_95)
                .setTextAlignment(alineacion)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(GRIS_BORDE, 0.25f))
                .setPaddingTop(6).setPaddingBottom(6)
                .setPaddingLeft(8).setPaddingRight(8)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        tabla.addCell(cell);
    }

    private void agregarCeldaColor(Table tabla, String texto, boolean par,
                                   DeviceRgb color, PdfFont fontBase) {
        Cell cell = new Cell()
                .add(new Paragraph(safe(texto)).setFont(fontBase).setFontSize(8.5f).setFontColor(color))
                .setBackgroundColor(par ? BLANCO : GRIS_95)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(GRIS_BORDE, 0.25f))
                .setPaddingTop(6).setPaddingBottom(6)
                .setPaddingLeft(8).setPaddingRight(8)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        tabla.addCell(cell);
    }

    private void agregarCeldaEstado(Table tabla, String estado, boolean par, PdfFont fontBase) {
        DeviceRgb color = switch (estado != null ? estado.toLowerCase() : "") {
            case "enviado" -> VERDE;
            case "pendiente" -> NARANJA;
            case "fallido" -> ROJO;
            case "sin_consentimiento" -> GRIS_CLARO;
            default -> GRIS_SUTIL;
        };

        String texto = switch (estado != null ? estado.toLowerCase() : "") {
            case "enviado" -> "Enviado";
            case "pendiente" -> "Pendiente";
            case "fallido" -> "Fallido";
            case "sin_consentimiento" -> "Sin consentimiento";
            default -> safe(estado);
        };

        agregarCeldaColor(tabla, texto, par, color, fontBase);
    }

    // =============================================
    // PIE DE PÁGINA
    // =============================================

    private static class PiePaginaHandler implements IEventHandler {
        private final PdfFont font;

        PiePaginaHandler(PdfFont font) {
            this.font = font;
        }

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdf = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            int pageNum = pdf.getPageNumber(page);
            int totalPages = pdf.getNumberOfPages();
            Rectangle area = page.getPageSize();

            PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), pdf);

            pdfCanvas.setStrokeColor(new DeviceRgb(229, 231, 235))
                    .setLineWidth(0.5f)
                    .moveTo(area.getLeft() + 40, area.getBottom() + 36)
                    .lineTo(area.getRight() - 40, area.getBottom() + 36)
                    .stroke();

            Canvas canvas = new Canvas(pdfCanvas, area);
            canvas.showTextAligned(
                    new Paragraph("SionTrack — Confidencial")
                            .setFont(font).setFontSize(7)
                            .setFontColor(new DeviceRgb(156, 163, 175)),
                    area.getLeft() + 40, area.getBottom() + 22,
                    TextAlignment.LEFT);

            canvas.showTextAligned(
                    new Paragraph("Página " + pageNum + " de " + totalPages)
                            .setFont(font).setFontSize(7)
                            .setFontColor(new DeviceRgb(156, 163, 175)),
                    area.getRight() - 40, area.getBottom() + 22,
                    TextAlignment.RIGHT);

            canvas.close();
        }
    }

    // =============================================
    // HELPERS
    // =============================================

    private String safe(String valor) {
        return valor != null && !valor.trim().isEmpty() ? valor : "—";
    }

    private String formatMoneda(BigDecimal valor) {
        if (valor == null) return "—";
        return String.format("$%,.0f", valor);
    }

    private String formatFecha(LocalDate fecha) {
        return fecha != null ? fecha.format(FMT_FECHA) : "—";
    }

    private String formatTipoCliente(String tipo) {
        if (tipo == null) return "—";
        return tipo.equalsIgnoreCase("empresa") ? "Empresa" : "Persona";
    }

    private String extraerTelefonoEntidad(List<Cliente_Telefonos> telefonos) {
        if (telefonos == null || telefonos.isEmpty()) return "—";
        return telefonos.get(0).getTelefono();
    }

    private String extraerCorreoEntidad(List<Cliente_Correos> correos) {
        if (correos == null || correos.isEmpty()) return "—";
        return correos.get(0).getCorreo();
    }

}
