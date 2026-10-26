package com.ganjj.support;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class SupportTest {
    @TempDir static Path dir;
    @DynamicPropertySource static void config(DynamicPropertyRegistry r) {
        r.add("support.secret",()->"integration-secret");
        r.add("support.db-path",()->dir.resolve("api.db").toString());
        r.add("support.email-provider",()->"fake");
        r.add("support.poll-initial-ms",()->"3600000");
    }
    @LocalServerPort int port;
    @Autowired ObjectMapper mapper;
    @Autowired NotificationStore store;
    @Autowired Dispatcher dispatcher;
    static OrderCreated event(String phone) {
        return new OrderCreated(UUID.randomUUID().toString(),"OrderCreated","2026-10-19T14:30:00Z","7","GANJJ-000007","42","cliente@example.com",phone,new BigDecimal("1234.56"),"BRL",null);
    }
    HttpResponse<String> request(String method,String path,String body,String secret) throws Exception {
        try(var client=HttpClient.newHttpClient()) {
            var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));
            if(secret!=null) builder.header("x-internal-secret",secret);
            builder.header("Content-Type","application/json");
            return client.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        }
    }
    @Test void acceptsProtectsDeduplicatesAndReportsDeliveries() throws Exception {
        var event=event("+5541999999999");
        String body=mapper.writeValueAsString(event);
        assertThat(request("POST","/internal/notifications",body,null).statusCode()).isEqualTo(403);
        assertThat(request("POST","/internal/notifications",body,"wrong").statusCode()).isEqualTo(403);
        assertThat(request("POST","/internal;matrix=x/notifications",body,null).statusCode()).isEqualTo(403);
        assertThat(store.deliveries(event.event_id())).isEmpty();
        var response=request("POST","/internal/notifications",body,"integration-secret");
        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(response.body()).contains("\"duplicate\":false");
        dispatcher.drain();
        assertThat(request("POST","/internal/notifications",body,"integration-secret").body()).contains("\"duplicate\":true");
        dispatcher.drain();
        var deliveries=store.deliveries(event.event_id()).orElseThrow();
        assertThat(deliveries).hasSize(3).allMatch(d->d.status().equals("sent")&&d.attempts()==1);
        assertThat(request("GET","/internal/notifications/"+event.event_id(),null,null).statusCode()).isEqualTo(403);
        assertThat(request("GET","/internal/notifications/"+event.event_id(),null,"integration-secret").body()).contains("deliveries","email","sms","whatsapp");
        assertThat(request("GET","/internal/notifications/unknown",null,"integration-secret").statusCode()).isEqualTo(404);
        assertThat(request("GET","/health",null,null).statusCode()).isEqualTo(200);
        assertThat(request("GET","/health/ready",null,null).statusCode()).isEqualTo(200);
        String metrics=request("GET","/metrics",null,null).body();
        assertThat(metrics).contains("support_events_total{result=\"accepted\"}","support_events_total{result=\"duplicate\"}","support_notifications_total{channel=\"email\",status=\"sent\"}","support_send_duration_seconds_bucket");
    }
    @Test void rejectsInvalidFieldsAndMissingTotal() throws Exception {
        for (var entry:Map.of("event_id","bad","event_type","Other","occurred_at","yesterday","customer_email","invalid","customer_phone","123","currency","USD").entrySet()) {
            var tree=mapper.readTree(mapper.writeValueAsString(event(null))).deepCopy();
            ((tools.jackson.databind.node.ObjectNode)tree).put(entry.getKey(),entry.getValue());
            var response=request("POST","/internal/notifications",tree.toString(),"integration-secret");
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body()).contains("campo inválido: "+entry.getKey());
        }
        var tree=(tools.jackson.databind.node.ObjectNode)mapper.readTree(mapper.writeValueAsString(event(null)));
        tree.remove("total");
        assertThat(request("POST","/internal/notifications",tree.toString(),"integration-secret").statusCode()).isEqualTo(400);
        tree.put("total",-1);
        assertThat(request("POST","/internal/notifications",tree.toString(),"integration-secret").body()).contains("total");
        assertThat(request("POST","/internal/notifications","{","integration-secret").statusCode()).isEqualTo(400);
        assertThat(request("GET","/metrics",null,null).body()).contains("support_events_total{result=\"invalid\"}");
    }
    @Test void missingEventIdIsRejectedWithoutEnqueuingDelivery() throws Exception {
        var before=store.pending().stream().map(OrderCreated::event_id).toList();
        var tree=(tools.jackson.databind.node.ObjectNode)mapper.readTree(mapper.writeValueAsString(event(null)));
        tree.remove("event_id");
        var response=request("POST","/internal/notifications",tree.toString(),"integration-secret");
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("campo inválido: event_id");
        assertThat(store.pending().stream().map(OrderCreated::event_id).toList()).containsExactlyElementsOf(before);
    }
    @Test void concurrentDuplicateRequestsProduceOneEvent() throws Exception {
        var e=event(null);
        var body=mapper.writeValueAsString(e);
        try (var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            var calls=new ArrayList<Future<String>>();
            for(int i=0;i<12;i++) calls.add(executor.submit(()->request("POST","/internal/notifications",body,"integration-secret").body()));
            int accepted=0;
            for(var result:calls) if(result.get().contains("\"duplicate\":false")) accepted++;
            assertThat(accepted).isEqualTo(1);
        }
        dispatcher.drain();
        assertThat(store.deliveries(e.event_id()).orElseThrow()).hasSize(3);
    }
    @Test void validatesProviderSelection() {
        assertThatThrownBy(()->new SupportConfig("","x","fake","fake","fake","x")).hasMessageContaining("INTERNAL_SECRET");
        assertThatThrownBy(()->new SupportConfig("s","x","fake","unknown","fake","x")).hasMessageContaining("SMS_PROVIDER");
        assertThatThrownBy(()->new SupportConfig("s","x","fake","fake","unknown","x")).hasMessageContaining("WHATSAPP_PROVIDER");
        assertThat(Channels.fake("sms").provider()).isEqualTo("fake");
    }
    @Test void persistsEventsAndDeliveryHistoryAcrossReopenAndResumesPending() throws Exception {
        var cfg=new SupportConfig("secret",dir.resolve("persist.db").toString(),"fake","fake","fake","from@example.com");
        var original=new NotificationStore(cfg,mapper);
        var e=event(null);
        assertThat(original.insert(e)).isTrue();
        assertThat(original.insert(e)).isFalse();
        original.save(e.event_id(),new NotificationStore.Delivery("email","sent","fake",1,null));
        var reopened=new NotificationStore(cfg,mapper);
        var calls=new AtomicInteger();
        var email=notifier("email",calls,0);
        var registry=new SimpleMeterRegistry();
        new Dispatcher(reopened,List.of(email,Channels.fake("sms"),Channels.fake("whatsapp")),registry,ms->{}).drain();
        assertThat(calls.get()).isZero();
        assertThat(reopened.deliveries(e.event_id()).orElseThrow()).hasSize(3).filteredOn(d->!d.channel().equals("email")).allMatch(d->d.status().equals("skipped")&&d.attempts()==0);
        assertThat(reopened.pending()).isEmpty();
        assertThat(registry.get("support_notifications_total").tags("channel","sms","status","skipped").counter().count()).isEqualTo(1);
    }
    static Notifier notifier(String channel,AtomicInteger calls,int failures) {
        return new Notifier() {
            public String channel(){return channel;}
            public String provider(){return "test";}
            public void send(Message m) throws Exception { if(calls.incrementAndGet()<=failures) throw new Exception("provider down"); }
        };
    }
    @Test void retriesAndContinuesOtherChannelsAfterEmailFailure() throws Exception {
        for(int failures:new int[]{1,3}) {
            var cfg=new SupportConfig("s",dir.resolve("retry"+failures+".db").toString(),"fake","fake","fake","x");
            var db=new NotificationStore(cfg,mapper);
            var e=event("+5541999999999");db.insert(e);
            var calls=new AtomicInteger();var waits=new ArrayList<Long>();var registry=new SimpleMeterRegistry();
            new Dispatcher(db,List.of(notifier("email",calls,failures),Channels.fake("sms"),Channels.fake("whatsapp")),registry,waits::add).drain();
            var deliveries=db.deliveries(e.event_id()).orElseThrow();
            assertThat(deliveries.get(0).attempts()).isEqualTo(failures==1?2:3);
            assertThat(deliveries.get(0).status()).isEqualTo(failures==1?"sent":"failed");
            assertThat(deliveries.subList(1,3)).allMatch(d->d.status().equals("sent"));
            assertThat(waits).containsExactlyElementsOf(failures==1?List.of(1000L):List.of(1000L,2000L));
            assertThat(registry.get("support_notifications_total").tags("channel","email","status",failures==1?"sent":"failed").counter().count()).isEqualTo(1);
        }
    }
    @Test void readinessDetectsMissingSchema() throws Exception {
        Path file=dir.resolve("readiness.db");
        var db=new NotificationStore(new SupportConfig("s",file.toString(),"fake","fake","fake","x"),mapper);
        assertThat(db.ready()).isTrue();
        Files.delete(file);
        assertThat(db.ready()).isFalse();
    }
    @Test void formatsEmailAndLimitsSmsByCodePoints() {
        var e=event("+5541999999999");
        assertThat(Dispatcher.message(e,"email").subject()).isEqualTo("Pedido GANJJ-000007 confirmado");
        assertThat(Dispatcher.message(e,"email").text()).contains("R$ 1.234,56","2026-10-19");
        var longEvent=new OrderCreated(e.event_id(),e.event_type(),e.occurred_at(),e.order_id(),"😀".repeat(200),e.customer_id(),e.customer_email(),e.customer_phone(),e.total(),e.currency(),null);
        String sms=Dispatcher.message(longEvent,"sms").text();
        assertThat(sms.codePointCount(0,sms.length())).isLessThanOrEqualTo(160);
        assertThat(sms).endsWith("Total R$ 1.234,56.");
        var hugeTotal=new OrderCreated(e.event_id(),e.event_type(),e.occurred_at(),e.order_id(),e.order_code(),e.customer_id(),e.customer_email(),e.customer_phone(),new BigDecimal("1e300"),e.currency(),null);
        String hugeSms=Dispatcher.message(hugeTotal,"sms").text();
        assertThat(hugeSms.codePointCount(0,hugeSms.length())).isLessThanOrEqualTo(160);
    }
}
