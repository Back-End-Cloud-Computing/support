package com.ganjj.support;

import java.nio.file.*;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import io.micrometer.core.instrument.MeterRegistry;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named="SUPPORT_AMQP_TEST_URL",matches=".+")
class RabbitConsumerTest {
    @TempDir static Path dir;
    @DynamicPropertySource static void props(DynamicPropertyRegistry r) {
        // Order/Promotion already specify a durable direct exchange with this name.
        // Declare it before Spring starts: a different type must fail the test.
        try {
            var broker=new com.rabbitmq.client.ConnectionFactory();
            broker.setUri(System.getenv("SUPPORT_AMQP_TEST_URL"));
            try(var connection=broker.newConnection();var channel=connection.createChannel()) {
                channel.exchangeDeclare("ganjj.orders","direct",true);
            }
        } catch(Exception ex) { throw new IllegalStateException("Cannot prepare shared direct exchange",ex); }
        r.add("AMQP_URL",()->System.getenv("SUPPORT_AMQP_TEST_URL"));r.add("support.secret",()->"rabbit-test");
        r.add("support.db-path",()->dir.resolve("rabbit.db").toString());r.add("support.email-provider",()->"fake");
        r.add("support.poll-initial-ms",()->"3600000");
    }
    @Autowired CachingConnectionFactory connection;
    @Autowired NotificationStore store;
    @Autowired org.springframework.amqp.rabbit.core.RabbitAdmin admin;
    @Autowired ObjectMapper mapper;
    @Autowired MeterRegistry metrics;
    @Test void persistsAcksDeduplicatesAndDeadLettersInvalidAndUnavailableStore() throws Exception {
        try(var c=connection.createConnection();var channel=c.createChannel(false)) {
            channel.queuePurge("support.order-created");channel.queuePurge("support.order-created.dlq");
            var event=SupportTest.event(null);
            byte[] body=mapper.writeValueAsBytes(event);
            channel.basicPublish("ganjj.orders","order.created",null,body);
            channel.basicPublish("ganjj.orders","order.created",null,body);
            await(()->store.deliveries(event.event_id()).isPresent() && metrics.find("support_events_total").tag("result","duplicate").counter()!=null);
            assertThat(metrics.get("support_events_total").tag("result","accepted").counter().count()).isEqualTo(1);
            channel.basicPublish("ganjj.orders","order.created",null,"{bad json".getBytes());
            await(()->channel.queueDeclarePassive("support.order-created.dlq").getMessageCount()==1);
            assertThat(metrics.get("support_events_total").tag("result","invalid").counter().count()).isEqualTo(1);
            admin.deleteQueue("support.order-created");
            await(()->admin.getQueueInfo("support.order-created")!=null);
            var recovered=SupportTest.event(null);
            channel.basicPublish("ganjj.orders","order.created",null,mapper.writeValueAsBytes(recovered));
            await(()->store.deliveries(recovered.event_id()).isPresent());
            Files.delete(dir.resolve("rabbit.db"));
            channel.basicPublish("ganjj.orders","order.created",null,mapper.writeValueAsBytes(SupportTest.event(null)));
            await(()->channel.queueDeclarePassive("support.order-created.dlq").getMessageCount()==2);
            assertThat(channel.queueDeclarePassive("support.order-created").getMessageCount()).isZero();
        }
    }
    interface Check { boolean ok() throws Exception; }
    static void await(Check check) throws Exception {
        long end=System.nanoTime()+Duration.ofSeconds(15).toNanos();
        while(System.nanoTime()<end) { if(check.ok()) return;Thread.sleep(50); }
        fail("RabbitMQ não atingiu o estado esperado");
    }
}
