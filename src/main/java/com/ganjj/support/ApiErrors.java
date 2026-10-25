package com.ganjj.support;

import java.sql.SQLException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import io.micrometer.core.instrument.MeterRegistry;

@RestControllerAdvice
public class ApiErrors {
    private final MeterRegistry metrics;
    public ApiErrors(MeterRegistry metrics) { this.metrics=metrics; }
    @ExceptionHandler({IllegalArgumentException.class,HttpMessageNotReadableException.class})
    ResponseEntity<?> invalid(Exception ex) {
        metrics.counter("support_events_total","result","invalid").increment();
        return ResponseEntity.badRequest().body(Map.of("error", ex instanceof IllegalArgumentException ? ex.getMessage() : "corpo JSON inválido"));
    }
    @ExceptionHandler(SQLException.class)
    ResponseEntity<?> unavailable(SQLException ex) {
        return ResponseEntity.status(503).body(Map.of("error","armazenamento indisponível"));
    }
}
