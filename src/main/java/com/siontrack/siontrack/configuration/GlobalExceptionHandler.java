package com.siontrack.siontrack.configuration;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejador global de excepciones para endpoints REST (@RestController).
 * No aplica a los ViewController (@Controller) que manejan sus errores
 * con BindingResult y try/catch para retornar vistas HTML.
 */
@RestControllerAdvice(basePackages = "com.siontrack.siontrack.controllers")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maneja errores de validacion de DTOs (@Valid).
     * Retorna HTTP 400 con los campos y mensajes de error.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errores = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
            errores.put(error.getField(), error.getDefaultMessage())
        );

        response.put("error", "Error de validacion");
        response.put("campos", errores);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Maneja RuntimeException genericas de los servicios.
     * Retorna HTTP 400 con mensaje generico, sin exponer detalles internos.
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        log.error("Error en la operacion: {}", ex.getMessage(), ex);

        Map<String, String> response = new HashMap<>();
        String mensaje = ex.getMessage();

        // Solo exponer mensajes controlados, no stack traces
        if (mensaje != null && mensaje.length() < 200 && !mensaje.contains("Exception")) {
            response.put("error", mensaje);
        } else {
            response.put("error", "Ocurrio un error al procesar la solicitud");
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Maneja cualquier excepcion no controlada.
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Error inesperado: {}", ex.getMessage(), ex);

        Map<String, String> response = new HashMap<>();
        response.put("error", "Ocurrio un error inesperado");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
