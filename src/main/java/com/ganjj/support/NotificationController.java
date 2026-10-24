package com.ganjj.support;

import java.sql.SQLException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.micrometer.core.instrument.MeterRegistry;

@RestController
public class NotificationController {
    private final NotificationStore store;
    private final MeterRegistry metrics;
    public NotificationController(NotificationStore store,MeterRegistry metrics) { this.store=store; this.metrics=metrics; }
    @PostMapping("/internal/notifications")
    ResponseEntity<?> accept(@RequestBody OrderCreated event,@RequestHeader(value="X-Correlation-ID",required=false) String correlation) throws SQLException {
        event.validate();
        if (event.correlation_id()==null && correlation!=null) {
            event=new OrderCreated(event.event_id(),event.event_type(),event.occurred_at(),event.order_id(),event.order_code(),event.customer_id(),event.customer_email(),event.customer_phone(),event.total(),event.currency(),correlation);
            event.validate();
        }
        boolean inserted=store.insert(event);
        metrics.counter("support_events_total","result",inserted?"accepted":"duplicate").increment();
        return ResponseEntity.accepted().body(Map.of("status","accepted","event_id",event.event_id(),"duplicate",!inserted));
    }
    @GetMapping("/internal/notifications/{id}")
    ResponseEntity<?> status(@PathVariable String id) throws SQLException {
        var result=store.deliveries(id);
        return result.isPresent() ? ResponseEntity.ok(Map.of("event_id",id,"deliveries",result.get())) : ResponseEntity.status(404).body(Map.of("error","evento não encontrado"));
    }
    @GetMapping("/health") Map<String,String> health() { return Map.of("status","ok"); }
    @GetMapping("/health/ready") ResponseEntity<?> ready() {
        boolean ready=store.ready();
        return ResponseEntity.status(ready?200:503).body(Map.of("status",ready?"ok":"unavailable"));
    }
}
