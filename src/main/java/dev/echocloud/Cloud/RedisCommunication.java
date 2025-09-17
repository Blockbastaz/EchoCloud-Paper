package dev.echocloud.Cloud;

import dev.echocloud.Events.Manager.EchoCloudEventManager;
import org.bukkit.Server;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class RedisCommunication extends CloudCommunication {
    private Jedis publishJedis;
    private Jedis subscribeJedis;
    private volatile boolean connected = false;
    private Thread subscribeThread;
    private int reconnectAttempts = 0;
    private EchoCloudEventManager eventManager;

    private String password = "";
    private int database = 0;
    private String channel = "echocloud:all";

    public RedisCommunication(String baseUrl, String serverId, String authToken, CloudLogger logger, Server server) {
        super(baseUrl, serverId, authToken, logger, server);
        this.eventManager = new EchoCloudEventManager("redis", serverId, server);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    @Override
    public void connect() {
        try {
            String[] parts = baseUrl.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 6379;

            publishJedis = new Jedis(host, port);
            subscribeJedis = new Jedis(host, port);

            if (password != null && !password.isEmpty()) {
                publishJedis.auth(password);
                subscribeJedis.auth(password);
            }

            // Datenbank auswählen
            if (database != 0) {
                publishJedis.select(database);
                subscribeJedis.select(database);
            }

            connected = true;
            boolean isReconnect = reconnectAttempts > 0;
            reconnectAttempts = 0;

            logger.info("[EchoCloud] Mit Redis verbunden: {}:{} (DB: {})", host, port, database);

            // CONNECTION ESTABLISHED EVENT FEUERN
            eventManager.fireConnectionEstablished(host + ":" + port, isReconnect);

            // Subscribe in eigenem Thread
            subscribeThread = new Thread(() -> {
                try {
                    subscribeJedis.subscribe(new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, String message) {
                            // REDIS CHANNEL EVENT FEUERN - Message Received
                            eventManager.fireRedisChannelEvent(channel, "message_received", message);
                            handleMessage(message);
                        }

                        @Override
                        public void onSubscribe(String channel, int subscribedChannels) {
                            logger.info("[EchoCloud][Redis] Subscribed zu {}", channel);

                            // REDIS CHANNEL EVENT FEUERN - Subscribe
                            eventManager.fireRedisChannelEvent(channel, "subscribe", null);
                        }

                        @Override
                        public void onUnsubscribe(String channel, int subscribedChannels) {
                            // REDIS CHANNEL EVENT FEUERN - Unsubscribe
                            eventManager.fireRedisChannelEvent(channel, "unsubscribe", null);
                        }
                    }, "echocloud:" + serverId, channel);
                } catch (Exception e) {
                    if (connected) {
                        logger.error("[EchoCloud][Redis] Subscribe Fehler: " + e.getMessage());

                        // CONNECTION LOST EVENT FEUERN
                        eventManager.fireConnectionLost("Subscribe Fehler", e, reconnectAttempts);

                        scheduleReconnect();
                    }
                }
            });
            subscribeThread.setDaemon(true);
            subscribeThread.start();

        } catch (Exception e) {
            connected = false;
            logger.error("[EchoCloud] Redis Verbindung fehlgeschlagen: " + e.getMessage());

            // CONNECTION LOST EVENT FEUERN
            eventManager.fireConnectionLost("Verbindung fehlgeschlagen", e, reconnectAttempts);

            scheduleReconnect();
        }
    }

    private void handleMessage(String message) {
        try {
            // Prüfe auf Heartbeat-Request
            HeartbeatRequest heartbeatRequest = gson.fromJson(message, HeartbeatRequest.class);
            if ("heartbeat_request".equals(heartbeatRequest.type)) {
                // Nur auf Requests für diesen Server oder "all" antworten
                if (serverId.equals(heartbeatRequest.server_id) || "all".equals(heartbeatRequest.server_id)) {

                    // HEARTBEAT REQUEST EVENT FEUERN
                    eventManager.fireHeartbeatRequest(
                            heartbeatRequest.server_id,
                            message
                    );

                    sendHeartbeatResponse(heartbeatRequest);
                }
                return;
            }
        } catch (Exception e) {
            // Kein Heartbeat-Request, normale Nachricht
            // MESSAGE RECEIVED EVENT FEUERN
            eventManager.fireMessageReceived(message, "unknown", "unknown");
        }
    }

    @Override
    public void sendHeartbeatResponse(HeartbeatRequest request) {
        if (!connected || publishJedis == null) {
            logger.warn("[EchoCloud][Redis] Kann Heartbeat nicht senden - nicht verbunden");

            // HEARTBEAT RESPONSE EVENT FEUERN - Failed
            eventManager.fireHeartbeatResponse(
                    false,
                    "Nicht verbunden"
            );
            return;
        }

        try {
            HeartbeatResponse response = createHeartbeatResponse(request);
            String json = gson.toJson(response);

            publishJedis.publish(channel, json);

            // REDIS CHANNEL EVENT FEUERN - Publish
            eventManager.fireRedisChannelEvent(channel, "publish", json);

            // HEARTBEAT RESPONSE EVENT FEUERN - Success
            eventManager.fireHeartbeatResponse(
                    true,
                    json
            );

        } catch (Exception e) {
            logger.error("[EchoCloud][Redis] Fehler beim Senden der Heartbeat-Response: " + e.getMessage());

            // HEARTBEAT RESPONSE EVENT FEUERN - Failed
            eventManager.fireHeartbeatResponse(
                    false,
                    e.getMessage()
            );

            scheduleReconnect();
        }
    }

    @Override
    public void sendShutdownHeartbeat() {
        if (publishJedis == null) {
            logger.warn("[EchoCloud][Redis] Kann Shutdown-Heartbeat nicht senden - keine Verbindung");

            // SHUTDOWN EVENT FEUERN - Failed
            eventManager.fireShutdown("Keine Verbindung verfügbar", false);
            return;
        }

        try {
            HeartbeatResponse shutdownHeartbeat = createShutdownHeartbeat();
            String json = gson.toJson(shutdownHeartbeat);

            publishJedis.publish(channel, json);
            logger.info("[EchoCloud][Redis] Shutdown-Heartbeat gesendet - Server ist jetzt OFFLINE");

            // REDIS CHANNEL EVENT FEUERN - Publish
            eventManager.fireRedisChannelEvent(channel, "publish", json);

            // SHUTDOWN EVENT FEUERN - Success
            eventManager.fireShutdown("Server shutdown", true);

        } catch (Exception e) {
            logger.error("[EchoCloud][Redis] Fehler beim Senden des Shutdown-Heartbeats: " + e.getMessage());

            // SHUTDOWN EVENT FEUERN - Failed
            eventManager.fireShutdown("Shutdown-Fehler: " + e.getMessage(), false);
        }
    }

    private void scheduleReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            logger.error("[EchoCloud][Redis] Maximale Reconnect-Versuche erreicht ({}). Gebe auf.", maxReconnectAttempts);
            return;
        }

        connected = false;
        reconnectAttempts++;
        logger.info("[EchoCloud][Redis] Versuche Reconnect in {}s... (Versuch {}/{})",
                reconnectInterval, reconnectAttempts, maxReconnectAttempts);

        // RECONNECT ATTEMPT EVENT FEUERN und prüfen ob erlaubt
        boolean allowed = eventManager.fireReconnectAttempt(reconnectAttempts, maxReconnectAttempts, reconnectInterval);

        if (allowed) {
            scheduler.schedule(() -> {
                if (!connected) {
                    connect();
                }
            }, reconnectInterval, TimeUnit.SECONDS);
        } else {
            logger.info("[EchoCloud][Redis] Reconnect wurde durch Event-Handler verhindert");
        }
    }

    @Override
    public void sendLogEvent(String playerName, String uuid, String action, boolean forced) {
        if (!connected || publishJedis == null) {
            logger.warn("[EchoCloud][Redis] Kann Log nicht senden - nicht verbunden");

            // LOG EVENT FEUERN - Failed
            eventManager.fireLogEvent(playerName, uuid, action, forced, false);
            return;
        }

        LogRequest log = new LogRequest(playerName, uuid, action, forced, Instant.now().toString());
        String json = gson.toJson(log);
        try {
            publishJedis.publish("echocloud:" + serverId, json);

            // REDIS CHANNEL EVENT FEUERN - Publish
            eventManager.fireRedisChannelEvent("echocloud:" + serverId, "publish", json);

            // LOG EVENT FEUERN - Success
            eventManager.fireLogEvent(playerName, uuid, action, forced, true);

        } catch (Exception e) {
            logger.error("[EchoCloud][Redis] Fehler beim Publish: " + e.getMessage());

            // LOG EVENT FEUERN - Failed
            eventManager.fireLogEvent(playerName, uuid, action, forced, false);

            scheduleReconnect();
        }
    }

    @Override
    public void disconnect() {
        connected = false;

        // SHUTDOWN EVENT FEUERN - Manual disconnect
        eventManager.fireShutdown("Manual disconnect", true);

        if (subscribeThread != null && subscribeThread.isAlive()) {
            subscribeThread.interrupt();
        }

        try {
            if (subscribeJedis != null) {
                subscribeJedis.disconnect();
                subscribeJedis.close();
            }
            if (publishJedis != null) {
                publishJedis.disconnect();
                publishJedis.close();
            }
        } catch (Exception e) {
            logger.error("[EchoCloud][Redis] Fehler beim Disconnect: " + e.getMessage());

            // CONNECTION LOST EVENT FEUERN
            eventManager.fireConnectionLost("Disconnect Fehler", e, 0);
        }
    }

    // Zusätzliche Methode für Server-zu-Server Kommunikation
    public void sendServerMessage(String targetServerId, String messageType, String payload) {
        if (!connected || publishJedis == null) {
            logger.warn("[EchoCloud][Redis] Kann Server-Nachricht nicht senden - nicht verbunden");

            // SERVER COMMUNICATION EVENT FEUERN - Failed
            if (eventManager != null) {
                // Hier müsste eine entsprechende fireServerCommunication Methode im EventManager erstellt werden
                // eventManager.fireServerCommunication(targetServerId, messageType, payload, false);
            }
            return;
        }

        try {
            // Custom Message Format für Server-zu-Server Kommunikation
            ServerMessage serverMessage = new ServerMessage(serverId, targetServerId, messageType, payload, Instant.now().toString());
            String json = gson.toJson(serverMessage);

            String channel = "echocloud:" + (targetServerId.equals("all") ? "all" : targetServerId);
            publishJedis.publish(channel, json);

            // REDIS CHANNEL EVENT FEUERN - Publish
            eventManager.fireRedisChannelEvent(channel, "publish", json);

            // SERVER COMMUNICATION EVENT FEUERN - Success (falls implementiert)
            // eventManager.fireServerCommunication(targetServerId, messageType, payload, true);

        } catch (Exception e) {
            logger.error("[EchoCloud][Redis] Fehler beim Senden der Server-Nachricht: " + e.getMessage());

            // SERVER COMMUNICATION EVENT FEUERN - Failed (falls implementiert)
            // eventManager.fireServerCommunication(targetServerId, messageType, payload, false);

            scheduleReconnect();
        }
    }

    // Zusätzliche Methode für Performance Metriken
    public void sendMetric(String metricType, double value, String unit) {
        if (eventManager != null) {
            // METRICS EVENT FEUERN
            // Hier müsste eine entsprechende fireMetrics Methode im EventManager erstellt werden
            // eventManager.fireMetrics(metricType, value, unit);
        }
    }

    // Getter für EventManager (falls externe Klassen Events feuern wollen)
    public EchoCloudEventManager getEventManager() {
        return eventManager;
    }

    // Inner class für Server-zu-Server Nachrichten
    private static class ServerMessage {
        public final String sender_id;
        public final String target_id;
        public final String message_type;
        public final String payload;
        public final String timestamp;

        public ServerMessage(String senderId, String targetId, String messageType, String payload, String timestamp) {
            this.sender_id = senderId;
            this.target_id = targetId;
            this.message_type = messageType;
            this.payload = payload;
            this.timestamp = timestamp;
        }
    }
}