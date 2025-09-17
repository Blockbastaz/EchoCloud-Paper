package dev.echocloud.Cloud;

import com.google.gson.Gson;
import dev.echocloud.PluginConfig;
import org.bukkit.Server;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class CloudCommunication {

    protected final String serverId;
    protected final String authToken;
    protected final String baseUrl;
    protected final CloudLogger logger;
    protected final Gson gson = new Gson();
    protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    protected final Server server;

    protected ServerMetricsProvider metricsProvider;

    protected int reconnectInterval = 30; // seconds
    protected int maxReconnectAttempts = 5;
    protected int heartbeatTimeout = 20; // seconds

    public interface ServerMetricsProvider {
        double getTPS();
        double getCPUUsage();
        double getRAMUsage();
        List<String> getPlayersOnline();
        int getMaxPlayers();
        String getServerState();
        boolean isRunning();
        String getStartTime(); // ISO format
    }

    protected CloudCommunication(String baseUrl, String serverId, String authToken, CloudLogger logger, Server server) {
        this.baseUrl = baseUrl;
        this.serverId = serverId;
        this.authToken = authToken;
        this.logger = logger;
        this.server = server;
    }

    public void setMetricsProvider(ServerMetricsProvider provider) {
        this.metricsProvider = provider;
    }

    public void setReconnectInterval(int seconds) {
        this.reconnectInterval = seconds;
    }

    public void setMaxReconnectAttempts(int attempts) {
        this.maxReconnectAttempts = attempts;
    }

    public void setHeartbeatTimeout(int seconds) {
        this.heartbeatTimeout = seconds;
    }

    public abstract void connect();
    public abstract void sendLogEvent(String playerName, String uuid, String action, boolean forced);
    public abstract void sendHeartbeatResponse(HeartbeatRequest request);
    public abstract void sendShutdownHeartbeat();
    public abstract void disconnect();

    protected HeartbeatResponse createHeartbeatResponse(HeartbeatRequest request) {
        if (metricsProvider == null) {
            logger.warn("[EchoCloud] MetricsProvider nicht gesetzt - verwende Standard-Werte");
            return new HeartbeatResponse(
                    "heartbeat_response",
                    serverId,
                    request.timestamp,
                    Instant.now().toString(),
                    "OFFLINE",
                    false,
                    0.0, 0.0, 0.0,
                    List.of(), 0, null
            );
        }

        return new HeartbeatResponse(
                "heartbeat_response",
                serverId,
                request.timestamp,
                Instant.now().toString(),
                metricsProvider.getServerState(),
                metricsProvider.isRunning(),
                metricsProvider.getTPS(),
                metricsProvider.getCPUUsage(),
                metricsProvider.getRAMUsage(),
                metricsProvider.getPlayersOnline(),
                metricsProvider.getMaxPlayers(),
                metricsProvider.getStartTime()
        );
    }

    protected HeartbeatResponse createShutdownHeartbeat() {
        return new HeartbeatResponse(
                "shutdown_notification",
                serverId,
                Instant.now().toString(), // request_timestamp
                Instant.now().toString(), // response_timestamp
                "OFFLINE",
                false,
                0.0, // TPS zurückgesetzt
                0.0, // CPU zurückgesetzt
                0.0, // RAM zurückgesetzt
                List.of(), // Keine Spieler online
                metricsProvider != null ? metricsProvider.getMaxPlayers() : 0,
                null // Startzeit zurückgesetzt
        );
    }

    public void shutdown() {
        logger.info("[EchoCloud] Sende Shutdown-Benachrichtigung...");

        try {
            // Finalen Heartbeat senden um Server über Shutdown zu informieren
            sendShutdownHeartbeat();
            logger.info("[EchoCloud] Shutdown-Heartbeat gesendet");

            // Kurz warten damit der Heartbeat noch gesendet werden kann
            Thread.sleep(1000);
        } catch (Exception e) {
            logger.error("[EchoCloud] Fehler beim Senden des Shutdown-Heartbeats: " + e.getMessage());
        }

        disconnect();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static CloudCommunication create(String type, String baseUrl, String serverId, String authToken, CloudLogger logger, Server server) {
        if ("Redis".equalsIgnoreCase(type)) {
            return new RedisCommunication(baseUrl, serverId, authToken, logger, server);
        } else {
            return new WebSocketCommunication(baseUrl, serverId, authToken, logger, true, server);
        }
    }

    public static CloudCommunication createFromConfig(PluginConfig config, CloudLogger logger, Server server) {
        String communicationType = config.getString("communication.type", "websocket").toLowerCase();

        if ("redis".equals(communicationType)) {
            String host = config.getString("redis.host", "127.0.0.1");
            int port = config.getInt("redis.port", 6379);
            String serverId = config.getString("websocket.serverId", "UNKNOWN");
            String authToken = config.getString("websocket.authToken", "");

            String redisUrl = host + ":" + port;
            RedisCommunication redis = new RedisCommunication(redisUrl, serverId, authToken, logger, server);

            // Redis
            redis.setPassword(config.getString("redis.password", ""));
            redis.setDatabase(config.getInt("redis.database", 0));
            redis.setChannel(config.getString("redis.channel", "echocloud:all"));
            redis.setReconnectInterval(config.getInt("communication.reconnectInterval", 30));
            redis.setMaxReconnectAttempts(config.getInt("communication.maxReconnectAttempts", 5));

            return redis;
        } else {
            // WebSocket
            String host = config.getString("websocket.host", "localhost");
            int port = config.getInt("websocket.port", 8080);
            boolean useHttps = config.getBoolean("websocket.useHttps", true);
            String serverId = config.getString("websocket.serverId", "UNKNOWN");
            String authToken = config.getString("websocket.authToken", "");

            String protocol = useHttps ? "https" : "http";
            String baseUrl = protocol + "://" + host + ":" + port;

            WebSocketCommunication ws = new WebSocketCommunication(baseUrl, serverId, authToken, logger, true, server);
            ws.setReconnectInterval(config.getInt("communication.reconnectInterval", 30));
            ws.setMaxReconnectAttempts(config.getInt("communication.maxReconnectAttempts", 5));

            return ws;
        }
    }

    protected static class LogRequest {
        String playerName;
        String uuid;
        String action;
        boolean forced;
        String timestamp;

        LogRequest(String playerName, String uuid, String action, boolean forced, String timestamp) {
            this.playerName = playerName;
            this.uuid = uuid;
            this.action = action;
            this.forced = forced;
            this.timestamp = timestamp;
        }
    }

    protected static class HeartbeatRequest {
        String type;
        String server_id;
        String timestamp;

        HeartbeatRequest() {}
    }

    protected static class HeartbeatResponse {
        String type;
        String server_id;
        String request_timestamp;
        String response_timestamp;
        String server_state;
        boolean is_running;
        double tps;
        double cpu_usage;
        double ram_usage_mb;
        List<String> players_online;
        int max_players;
        String start_time;

        HeartbeatResponse(String type, String server_id, String request_timestamp,
                          String response_timestamp, String server_state, boolean is_running,
                          double tps, double cpu_usage, double ram_usage_mb,
                          List<String> players_online, int max_players, String start_time) {
            this.type = type;
            this.server_id = server_id;
            this.request_timestamp = request_timestamp;
            this.response_timestamp = response_timestamp;
            this.server_state = server_state;
            this.is_running = is_running;
            this.tps = tps;
            this.cpu_usage = cpu_usage;
            this.ram_usage_mb = ram_usage_mb;
            this.players_online = players_online;
            this.max_players = max_players;
            this.start_time = start_time;
        }
    }
}