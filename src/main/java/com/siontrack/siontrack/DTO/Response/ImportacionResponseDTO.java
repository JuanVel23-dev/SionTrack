package com.siontrack.siontrack.DTO.Response;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImportacionResponseDTO {

    private String tipoImportacion;
    private int registrosProcesados;
    private int registrosExitosos;
    private int registrosCreados;
    private int registrosActualizados;
    private int registrosFallidos;
    private List<String> errores = new ArrayList<>();

    public void agregarError(int fila, String mensaje) {
        errores.add("Fila " + fila + ": " + mensaje);
    
    }
}
