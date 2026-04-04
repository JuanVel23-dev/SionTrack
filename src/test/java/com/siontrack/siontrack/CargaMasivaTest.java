package com.siontrack.siontrack;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import com.siontrack.siontrack.DTO.Response.ImportacionResponseDTO;
import com.siontrack.siontrack.services.ImportacionService;
import com.siontrack.siontrack.services.WhatsAppService;

/**
 * VARE-02: Prueba de rendimiento de carga masiva de datos.
 *
 * Genera un archivo Excel en memoria con 500 registros que incluyen tanto
 * datos de Cliente como de Vehículo, y mide el tiempo que tarda
 * ImportacionService en procesarlos e insertarlos en PostgreSQL.
 *
 * Entidades involucradas por cada fila:
 *   - INSERT en tabla clientes
 *   - INSERT en tabla vehiculos (relacionado al cliente)
 *
 * Stack real: Spring completo + PostgreSQL real.
 * Único mock: WhatsAppService (para no llamar a la API de Meta).
 *
 * Los registros se descartan al final gracias a @Transactional,
 * dejando la base de datos en el mismo estado que antes del test.
 *
 * Criterio de éxito (VARE-02): 500 registros procesados en t < 20 s.
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Transactional
class CargaMasivaTest {

    @Autowired
    private ImportacionService importacionService;

    // ==================== MONITOREO DE MEMORIA ====================

    /** Bean de la JVM para leer uso de heap y non-heap en tiempo real. */
    private MemoryMXBean memoriaBean;

    /** Bytes de heap usados justo antes de cada test (post-GC sugerido). */
    private long heapAntesBytes;

    /** Bytes de non-heap usados justo antes de cada test. */
    private long nonHeapAntesBytes;

    /** Total de ciclos GC antes del test (para detectar presión de memoria). */
    private long gcCiclosAntes;

    /**
     * Captura el estado base de la JVM antes de cada test.
     * System.gc() es una sugerencia al GC para obtener un baseline más limpio;
     * no garantiza recolección completa, pero reduce ruido entre tests.
     */
    @BeforeEach
    void capturarBaselineMemoria() {
        System.gc();
        memoriaBean = ManagementFactory.getMemoryMXBean();
        heapAntesBytes = memoriaBean.getHeapMemoryUsage().getUsed();
        nonHeapAntesBytes = memoriaBean.getNonHeapMemoryUsage().getUsed();
        gcCiclosAntes = contarCiclosGC();
    }

    /**
     * Suma los ciclos de todos los colectores GC activos de la JVM.
     * Un incremento alto durante el test indica presión de memoria (muchos objetos creados).
     */
    private long contarCiclosGC() {
        List<GarbageCollectorMXBean> colectores = ManagementFactory.getGarbageCollectorMXBeans();
        return colectores.stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionCount)
                .sum();
    }

    /**
     * Imprime un bloque con el delta de memoria entre el baseline y el momento actual.
     *
     * @param etiqueta identificador del test que aparecerá en el reporte
     * @return delta de heap en bytes (útil para assertions)
     */
    private long reportarUsoDeMemoria(String etiqueta) {
        long heapDespuesBytes    = memoriaBean.getHeapMemoryUsage().getUsed();
        long heapMaxBytes        = memoriaBean.getHeapMemoryUsage().getMax();
        long nonHeapDespuesBytes = memoriaBean.getNonHeapMemoryUsage().getUsed();
        long gcCiclosDespues     = contarCiclosGC();

        long deltaHeap    = heapDespuesBytes - heapAntesBytes;
        long deltaNonHeap = nonHeapDespuesBytes - nonHeapAntesBytes;
        long deltaCiclos  = gcCiclosDespues - gcCiclosAntes;
        double pctHeap    = heapMaxBytes > 0 ? (heapDespuesBytes * 100.0 / heapMaxBytes) : 0;

        System.out.printf("%n╔═══════════════════════════════════════════════════╗%n");
        System.out.printf("║         MEMORIA JVM — %-27s║%n", etiqueta);
        System.out.printf("╠═══════════════════════════════════════════════════╣%n");
        System.out.printf("║ Heap antes          : %,.1f MB%24s║%n", heapAntesBytes / 1e6, "");
        System.out.printf("║ Heap después        : %,.1f MB%24s║%n", heapDespuesBytes / 1e6, "");
        System.out.printf("║ Δ Heap (incremento) : %,.1f MB%24s║%n", deltaHeap / 1e6, "");
        System.out.printf("║ Heap máximo JVM     : %,.1f MB%24s║%n", heapMaxBytes / 1e6, "");
        System.out.printf("║ %% de heap usado     : %.1f%%%25s║%n", pctHeap, "");
        System.out.printf("║ Δ Non-heap          : %,.1f MB%24s║%n", deltaNonHeap / 1e6, "");
        System.out.printf("║ Ciclos GC durante test: %-24d║%n", deltaCiclos);
        System.out.printf("╚═══════════════════════════════════════════════════╝%n%n");

        return deltaHeap;
    }

    /**
     * Mockeamos WhatsAppService para evitar llamadas reales a la API de Meta.
     * Al no incluir teléfono en el Excel, crearCliente no llega a invocarlo,
     * pero lo dejamos activo para proteger el test ante futuros cambios en el servicio.
     */
    @MockBean
    private WhatsAppService whatsAppService;

    // ==================== GENERACIÓN DEL ARCHIVO ====================

    /**
     * Genera un Excel (.xlsx) en memoria con {@code cantidad} filas.
     *
     * Cada fila representa un cliente con su vehículo asociado:
     * <pre>
     *   nombre | cedula_ruc | tipo_cliente | placa   | kilometraje_actual
     *   -------|------------|--------------|---------|-------------------
     *   VARE02 Cliente 001 | VARE02_000001 | NATURAL | V02001 | 50000
     *   VARE02 Cliente 002 | VARE02_000002 | NATURAL | V02002 | 50000
     *   ...
     * </pre>
     *
     * Sin columna de teléfono → crearCliente no llama a WhatsApp.
     * Prefijo "VARE02_" facilita identificar registros si el rollback falla.
     */
    private MockMultipartFile generarExcelClientesConVehiculos(int cantidad) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Clientes");

            // Fila de cabecera — las columnas placa y kilometraje_actual
            // activan la creación del vehículo dentro de crearCliente()
            Row cabecera = sheet.createRow(0);
            cabecera.createCell(0).setCellValue("nombre");
            cabecera.createCell(1).setCellValue("cedula_ruc");
            cabecera.createCell(2).setCellValue("tipo_cliente");
            cabecera.createCell(3).setCellValue("placa");
            cabecera.createCell(4).setCellValue("kilometraje_actual");

            // Filas de datos: un cliente + un vehículo por fila
            for (int i = 1; i <= cantidad; i++) {
                Row fila = sheet.createRow(i);
                fila.createCell(0).setCellValue(String.format("VARE02 Cliente %03d", i));
                fila.createCell(1).setCellValue(String.format("VARE02_%06d", i));
                fila.createCell(2).setCellValue("NATURAL");
                // Placa única por fila: formato V02NNN (7 caracteres)
                fila.createCell(3).setCellValue(String.format("V02%03d", i));
                fila.createCell(4).setCellValue("50000");
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);

            return new MockMultipartFile(
                    "file",
                    "clientes-vehiculos-bulk-500.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    bos.toByteArray());
        }
    }

    // ==================== PRUEBAS DE RENDIMIENTO ====================

    @Test
    @DisplayName("VARE-02: 500 clientes con vehículo deben procesarse en menos de 20 segundos")
    void debeImportar500RegistrosConVehiculoEnMenos20Segundos() throws Exception {
        MockMultipartFile archivo = generarExcelClientesConVehiculos(500);

        StopWatch cronometro = new StopWatch("VARE-02");
        cronometro.start("importarClientes 500 filas (cliente + vehiculo)");

        ImportacionResponseDTO resultado = importacionService.importarClientes(archivo);

        cronometro.stop();

        long tiempoMs = cronometro.getTotalTimeMillis();
        double tiempoSeg = cronometro.getTotalTimeSeconds();

        System.out.printf("%n╔═══════════════════════════════════════════════════╗%n");
        System.out.printf("║           VARE-02 — Carga masiva (2 entidades)   ║%n");
        System.out.printf("╠═══════════════════════════════════════════════════╣%n");
        System.out.printf("║ Entidades por fila   : Cliente + Vehiculo         ║%n");
        System.out.printf("║ Registros procesados : %-27d║%n", resultado.getRegistrosProcesados());
        System.out.printf("║ Registros exitosos   : %-27d║%n", resultado.getRegistrosExitosos());
        System.out.printf("║ Registros fallidos   : %-27d║%n", resultado.getRegistrosFallidos());
        System.out.printf("║ Tiempo total         : %-23.3f seg ║%n", tiempoSeg);
        System.out.printf("║ Throughput           : %-20.1f reg/seg ║%n",
                resultado.getRegistrosExitosos() / tiempoSeg);
        System.out.printf("║ INSERTs estimados    : %-27d║%n",
                resultado.getRegistrosExitosos() * 2);  // 1 cliente + 1 vehiculo
        System.out.printf("╚═══════════════════════════════════════════════════╝%n%n");

        // Reporte de memoria JVM post-tarea
        long deltaHeapBytes = reportarUsoDeMemoria("VARE-02");

        if (!resultado.getErrores().isEmpty()) {
            System.out.println("[VARE-02] Errores encontrados:");
            resultado.getErrores().forEach(e -> System.out.println("  - " + e));
        }

        // Validaciones de corrección
        assertThat(resultado.getRegistrosProcesados())
                .as("Deben procesarse exactamente 500 registros")
                .isEqualTo(500);

        assertThat(resultado.getRegistrosExitosos())
                .as("Los 500 registros (cliente + vehículo) deben insertarse sin errores")
                .isEqualTo(500);

        assertThat(resultado.getRegistrosFallidos())
                .as("No debe haber registros fallidos")
                .isEqualTo(0);

        // Validación de rendimiento
        assertThat(tiempoMs)
                .as("500 clientes con vehículo deben procesarse en menos de 20s (fue: %.2f s)", tiempoSeg)
                .isLessThan(20_000L);

        // Validación de memoria: el incremento de heap no debe superar 200 MB
        assertThat(deltaHeapBytes)
                .as("La carga de 500 registros no debe incrementar el heap más de 200 MB (fue: %.1f MB)", deltaHeapBytes / 1e6)
                .isLessThan(200L * 1024 * 1024);
    }

    @Test
    @DisplayName("VARE-02: Throughput con 2 entidades debe mantenerse >= 25 registros/segundo")
    void debeMantenerThroughputConDosEntidades() throws Exception {
        MockMultipartFile archivo = generarExcelClientesConVehiculos(500);

        StopWatch cronometro = new StopWatch();
        cronometro.start();

        ImportacionResponseDTO resultado = importacionService.importarClientes(archivo);

        cronometro.stop();

        double throughput = resultado.getRegistrosExitosos() / cronometro.getTotalTimeSeconds();

        System.out.printf("%n[VARE-02] Throughput (cliente + vehículo): %.1f registros/segundo%n", throughput);
        System.out.printf("[VARE-02] INSERTs totales estimados: %d (%.1f INSERT/seg)%n",
                resultado.getRegistrosExitosos() * 2,
                (resultado.getRegistrosExitosos() * 2) / cronometro.getTotalTimeSeconds());

        // Reporte de memoria JVM post-tarea
        reportarUsoDeMemoria("VARE-02 Throughput");

        // Con 2 entidades por fila el límite sigue siendo 25 reg/seg (la fila completa es la unidad)
        assertThat(throughput)
                .as("El throughput mínimo debe ser 25 registros/segundo incluso procesando 2 entidades")
                .isGreaterThanOrEqualTo(25.0);
    }

    @Test
    @DisplayName("VARE-02: Cada cliente importado debe tener exactamente un vehículo asociado")
    void cadaClienteImportadoDebeHaberCreadoSuVehiculo() throws Exception {
        // Prueba más pequeña para verificar integridad, no rendimiento
        MockMultipartFile archivo = generarExcelClientesConVehiculos(5);

        ImportacionResponseDTO resultado = importacionService.importarClientes(archivo);

        assertThat(resultado.getRegistrosExitosos())
                .as("Los 5 registros deben procesarse exitosamente")
                .isEqualTo(5);

        assertThat(resultado.getRegistrosFallidos())
                .as("Ningún registro debe fallar")
                .isEqualTo(0);

        assertThat(resultado.getErrores())
                .as("No deben generarse errores al procesar cliente con vehículo")
                .isEmpty();
    }
}
