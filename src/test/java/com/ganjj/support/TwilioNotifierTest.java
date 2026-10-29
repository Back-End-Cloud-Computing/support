package com.ganjj.support;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class TwilioNotifierTest {
    @Test void encodesSmsAndWhatsAppTemplateAndRejectsErrorsWithoutLeakingBody() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var fields=new AtomicReference<Map<String,String>>();var auth=new AtomicReference<String>();var code=new AtomicInteger(201);
        String account="AC"+"a".repeat(32);
        server.createContext("/2010-04-01/Accounts/"+account+"/Messages.json",exchange->{
            assertThat(exchange.getRequestMethod()).isEqualTo("POST");
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            var form=new HashMap<String,String>();
            for(var entry:new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8).split("&")) {
                var pair=entry.split("=",2);form.put(URLDecoder.decode(pair[0],StandardCharsets.UTF_8),URLDecoder.decode(pair[1],StandardCharsets.UTF_8));
            }
            fields.set(form);byte[] response="private-provider-response".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code.get(),response.length);exchange.getResponseBody().write(response);exchange.close();
        });server.start();
        try {
            var base=URI.create("http://127.0.0.1:"+server.getAddress().getPort());
            var sms=new TwilioNotifier("sms",account,"test-token","+15550000001","","",false,base);
            var message=new Notifier.Message("+15550000002","Pedido confirmado","Olá + & R$ 12,00");
            sms.send(message);
            assertThat(fields.get()).containsEntry("To","+15550000002").containsEntry("From","+15550000001").containsEntry("Body",message.text());
            assertThat(auth.get()).isEqualTo("Basic "+Base64.getEncoder().encodeToString((account+":test-token").getBytes(StandardCharsets.UTF_8)));
            new TwilioNotifier("sms",account,"test-token","+15550000001","","sms_order_confirmation",false,base).send(message);
            assertThat(fields.get()).containsEntry("Body","sms_order_confirmation").containsEntry("To",message.recipient()).doesNotContainKey("ContentSid");
            new TwilioNotifier("whatsapp",account,"test-token","whatsapp:+15550000001","","sms_order_confirmation",false,base).send(message);
            assertThat(fields.get()).containsEntry("Body",message.text()).containsEntry("To","whatsapp:"+message.recipient());
            new TwilioNotifier("whatsapp",account,"test-token","whatsapp:+15550000001","HX"+"a".repeat(32),"sms_order_confirmation",false,base).send(message);
            assertThat(fields.get()).containsEntry("To","whatsapp:+15550000002").containsKeys("ContentSid","ContentVariables").doesNotContainKey("Body");
            assertThat(fields.get().get("ContentVariables")).contains("Pedido confirmado","Olá");
            new TwilioNotifier("whatsapp",account,"test-token","whatsapp:+15550000001","HX"+"b".repeat(32),"",true,base).send(message);
            assertThat(fields.get()).containsEntry("ContentSid","HX"+"b".repeat(32))
                    .containsEntry("To","whatsapp:"+message.recipient()).doesNotContainKeys("Body","ContentVariables");
            code.set(429);
            assertThatThrownBy(()->sms.send(message)).hasMessage("Twilio retornou HTTP 429");
        } finally { server.stop(0); }
    }
    @Test void selectedProviderRequiresCredentialsButFakeDoesNot() {
        var fake=new SupportConfig("s","db","fake","fake","fake","x");
        assertThatCode(()->new TwilioSettings(fake,new MockEnvironment())).doesNotThrowAnyException();
        var real=new SupportConfig("s","db","fake","twilio","fake","x");
        assertThatThrownBy(()->new TwilioSettings(real,new MockEnvironment())).hasMessageContaining("TWILIO_ACCOUNT_SID");
        var env=new MockEnvironment().withProperty("TWILIO_ACCOUNT_SID","AC"+"a".repeat(32)).withProperty("TWILIO_AUTH_TOKEN","test-token").withProperty("TWILIO_SMS_FROM","+15550000001");
        assertThat(new TwilioSettings(real,env).notifier("sms").provider()).isEqualTo("twilio");
        env.withProperty("TWILIO_SMS_TRIAL_TEMPLATE","sms_order_confirmation");
        assertThatCode(()->new TwilioSettings(real,env)).doesNotThrowAnyException();
        var whatsapp=new SupportConfig("s","db","fake","fake","twilio","x");
        env.withProperty("TWILIO_WHATSAPP_FROM","whatsapp:+15550000001").withProperty("TWILIO_WHATSAPP_TRIAL_MODE","true");
        assertThatThrownBy(()->new TwilioSettings(whatsapp,env)).hasMessageContaining("obrigatório no modo trial");
        env.withProperty("TWILIO_WHATSAPP_CONTENT_SID","HX"+"a".repeat(32));
        assertThatCode(()->new TwilioSettings(whatsapp,env)).doesNotThrowAnyException();
        env.withProperty("TWILIO_SMS_TRIAL_TEMPLATE","unknown");
        assertThatThrownBy(()->new TwilioSettings(real,env)).hasMessageContaining("TWILIO_SMS_TRIAL_TEMPLATE");
    }
}
