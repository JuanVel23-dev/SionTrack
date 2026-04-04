package com.siontrack.siontrack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.siontrack.siontrack.models.Clientes;
import com.siontrack.siontrack.models.Notificaciones;
import com.siontrack.siontrack.models.Servicios;
import com.siontrack.siontrack.models.Vehiculos;
import com.siontrack.siontrack.repository.NotificacionesRepository;
import com.siontrack.siontrack.services.RecordatorioService;


@ExtendWith(MockitoExtension.class)
class RecordatoriosTest {

    @Mock
    private NotificacionesRepository notificacionesRepository;

    @InjectMocks
    private RecordatorioService recordatorioService;

    /**
     * Construye un servicio de prueba con los datos mínimos requeridos.
     *
     * @param fechaServicio fecha en que se realizó el servicio
     * @return instancia de Servicios lista para usarse en pruebas
     */
    private Servicios construirServicio(LocalDate fechaServicio) {
        Clientes cliente = new Clientes();
        cliente.setCliente_id(1);

        Vehiculos vehiculo = new Vehiculos();
        vehiculo.setVehiculo_id(1);
        vehiculo.setClientes(cliente);

        Servicios servicio = new Servicios();
        servicio.setServicio_id(1);
        servicio.setFecha_servicio(fechaServicio);
        servicio.setKilometraje_servicio("50000");
        servicio.setClientes(cliente);
        servicio.setVehiculos(vehiculo);

        return servicio;
    }

    /**
     * Configura los mocks del repositorio para simular que no existen
     * recordatorios previos (estado limpio antes de crear nuevos).
     */
    private void simularSinRecordatoriosPrevios(Servicios servicio) {
        when(notificacionesRepository.existsByServicio(servicio)).thenReturn(false);
        when(notificacionesRepository.findRecordatoriosPendientesByClienteVehiculo(anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(notificacionesRepository.existsRecordatorioDuplicado(anyInt(), anyInt(), any(Timestamp.class)))
                .thenReturn(false);
    }


    @Test
    @DisplayName("VA-01: Servicio del 10/09/2025 debe generar recordatorios el 10/04/2026 y 10/05/2026")
    void debeGenerarDosRecordatoriosConFechasCorrectas() {
        Servicios servicio = construirServicio(LocalDate.of(2025, 9, 10));
        simularSinRecordatoriosPrevios(servicio);

        ArgumentCaptor<Notificaciones> captor = ArgumentCaptor.forClass(Notificaciones.class);

        recordatorioService.procesarServicioParaRecordatorios(servicio);

        verify(notificacionesRepository, times(2)).save(captor.capture());

        List<LocalDate> fechasGeneradas = captor.getAllValues().stream()
                .map(n -> n.getFecha_programada().toLocalDateTime().toLocalDate())
                .toList();

        assertThat(fechasGeneradas)
                .as("Deben generarse recordatorios el 10/04/2026 y el 10/05/2026")
                .containsExactlyInAnyOrder(
                        LocalDate.of(2026, 4, 10),
                        LocalDate.of(2026, 5, 10));
    }

    @Test
    @DisplayName("VA-01: Los recordatorios deben tener tipo RECORDATORIO_SERVICIO y canal whatsapp")
    void losRecordatoriosDebenTenerTipoYCanalCorrectos() {
        Servicios servicio = construirServicio(LocalDate.of(2025, 9, 10));
        simularSinRecordatoriosPrevios(servicio);

        ArgumentCaptor<Notificaciones> captor = ArgumentCaptor.forClass(Notificaciones.class);
        recordatorioService.procesarServicioParaRecordatorios(servicio);
        verify(notificacionesRepository, times(2)).save(captor.capture());

        captor.getAllValues().forEach(n -> {
            assertThat(n.getTipoNotificacion())
                    .as("El tipo debe ser RECORDATORIO_SERVICIO")
                    .isEqualTo("RECORDATORIO_SERVICIO");
            assertThat(n.getCanal())
                    .as("El canal debe ser whatsapp")
                    .isEqualTo("whatsapp");
            assertThat(n.getEstado())
                    .as("El estado inicial debe ser pendiente")
                    .isEqualTo("pendiente");
        });
    }

    @Test
    @DisplayName("VA-01: Los recordatorios deben tener el kilometraje y la fecha de próximo servicio")
    void losRecordatoriosDebenGuardarFechaProximoServicio() {
        Servicios servicio = construirServicio(LocalDate.of(2025, 9, 10));
        simularSinRecordatoriosPrevios(servicio);

        ArgumentCaptor<Notificaciones> captor = ArgumentCaptor.forClass(Notificaciones.class);
        recordatorioService.procesarServicioParaRecordatorios(servicio);
        verify(notificacionesRepository, times(2)).save(captor.capture());

        LocalDate fechaEsperadaProximoServicio = LocalDate.of(2026, 5, 10);
        captor.getAllValues().forEach(n -> {
            assertThat(n.getFechaProximoServicio().toLocalDateTime().toLocalDate())
                    .as("La fecha de próximo servicio debe ser 10/05/2026")
                    .isEqualTo(fechaEsperadaProximoServicio);
            assertThat(n.getKilometrajeServicio())
                    .as("Debe conservar el kilometraje del servicio original")
                    .isEqualTo("50000");
        });
    }

    // ==================== CASOS DE BORDE ==================== //

    @Test
    @DisplayName("VA-01: No debe crear recordatorios si el servicio no tiene kilometraje")
    void noDebeCrearRecordatoriosSinKilometraje() {
        Servicios servicio = construirServicio(LocalDate.of(2025, 9, 10));
        servicio.setKilometraje_servicio(null);

        recordatorioService.procesarServicioParaRecordatorios(servicio);

        verifyNoInteractions(notificacionesRepository);
    }

    @Test
    @DisplayName("VA-01: No debe crear recordatorios si el kilometraje está vacío")
    void noDebeCrearRecordatoriosConKilometrajeVacio() {
        Servicios servicio = construirServicio(LocalDate.of(2025, 9, 10));
        servicio.setKilometraje_servicio("   ");

        recordatorioService.procesarServicioParaRecordatorios(servicio);

        verifyNoInteractions(notificacionesRepository);
    }

    @Test
    @DisplayName("VA-01: No debe crear recordatorios si ya existen para este servicio")
    void noDebeCrearRecordatoriosSiYaExistenParaEsteServicio() {
        Servicios servicio = construirServicio(LocalDate.of(2025, 9, 10));
        when(notificacionesRepository.existsByServicio(servicio)).thenReturn(true);

        recordatorioService.procesarServicioParaRecordatorios(servicio);

        verify(notificacionesRepository, never()).save(any());
    }

    @Test
    @DisplayName("VA-01: Debe eliminar recordatorios pendientes anteriores antes de crear los nuevos")
    void debeReemplazarRecordatoriosPendientesAnteriores() {
        Servicios servicio = construirServicio(LocalDate.of(2025, 9, 10));

        when(notificacionesRepository.existsByServicio(servicio)).thenReturn(false);

        Notificaciones pendienteAntiguo = new Notificaciones();
        pendienteAntiguo.setNotificacion_id(99);
        when(notificacionesRepository.findRecordatoriosPendientesByClienteVehiculo(anyInt(), anyInt()))
                .thenReturn(List.of(pendienteAntiguo));

        when(notificacionesRepository.existsRecordatorioDuplicado(anyInt(), anyInt(), any(Timestamp.class)))
                .thenReturn(false);

        recordatorioService.procesarServicioParaRecordatorios(servicio);

        verify(notificacionesRepository).deleteAll(List.of(pendienteAntiguo));
        verify(notificacionesRepository, times(2)).save(any(Notificaciones.class));
    }

    @Test
    @DisplayName("VA-01: No debe crear recordatorios duplicados para la misma fecha objetivo")
    void noDebeCrearRecordatorioDuplicado() {
        Servicios servicio = construirServicio(LocalDate.of(2025, 9, 10));
        when(notificacionesRepository.existsByServicio(servicio)).thenReturn(false);
        when(notificacionesRepository.findRecordatoriosPendientesByClienteVehiculo(anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        when(notificacionesRepository.existsRecordatorioDuplicado(anyInt(), anyInt(), any(Timestamp.class)))
                .thenReturn(true);

        recordatorioService.procesarServicioParaRecordatorios(servicio);

        verify(notificacionesRepository, never()).save(any());
    }
}