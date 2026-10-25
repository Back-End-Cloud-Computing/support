package com.ganjj.support;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.env.Environment;
import tools.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
@Conditional(RabbitConfiguration.Enabled.class)
public class RabbitConfiguration {
    static class Enabled implements Condition {
        public boolean matches(ConditionContext context,AnnotatedTypeMetadata metadata) {
            return !context.getEnvironment().getProperty("AMQP_URL","").isBlank();
        }
    }
    @Bean(destroyMethod="destroy") CachingConnectionFactory supportRabbitConnection(Environment env) throws Exception {
        var raw=new com.rabbitmq.client.ConnectionFactory();raw.setAutomaticRecoveryEnabled(false);raw.setUri(env.getRequiredProperty("AMQP_URL"));
        return new CachingConnectionFactory(raw);
    }
    @Bean RabbitAdmin supportRabbitAdmin(CachingConnectionFactory connection) { return new RabbitAdmin(connection); }
    @Bean Declarables supportQueues() {
        var exchange=new TopicExchange("ganjj.orders",true,false);
        var queue=QueueBuilder.durable("support.order-created").deadLetterExchange("").deadLetterRoutingKey("support.order-created.dlq").build();
        return new Declarables(exchange,queue,new Queue("support.order-created.dlq",true),
                new Binding("support.order-created",Binding.DestinationType.QUEUE,"ganjj.orders","order.created",null));
    }
    @Bean SimpleMessageListenerContainer supportConsumer(CachingConnectionFactory connection,RabbitAdmin admin,Declarables supportQueues,NotificationAcceptance acceptance,ObjectMapper mapper,MeterRegistry metrics) {
        var container=new SimpleMessageListenerContainer(connection);
        container.setAmqpAdmin(admin);container.setMissingQueuesFatal(false);
        container.setQueueNames("support.order-created");container.setAcknowledgeMode(AcknowledgeMode.MANUAL);container.setPrefetchCount(1);
        container.setMessageListener((ChannelAwareMessageListener)(message,channel)->{
            long tag=message.getMessageProperties().getDeliveryTag();
            OrderCreated event;
            try { event=mapper.readValue(message.getBody(),OrderCreated.class);event.validate(); }
            catch(Exception ex) { metrics.counter("support_events_total","result","invalid").increment();channel.basicReject(tag,false);return; }
            for(int attempt=1;attempt<=3;attempt++) {
                try { acceptance.accept(event);channel.basicAck(tag,false);return; }
                catch(java.sql.SQLException ex) {
                    if(attempt==3) { channel.basicReject(tag,false);return; }
                    Thread.sleep(attempt*100L);
                }
            }
        });
        return container;
    }
}
