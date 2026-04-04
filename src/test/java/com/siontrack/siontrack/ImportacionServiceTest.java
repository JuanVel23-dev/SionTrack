package com.siontrack.siontrack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.siontrack.siontrack.DTO.Response.ImportacionResponseDTO;
import com.siontrack.siontrack.models.Clientes;
import com.siontrack.siontrack.repository.ClienteRepository;
import com.siontrack.siontrack.repository.ProductosRepository;
import com.siontrack.siontrack.repository.ServiciosRepository;
import com.siontrack.siontrack.repository.VehiculosRepository;
import com.siontrack.siontrack.services.ClienteServicios;
import com.siontrack.siontrack.services.ImportacionService;
import com.siontrack.siontrack.services.ProductosServicios;
import com.siontrack.siontrack.services.ProveedoresService;
import com.siontrack.siontrack.services.ServiciosService;

/**
 * VA-03: Pruebas de carga de datos al sistema desde archivos CSV/Excel.
 * Valida que la importación procese correctamente los registros, detecte
 * duplicados y reporte errores sin afectar los registros válidos.
 */
@ExtendWith(MockitoExtension.class)
class ImportacionServiceTest {

    // --- Dependencias de ImportacionService (todas mockeadas) ---
    @Mock private ClienteServicios clienteServicios;
    @Mock private ProductosServicios productosServicios;
    @Mock private ProveedoresService proveedoresService;
    @Mock private ServiciosService serviciosService;
    @Mock private ClienteRepository clienteRepository;
    @Mock private VehiculosRepository vehiculosRepository;
    @Mock private ProductosRepository productosRepository;
    @Mock private ServiciosRepository serviciosRepository;

    @InjectMocks
    private ImportacionService importacionService;

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Crea un MockMultipartFile a partir de contenido CSV en texto plano.
     * Simula la carga de un archivo .csv desde el formulario web.
     */
    private MockMultipartFile csvFile(String nombre, String contenidoCsv) {
        return new MockMultipartFile(
                "file",
                nombre,
                "text/csv",
                contenidoCsv.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Configura el repositorio para simular que ningún cliente existe en BD.
     * Se usa cuando se quiere probar la creación de registros nuevos.
     */
    private void simularClientesNuevos() {
        when(clienteRepository.findByCedulaRuc(anyString())).thenReturn(Optional.empty());
        when(clienteRepository.findByNombreIgnoreCase(anyString())).thenReturn(Optional.empty());
    }

    // ==================== PRUEBAS DE IMPORTACIÓN DE CLIENTES ====================

    @Test
    @DisplayName("VA-03: Importar 4 filas con 1 duplicado interno → 3 exitosos, 1 fallido")
    void debeDetectarDuplicadosDentroDelArchivo() {
        String csv = """
                nombre,cedula_ruc,tipo_cliente,telefono
                Juan Perez,1001,NATURAL,3001111111
                Ana Garcia,1002,NATURAL,3002222222
                Pedro Lopez,1003,NATURAL,3003333333
                Juan Perez,1001,NATURAL,3001111111
                """;

        simularClientesNuevos();

        ImportacionResponseDTO resultado = importacionService.importarClientes(csvFile("clientes.csv", csv));

        assertThat(resultado.getRegistrosProcesados()).isEqualTo(4);
        assertThat(resultado.getRegistrosExitosos()).isEqualTo(3);
        assertThat(resultado.getRegistrosFallidos()).isEqualTo(1);
        assertThat(resultado.getErrores())
                .hasSize(1)
                .first().asString()
                .contains("Duplicado en el archivo");
    }

    @Test
    @DisplayName("VA-03: Importar 3 clientes sin duplicados → todos deben ser exitosos")
    void debeImportarClientesUnicosCorrectamente() {
        String csv = """
                nombre,cedula_ruc,tipo_cliente
                Laura Martinez,2001,NATURAL
                Carlos Jimenez,2002,NATURAL
                Sofia Herrera,2003,NATURAL
                """;

        simularClientesNuevos();

        ImportacionResponseDTO resultado = importacionService.importarClientes(csvFile("clientes.csv", csv));

        assertThat(resultado.getRegistrosExitosos()).isEqualTo(3);
        assertThat(resultado.getRegistrosFallidos()).isEqualTo(0);
        assertThat(resultado.getErrores()).isEmpty();
        // Debe haberse llamado crearCliente exactamente 3 veces
        verify(clienteServicios, times(3)).crearCliente(any());
    }

    @Test
    @DisplayName("VA-03: Fila sin nombre debe generar error y no importarse")
    void filasinNombreDebeGenerarError() {
        String csv = """
                nombre,cedula_ruc,tipo_cliente
                ,5001,NATURAL
                Nombre Valido,5002,NATURAL
                """;

        simularClientesNuevos();

        ImportacionResponseDTO resultado = importacionService.importarClientes(csvFile("clientes.csv", csv));

        assertThat(resultado.getRegistrosFallidos()).isEqualTo(1);
        assertThat(resultado.getRegistrosExitosos()).isEqualTo(1);
        assertThat(resultado.getErrores())
                .hasSize(1)
                .first().asString()
                .contains("Nombre es requerido");
    }

    @Test
    @DisplayName("VA-03: Cliente ya existente en BD debe actualizarse, no duplicarse")
    void clienteExistenteEnBdDebeActualizarseNoCrearse() {
        String csv = """
                nombre,cedula_ruc,tipo_cliente
                Juan Existente,7001,NATURAL
                """;

        // Simular que el cliente con cédula 7001 ya existe en BD
        Clientes clienteExistente = new Clientes();
        clienteExistente.setCliente_id(10);
        clienteExistente.setNombre("Juan Existente");
        when(clienteRepository.findByCedulaRuc("7001")).thenReturn(Optional.of(clienteExistente));

        ImportacionResponseDTO resultado = importacionService.importarClientes(csvFile("clientes.csv", csv));

        assertThat(resultado.getRegistrosExitosos()).isEqualTo(1);
        assertThat(resultado.getRegistrosActualizados()).isEqualTo(1);
        assertThat(resultado.getRegistrosCreados()).isEqualTo(0);

        // Debe actualizar, NO crear
        verify(clienteServicios, never()).crearCliente(any());
        verify(clienteServicios).actualizarCliente(eq(10), any());
    }

    @Test
    @DisplayName("VA-03: Mezcla de clientes nuevos y existentes debe procesarse correctamente")
    void debeManejarMezclaDeClientesNuevosYExistentes() {
        String csv = """
                nombre,cedula_ruc,tipo_cliente
                Cliente Nuevo,8001,NATURAL
                Cliente Existente,8002,JURIDICO
                """;

        Clientes existente = new Clientes();
        existente.setCliente_id(20);
        existente.setNombre("Cliente Existente");

        when(clienteRepository.findByCedulaRuc("8001")).thenReturn(Optional.empty());
        when(clienteRepository.findByNombreIgnoreCase("Cliente Nuevo")).thenReturn(Optional.empty());
        when(clienteRepository.findByCedulaRuc("8002")).thenReturn(Optional.of(existente));

        ImportacionResponseDTO resultado = importacionService.importarClientes(csvFile("clientes.csv", csv));

        assertThat(resultado.getRegistrosExitosos()).isEqualTo(2);
        assertThat(resultado.getRegistrosCreados()).isEqualTo(1);
        assertThat(resultado.getRegistrosActualizados()).isEqualTo(1);
        assertThat(resultado.getRegistrosFallidos()).isEqualTo(0);
    }

    @Test
    @DisplayName("VA-03: Archivo CSV vacío (solo cabecera) debe retornar 0 registros procesados")
    void archivoCsvSoloCabeceraDebeRetornarCero() {
        String csv = "nombre,cedula_ruc,tipo_cliente\n";

        ImportacionResponseDTO resultado = importacionService.importarClientes(csvFile("vacio.csv", csv));

        assertThat(resultado.getRegistrosProcesados()).isEqualTo(0);
        assertThat(resultado.getRegistrosExitosos()).isEqualTo(0);
        verifyNoInteractions(clienteServicios);
    }

    @Test
    @DisplayName("VA-03: Todos los registros duplicados → 0 exitosos, todos fallidos")
    void todosDuplicadosDebenResultarEnCeroExitosos() {
        String csv = """
                nombre,cedula_ruc,tipo_cliente
                Repetido Uno,9001,NATURAL
                Repetido Uno,9001,NATURAL
                Repetido Uno,9001,NATURAL
                """;

        simularClientesNuevos();

        ImportacionResponseDTO resultado = importacionService.importarClientes(csvFile("clientes.csv", csv));

        // Solo el primero pasa; los dos siguientes son duplicados del archivo
        assertThat(resultado.getRegistrosExitosos()).isEqualTo(1);
        assertThat(resultado.getRegistrosFallidos()).isEqualTo(2);
    }

    // ==================== PRUEBAS DE TIPO DE IMPORTACIÓN ====================

    @Test
    @DisplayName("VA-03: El resultado debe identificar correctamente el tipo de importación")
    void elResultadoDebeIdentificarElTipoImportacion() {
        String csv = "nombre,cedula_ruc\nTest,1234\n";
        simularClientesNuevos();

        ImportacionResponseDTO resultado = importacionService.importarClientes(csvFile("test.csv", csv));

        assertThat(resultado.getTipoImportacion()).isEqualTo("CLIENTES");
    }

    // ==================== PRUEBA DE INTEGRIDAD: SIN REGISTROS INCOMPLETOS ====================

    @Test
    @DisplayName("VA-03: Registros con error no deben crear entradas parciales en BD")
    void registrosConErrorNoDebenCrearEntradasParciales() {
        String csv = """
                nombre,cedula_ruc,tipo_cliente
                ,3001,NATURAL
                ,3002,NATURAL
                ,3003,NATURAL
                """;

        // No se necesita simular BD porque ningún registro debe llegar hasta ahí
        ImportacionResponseDTO resultado = importacionService.importarClientes(csvFile("invalidos.csv", csv));

        assertThat(resultado.getRegistrosExitosos()).isEqualTo(0);
        assertThat(resultado.getRegistrosFallidos()).isEqualTo(3);
        // Nunca debe tocarse el repositorio de clientes
        verifyNoInteractions(clienteServicios);
    }
}
