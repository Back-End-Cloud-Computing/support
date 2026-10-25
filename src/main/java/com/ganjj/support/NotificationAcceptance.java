package com.ganjj.support;

import java.sql.SQLException;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class NotificationAcceptance {
    private final NotificationStore store;
    private final MeterRegistry metrics;
    public NotificationAcceptance(NotificationStore store,MeterRegistry metrics){this.store=store;this.metrics=metrics;}
    public boolean accept(OrderCreated event) throws SQLException {
        event.validate();
        boolean inserted=store.insert(event);
        metrics.counter("support_events_total","result",inserted?"accepted":"duplicate").increment();
        return inserted;
    }
}
