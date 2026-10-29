package com.ganjj.support;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

// Endpoint is fixed in production; only package-local tests may supply a loopback server.
final class TwilioNotifier implements Notifier {
    private final String channel, account, auth, from, contentSid, smsTrialTemplate;
    private final URI endpoint;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    TwilioNotifier(String channel,String account,String token,String from,String contentSid,String smsTrialTemplate,URI base) {
        this.channel=channel;this.account=account;this.from=from;this.contentSid=contentSid;this.smsTrialTemplate=smsTrialTemplate;
        auth="Basic "+Base64.getEncoder().encodeToString((account+":"+token).getBytes(StandardCharsets.UTF_8));
        endpoint=base.resolve("/2010-04-01/Accounts/"+account+"/Messages.json");
    }
    public String channel(){return channel;}
    public String provider(){return "twilio";}
    public void send(Message message) throws Exception {
        var fields=new LinkedHashMap<String,String>();
        fields.put("From",from);fields.put("To",("whatsapp".equals(channel)?"whatsapp:":"")+message.recipient());
        if ("whatsapp".equals(channel) && !contentSid.isBlank()) {
            fields.put("ContentSid",contentSid);
            fields.put("ContentVariables",JsonMapper.builder().build().writeValueAsString(Map.of("1",message.subject(),"2",message.text())));
        } else fields.put("Body","sms".equals(channel) && !smsTrialTemplate.isBlank() ? smsTrialTemplate : message.text());
        String encoded=fields.entrySet().stream().map(e->encode(e.getKey())+"="+encode(e.getValue())).collect(java.util.stream.Collectors.joining("&"));
        var request=HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(10)).header("Authorization",auth)
                .header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(encoded)).build();
        var response=client.send(request,HttpResponse.BodyHandlers.discarding());
        if (response.statusCode()<200 || response.statusCode()>=300) throw new IOException("Twilio retornou HTTP "+response.statusCode());
    }
    private static String encode(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
}
