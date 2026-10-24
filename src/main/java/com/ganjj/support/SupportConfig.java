package com.ganjj.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public record SupportConfig(String secret, String dbPath, String emailProvider,
                            String smsProvider, String whatsappProvider, String smtpFrom) {
    @Autowired
    public SupportConfig(Environment env) {
        this(env.getProperty("support.secret", ""), env.getProperty("support.db-path"),
             env.getProperty("support.email-provider", "smtp"), env.getProperty("support.sms-provider", "fake"),
             env.getProperty("support.whatsapp-provider", "fake"), env.getProperty("support.smtp-from"));
    }
    public SupportConfig {
        if (secret == null || secret.isBlank()) throw new IllegalArgumentException("INTERNAL_SECRET é obrigatório");
        if (!"smtp".equals(emailProvider) && !"fake".equals(emailProvider)) throw new IllegalArgumentException("EMAIL_PROVIDER inválido");
        if (!"fake".equals(smsProvider)) throw new IllegalArgumentException("SMS_PROVIDER inválido");
        if (!"fake".equals(whatsappProvider)) throw new IllegalArgumentException("WHATSAPP_PROVIDER inválido");
    }
}
