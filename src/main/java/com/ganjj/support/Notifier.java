package com.ganjj.support;

public interface Notifier {
    record Message(String recipient, String subject, String text) {}
    String channel();
    String provider();
    void send(Message message) throws Exception;
}
