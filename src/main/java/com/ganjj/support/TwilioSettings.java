package com.ganjj.support;

import java.net.URI;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class TwilioSettings {
    private final String account, token, smsFrom, whatsappFrom, contentSid, smsTrialTemplate;
    private final boolean whatsappTrial;
    public TwilioSettings(SupportConfig config, Environment env) {
        account=env.getProperty("TWILIO_ACCOUNT_SID","");
        token=env.getProperty("TWILIO_AUTH_TOKEN","");
        smsFrom=env.getProperty("TWILIO_SMS_FROM","");
        whatsappFrom=env.getProperty("TWILIO_WHATSAPP_FROM","");
        contentSid=env.getProperty("TWILIO_WHATSAPP_CONTENT_SID","");
        smsTrialTemplate=env.getProperty("TWILIO_SMS_TRIAL_TEMPLATE","");
        whatsappTrial=env.getProperty("TWILIO_WHATSAPP_TRIAL_MODE",Boolean.class,false);
        if ("twilio".equals(config.smsProvider()) || "twilio".equals(config.whatsappProvider())) {
            if (!account.matches("AC[0-9a-fA-F]{32}")) throw new IllegalArgumentException("TWILIO_ACCOUNT_SID inválido");
            if (token.isBlank()) throw new IllegalArgumentException("TWILIO_AUTH_TOKEN é obrigatório");
        }
        if ("twilio".equals(config.smsProvider()) && !smsFrom.matches("\\+[1-9][0-9]{7,14}")) throw new IllegalArgumentException("TWILIO_SMS_FROM inválido");
        if ("twilio".equals(config.whatsappProvider()) && !whatsappFrom.matches("whatsapp:\\+[1-9][0-9]{7,14}")) throw new IllegalArgumentException("TWILIO_WHATSAPP_FROM inválido");
        if ("twilio".equals(config.whatsappProvider()) && whatsappTrial && contentSid.isBlank())
            throw new IllegalArgumentException("TWILIO_WHATSAPP_CONTENT_SID é obrigatório no modo trial WhatsApp");
        if (!contentSid.isBlank() && !contentSid.matches("HX[0-9a-fA-F]{32}")) throw new IllegalArgumentException("TWILIO_WHATSAPP_CONTENT_SID inválido");
        if ("twilio".equals(config.smsProvider()) && !smsTrialTemplate.isBlank() && !"sms_order_confirmation".equals(smsTrialTemplate))
            throw new IllegalArgumentException("TWILIO_SMS_TRIAL_TEMPLATE deve ser sms_order_confirmation ou vazio");
    }
    Notifier notifier(String channel) {
        return new TwilioNotifier(channel,account,token,"sms".equals(channel)?smsFrom:whatsappFrom,contentSid,smsTrialTemplate,whatsappTrial,URI.create("https://api.twilio.com"));
    }
}
