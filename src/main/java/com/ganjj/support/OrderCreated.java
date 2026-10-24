package com.ganjj.support;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public record OrderCreated(String event_id, String event_type, String occurred_at, String order_id,
        String order_code, String customer_id, String customer_email, String customer_phone,
        BigDecimal total, String currency, String correlation_id) {
    public void validate() {
        required(event_id, "event_id", 36);
        try { if (!UUID.fromString(event_id).toString().equalsIgnoreCase(event_id)) invalid("event_id"); }
        catch (IllegalArgumentException ex) { invalid("event_id"); }
        if (!"OrderCreated".equals(event_type)) invalid("event_type");
        try { OffsetDateTime.parse(occurred_at); } catch (Exception ex) { invalid("occurred_at"); }
        required(order_id, "order_id", 256);
        required(order_code, "order_code", 1000);
        required(customer_id, "customer_id", 256);
        required(customer_email, "customer_email", 254);
        if (!Pattern.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+", customer_email)) invalid("customer_email");
        if (customer_phone != null && !customer_phone.isEmpty() && !Pattern.matches("\\+[1-9][0-9]{7,14}", customer_phone)) invalid("customer_phone");
        if (total == null || total.signum() < 0) invalid("total");
        if (!"BRL".equals(currency)) invalid("currency");
        if (correlation_id != null && (correlation_id.length() > 256 || correlation_id.contains("\n") || correlation_id.contains("\r"))) invalid("correlation_id");
    }
    private static void required(String value, String field, int max) {
        if (!StringUtils.hasText(value) || value.length() > max || value.contains("\r") || value.contains("\n")) invalid(field);
    }
    private static void invalid(String field) { throw new IllegalArgumentException("campo inválido: " + field); }
}
