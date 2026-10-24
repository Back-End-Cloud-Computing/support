package com.ganjj.support;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class Channels {
    public static Notifier fake(String channel) {
        return new Notifier() {
            public String channel() { return channel; }
            public String provider() { return "fake"; }
            public void send(Message message) { LoggerFactory.getLogger(Channels.class).info("Envio simulado channel={}", channel); }
        };
    }
    @Bean Notifier email(SupportConfig config, JavaMailSender sender) {
        if ("fake".equals(config.emailProvider())) return fake("email");
        return new Notifier() {
            public String channel() { return "email"; }
            public String provider() { return "smtp"; }
            public void send(Message message) {
                var mail = new SimpleMailMessage();
                mail.setFrom(config.smtpFrom()); mail.setTo(message.recipient());
                mail.setSubject(message.subject()); mail.setText(message.text()); sender.send(mail);
            }
        };
    }
    @Bean Notifier sms() { return fake("sms"); }
    @Bean Notifier whatsapp() { return fake("whatsapp"); }
}
