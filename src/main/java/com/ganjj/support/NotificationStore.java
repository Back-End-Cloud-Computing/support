package com.ganjj.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class NotificationStore {
    public record Delivery(String channel, String status, String provider, int attempts, String error) {}
    private final String url;
    private final ObjectMapper mapper;
    public NotificationStore(SupportConfig config, ObjectMapper mapper) throws Exception {
        this.mapper = mapper;
        Path file = Path.of(config.dbPath()).toAbsolutePath();
        Files.createDirectories(file.getParent());
        url = "jdbc:sqlite:" + file;
        try (var c = open(); var s = c.createStatement()) {
            s.execute("PRAGMA journal_mode=WAL");
            s.execute("CREATE TABLE IF NOT EXISTS events (event_id TEXT PRIMARY KEY, event_type TEXT NOT NULL, order_id TEXT NOT NULL, received_at TEXT NOT NULL, payload TEXT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS deliveries (id INTEGER PRIMARY KEY AUTOINCREMENT, event_id TEXT NOT NULL REFERENCES events(event_id), channel TEXT NOT NULL, status TEXT NOT NULL, attempts INTEGER NOT NULL, error TEXT, provider TEXT NOT NULL, updated_at TEXT NOT NULL, UNIQUE(event_id, channel))");
        }
    }
    private Connection open() throws SQLException {
        var c = DriverManager.getConnection(url);
        try (var s = c.createStatement()) { s.execute("PRAGMA busy_timeout=5000"); s.execute("PRAGMA foreign_keys=ON"); }
        return c;
    }
    public boolean insert(OrderCreated event) throws SQLException {
        try (var c=open(); var s=c.prepareStatement("INSERT INTO events VALUES (?, ?, ?, ?, ?) ON CONFLICT(event_id) DO NOTHING")) {
            s.setString(1,event.event_id()); s.setString(2,event.event_type()); s.setString(3,event.order_id());
            s.setString(4,Instant.now().toString()); s.setString(5,mapper.writeValueAsString(event));
            return s.executeUpdate() == 1;
        }
    }
    public Optional<List<Delivery>> deliveries(String id) throws SQLException {
        try (var c=open(); var exists=c.prepareStatement("SELECT 1 FROM events WHERE event_id=?")) {
            exists.setString(1,id);
            try (var rows=exists.executeQuery()) { if (!rows.next()) return Optional.empty(); }
            try (var s=c.prepareStatement("SELECT channel,status,provider,attempts,error FROM deliveries WHERE event_id=? ORDER BY id")) {
                s.setString(1,id);
                var result=new ArrayList<Delivery>();
                try (var rows=s.executeQuery()) { while(rows.next()) result.add(new Delivery(rows.getString(1),rows.getString(2),rows.getString(3),rows.getInt(4),rows.getString(5))); }
                return Optional.of(result);
            }
        }
    }
    public List<OrderCreated> pending() throws SQLException {
        try (var c=open(); var s=c.prepareStatement("SELECT payload FROM events e WHERE (SELECT COUNT(*) FROM deliveries d WHERE d.event_id=e.event_id)<3 ORDER BY received_at LIMIT 10"); var rows=s.executeQuery()) {
            var result=new ArrayList<OrderCreated>();
            while (rows.next()) result.add(mapper.readValue(rows.getString(1),OrderCreated.class));
            return result;
        }
    }
    public void save(String id, Delivery delivery) throws SQLException {
        try (var c=open(); var s=c.prepareStatement("INSERT INTO deliveries(event_id,channel,status,attempts,error,provider,updated_at) VALUES (?,?,?,?,?,?,?) ON CONFLICT(event_id,channel) DO NOTHING")) {
            s.setString(1,id); s.setString(2,delivery.channel()); s.setString(3,delivery.status()); s.setInt(4,delivery.attempts());
            s.setString(5,delivery.error()); s.setString(6,delivery.provider()); s.setString(7,Instant.now().toString()); s.executeUpdate();
        }
    }
    public boolean ready() { try (var c=open(); var s=c.createStatement()) { s.execute("SELECT 1 FROM events LIMIT 1"); return true; } catch(SQLException e) { return false; } }
}
