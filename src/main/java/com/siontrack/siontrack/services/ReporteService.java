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
import com.siontrack.siontrack.models.Notificaciones;

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

    private final ClienteServicios clienteServicios;
    private final ProductosServicios productosServicios;
    private final ProveedoresService proveedoresService;
    private final ServiciosService serviciosService;
    private final NotificacionesService notificacionesService;

    // Paleta minimalista — fondo blanco, acentos sutiles
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

    public ReporteService(ClienteServicios clienteServicios,
                          ProductosServicios productosServicios,
                          ProveedoresService proveedoresService,
                          ServiciosService serviciosService,
                          NotificacionesService notificacionesService) {
        this.clienteServicios = clienteServicios;
        this.productosServicios = productosServicios;
        this.proveedoresService = proveedoresService;
        this.serviciosService = serviciosService;
        this.notificacionesService = notificacionesService;
    }

    // =============================================
    // REPORTES
    // =============================================

    @Transactional(readOnly = true)
    public byte[] generarReporteClientes() throws Exception {
        List<ClienteResponseDTO> clientes = clienteServicios.obtenerListaClientes();

        String[] enc = {"Nombre", "Cédula / NIT", "Tipo", "Teléfono", "Email", "Notificaciones"};
        float[] anc = {3f, 2f, 1.3f, 2f, 2.5f, 1.5f};

        return generarPdf("Clientes", "Directorio completo de clientes registrados",
                enc, anc, clientes.size(), (tabla, fontBase, i) -> {
            ClienteResponseDTO c = clientes.get(i);
            boolean par = i % 2 == 0;

            agregarCelda(tabla, c.getNombre(), par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, c.getCedula_ruc(), par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, formatTipoCliente(c.getTipo_cliente()), par, TextAlignment.CENTER, fontBase);
            agregarCelda(tabla, extraerTelefono(c.getTelefonos()), par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, extraerEmail(c.getCorreos()), par, TextAlignment.LEFT, fontBase);
            agregarCeldaColor(tabla, c.isRecibe_notificaciones() ? "Activo" : "Inactivo", par,
                    c.isRecibe_notificaciones() ? VERDE : GRIS_CLARO, fontBase);
        });
    }

    @Transactional(readOnly = true)
    public byte[] generarReporteProductos() throws Exception {
        List<ProductosResponseDTO> productos = productosServicios.obtenerListaProductos();

        String[] enc = {"Código", "Nombre", "Categoría", "Proveedor", "Precio Venta", "Stock"};
        float[] anc = {1.5f, 3f, 2f, 2f, 1.5f, 1f};

        return generarPdf("Productos", "Inventario completo con precios y niveles de stock",
                enc, anc, productos.size(), (tabla, fontBase, i) -> {
            ProductosResponseDTO p = productos.get(i);
            boolean par = i % 2 == 0;

            agregarCelda(tabla, p.getCodigo_producto(), par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, p.getNombre(), par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, p.getCategoria(), par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, p.getProveedor() != null ? p.getProveedor().getNombre() : "—", par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, formatMoneda(p.getPrecio_venta()), par, TextAlignment.RIGHT, fontBase);
            agregarCeldaColor(tabla, p.getCantidad_disponible() != null ? String.valueOf(p.getCantidad_disponible()) : "—",
                    par, p.isAlerta_stock() ? ROJO : GRIS_TEXTO, fontBase);
        });
    }

    @Transactional(readOnly = true)
    public byte[] generarReporteProveedores() throws Exception {
        List<ProveedoresResponseDTO> proveedores = proveedoresService.obtenerListaProveedores();

        String[] enc = {"ID", "Nombre", "Teléfono", "Email", "Contacto"};
        float[] anc = {0.7f, 3f, 2f, 3f, 2.5f};

        return generarPdf("Proveedores", "Directorio de proveedores y contactos",
                enc, anc, proveedores.size(), (tabla, fontBase, i) -> {
            ProveedoresResponseDTO p = proveedores.get(i);
            boolean par = i % 2 == 0;

            agregarCelda(tabla, String.valueOf(p.getProveedor_id()), par, TextAlignment.CENTER, fontBase);
            agregarCelda(tabla, p.getNombre(), par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, safe(p.getTelefono()), par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, safe(p.getEmail()), par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, safe(p.getNombre_contacto()), par, TextAlignment.LEFT, fontBase);
        });
    }

    @Transactional(readOnly = true)
    public byte[] generarReporteServicios() throws Exception {
        List<ServicioResponseDTO> servicios = serviciosService.obtenerTodos();

        String[] enc = {"Fecha", "Cliente", "Vehículo", "Kilometraje", "Total", "Tipo"};
        float[] anc = {1.5f, 2.5f, 1.8f, 1.3f, 1.5f, 1.5f};

        return generarPdf("Servicios", "Historial completo de servicios realizados",
                enc, anc, servicios.size(), (tabla, fontBase, i) -> {
            ServicioResponseDTO s = servicios.get(i);
            boolean par = i % 2 == 0;

            agregarCelda(tabla, formatFecha(s.getFecha_servicio()), par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, s.getCliente() != null ? s.getCliente().getNombre() : "—", par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, s.getVehiculo() != null ? s.getVehiculo().getPlaca() : "—", par, TextAlignment.LEFT, fontBase);
            agregarCelda(tabla, safe(s.getKilometraje_servicio()), par, TextAlignment.CENTER, fontBase);
            agregarCelda(tabla, formatMoneda(s.getTotal()), par, TextAlignment.RIGHT, fontBase);
            agregarCelda(tabla, safe(s.getTipo_servicio()), par, TextAlignment.CENTER, fontBase);
        });
    }

    @Transactional(readOnly = true)
    public byte[] generarReporteNotificaciones() throws Exception {
        List<Notificaciones> promociones = notificacionesService.obtenerPromocionesEnviadas();
        List<Notificaciones> recordatorios = notificacionesService.obtenerRecordatorios();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4.rotate());
        doc.setMargins(40, 40, 50, 40);

        PdfFont fontBase = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new PiePaginaHandler(fontBase));

        int total = promociones.size() + recordatorios.size();
        agregarEncabezado(doc, "Notificaciones",
                "Registro de promociones y recordatorios", total, fontBase, fontBold);

        // Promociones
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

        // Recordatorios
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

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(40, 40, 50, 40);

        PdfFont fontBase = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

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

                    // Contenedor interior con borde
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

    @FunctionalInterface
    private interface FilaCallback {
        void agregar(Table tabla, PdfFont fontBase, int indice);
    }

    private byte[] generarPdf(String titulo, String descripcion,
                               String[] encabezados, float[] anchos,
                               int totalRegistros, FilaCallback callback) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4.rotate());
        doc.setMargins(40, 40, 50, 40);

        PdfFont fontBase = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new PiePaginaHandler(fontBase));

        agregarEncabezado(doc, titulo, descripcion, totalRegistros, fontBase, fontBold);

        Table tabla = crearTabla(anchos, encabezados, fontBold);

        for (int i = 0; i < totalRegistros; i++) {
            callback.agregar(tabla, fontBase, i);
        }

        doc.add(tabla);
        doc.close();
        return baos.toByteArray();
    }

    /**
     * Encabezado del documento: línea dorada, nombre empresa, título, metadata
     */
    private void agregarEncabezado(Document doc, String titulo, String descripcion,
                                    int totalRegistros, PdfFont fontBase, PdfFont fontBold) {
        // Línea dorada superior
        Table linea = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        linea.addCell(new Cell().setHeight(2.5f).setBackgroundColor(ACENTO).setBorder(Border.NO_BORDER));
        doc.add(linea);

        // Header layout: izquierda (marca + título) | derecha (metadata)
        Table header = new Table(UnitValue.createPercentArray(new float[]{1f, 1f}))
                .useAllAvailableWidth().setMarginTop(14).setMarginBottom(6);

        // Columna izquierda
        Cell izq = new Cell().setBorder(Border.NO_BORDER);
        izq.add(new Paragraph("SIONTRACK")
                .setFont(fontBold).setFontSize(9).setFontColor(ACENTO)
                .setCharacterSpacing(3).setMarginBottom(1));
        izq.add(new Paragraph(titulo)
                .setFont(fontBold).setFontSize(18).setFontColor(NEGRO_TITULO)
                .setMarginBottom(0));
        header.addCell(izq);

        // Columna derecha — metadata
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

        // Subtítulo: descripción + contador
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

    /**
     * Título de sección dentro del documento
     */
    private void agregarTituloSeccion(Document doc, String titulo, int cantidad, PdfFont fontBold) {
        Table sec = new Table(UnitValue.createPercentArray(new float[]{0.03f, 1f}))
                .useAllAvailableWidth().setMarginTop(20).setMarginBottom(8);

        // Indicador vertical dorado
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
    // PIE DE PÁGINA CON NÚMERO DE PÁGINA
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

            // Línea separadora del pie
            pdfCanvas.setStrokeColor(new DeviceRgb(229, 231, 235))
                    .setLineWidth(0.5f)
                    .moveTo(area.getLeft() + 40, area.getBottom() + 36)
                    .lineTo(area.getRight() - 40, area.getBottom() + 36)
                    .stroke();

            // Texto izquierdo: marca
            Canvas canvas = new Canvas(pdfCanvas, area);
            canvas.showTextAligned(
                    new Paragraph("SionTrack — Confidencial")
                            .setFont(font).setFontSize(7)
                            .setFontColor(new DeviceRgb(156, 163, 175)),
                    area.getLeft() + 40, area.getBottom() + 22,
                    TextAlignment.LEFT);

            // Texto derecho: número de página
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

    private String extraerTelefono(List<TelefonosResponseDTO> telefonos) {
        if (telefonos == null || telefonos.isEmpty()) return "—";
        return telefonos.get(0).getTelefono();
    }

    private String extraerEmail(List<CorreosResponseDTO> correos) {
        if (correos == null || correos.isEmpty()) return "—";
        return correos.get(0).getCorreo();
    }
}
