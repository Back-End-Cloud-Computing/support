package com.ganjj.support;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class Dispatcher {
    @FunctionalInterface interface Sleeper { void sleep(long millis) throws InterruptedException; }
    private final NotificationStore store;
    private final List<Notifier> channels;
    private final MeterRegistry metrics;
    private final Sleeper sleeper;

    @Autowired
    public Dispatcher(NotificationStore store, List<Notifier> channels, MeterRegistry metrics) {
        this(store, channels, metrics, Thread::sleep);
    }
    Dispatcher(NotificationStore store, List<Notifier> channels, MeterRegistry metrics, Sleeper sleeper) {
        this.store=store; this.channels=channels; this.metrics=metrics; this.sleeper=sleeper;
    }
    // A single worker drains durable events, including those accepted before a restart.
    @Scheduled(fixedDelayString="${support.poll-ms:250}", initialDelayString="${support.poll-initial-ms:250}")
    public void drain() {
        try {
            for (var event : store.pending()) process(event);
        } catch (Exception ex) {
            LoggerFactory.getLogger(Dispatcher.class).error("Falha ao processar fila persistida", ex);
        }
    }
    void process(OrderCreated event) throws Exception {
        var saved=store.deliveries(event.event_id()).orElseThrow();
        for (var notifier:channels) {
            if (saved.stream().anyMatch(d -> d.channel().equals(notifier.channel()))) continue;
            boolean email="email".equals(notifier.channel());
            String recipient=email ? event.customer_email() : event.customer_phone();
            String status="skipped", error=null;
            int attempts=0;
            var start=Timer.start(metrics);
            if (recipient!=null && !recipient.isBlank()) {
                for (int attempt=1;attempt<=3;attempt++) {
                    attempts=attempt;
                    try { notifier.send(message(event,notifier.channel())); status="sent"; error=null; break; }
                    catch (Exception ex) {
                        status="failed";
                        // Error text from a provider may contain recipient details or credentials.
                        error="Falha no provedor " + notifier.provider();
                        if (attempt<3) sleeper.sleep(1000L << (attempt-1));
                    }
                }
            }
            store.save(event.event_id(),new NotificationStore.Delivery(notifier.channel(),status,notifier.provider(),attempts,error));
            metrics.counter("support_notifications_total","channel",notifier.channel(),"status",status).increment();
            start.stop(Timer.builder("support_send_duration_seconds").tag("channel",notifier.channel()).publishPercentileHistogram().register(metrics));
            LoggerFactory.getLogger(Dispatcher.class).info("Entrega event_id={} channel={} status={} correlation_id={}", event.event_id(),notifier.channel(),status,event.correlation_id());
        }
    }
    static Notifier.Message message(OrderCreated event,String channel) {
        var format=NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
        String amount=format.format(event.total()).replace('\u00a0',' ');
        String text="GANJJ: pedido " + event.order_code() + " confirmado. Total " + amount + ".";
        if (!"email".equals(channel) && text.codePointCount(0,text.length())>160) {
            String suffix=" confirmado. Total " + amount + ".";
            String prefix="GANJJ: pedido ";
            int budget=160-prefix.length()-suffix.length()-1;
            String code=event.order_code();
            if (budget > 0) {
                text=prefix+code.substring(0,code.offsetByCodePoints(0,Math.min(budget,code.codePointCount(0,code.length()))))+"…"+suffix;
            } else {
                text=text.substring(0,text.offsetByCodePoints(0,159))+"…";
            }
        }
        return new Notifier.Message("email".equals(channel)?event.customer_email():event.customer_phone(),
                "Pedido " + event.order_code() + " confirmado", "email".equals(channel)?text+"\nData: "+event.occurred_at():text);
    }
}
