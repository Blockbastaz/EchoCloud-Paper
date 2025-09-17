package dev.echocloud.Events.Manager;

import dev.echocloud.Events.*;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;

/**
 * Manager-Klasse zum einfachen Feuern von EchoCloud Events für Paper
 */
public class EchoCloudEventManager {
    private final String communicationType;
    private final String serverId;
    private final Server server;
    private final PluginManager pluginManager;

    public EchoCloudEventManager(String communicationType, String serverId, Server server) {
        this.communicationType = communicationType;
        this.serverId = serverId;
        this.server = server;
        this.pluginManager = server.getPluginManager();
    }

    /**
     * Feuert ein Connection Established Event
     */
    public void fireConnectionEstablished(String connectionUrl, boolean isReconnect) {
        EchoCloudConnectionEstablishedEvent event = new EchoCloudConnectionEstablishedEvent(
                communicationType, serverId, connectionUrl, isReconnect
        );
        pluginManager.callEvent(event);
    }

    /**
     * Feuert ein Connection Lost Event
     */
    public void fireConnectionLost(String reason, Exception exception, int reconnectAttempt) {
        EchoCloudConnectionLostEvent event = new EchoCloudConnectionLostEvent(
                communicationType, serverId, reason, exception, reconnectAttempt
        );
        pluginManager.callEvent(event);
    }

    /**
     * Feuert ein Heartbeat Request Event
     */
    public void fireHeartbeatRequest(String targetServerId, String requestData) {
        EchoCloudHeartbeatRequestEvent event = new EchoCloudHeartbeatRequestEvent(
                communicationType, serverId, targetServerId, requestData
        );
        pluginManager.callEvent(event);
    }

    /**
     * Feuert ein Heartbeat Response Event
     */
    public void fireHeartbeatResponse(boolean successful, String responseData) {
        EchoCloudHeartbeatResponseEvent event = new EchoCloudHeartbeatResponseEvent(
                communicationType, serverId, successful, responseData
        );
        pluginManager.callEvent(event);
    }

    /**
     * Feuert ein Shutdown Event
     */
    public void fireShutdown(String shutdownReason, boolean graceful) {
        EchoCloudShutdownEvent event = new EchoCloudShutdownEvent(
                communicationType, serverId, shutdownReason, graceful
        );
        pluginManager.callEvent(event);
    }

    /**
     * Feuert ein Log Event
     */
    public void fireLogEvent(String playerName, String uuid, String action, boolean forced, boolean successful) {
        EchoCloudLogEvent event = new EchoCloudLogEvent(
                communicationType, serverId, playerName, uuid, action, forced, successful
        );
        pluginManager.callEvent(event);
    }

    /**
     * Feuert ein Message Received Event
     */
    public void fireMessageReceived(String messageContent, String messageType, String senderServerId) {
        EchoCloudMessageReceivedEvent event = new EchoCloudMessageReceivedEvent(
                communicationType, serverId, messageContent, messageType, senderServerId
        );
        pluginManager.callEvent(event);
    }

    /**
     * Feuert ein Reconnect Attempt Event und gibt zurück, ob es erlaubt ist
     */
    public boolean fireReconnectAttempt(int attemptNumber, int maxAttempts, int delaySeconds) {
        EchoCloudReconnectAttemptEvent event = new EchoCloudReconnectAttemptEvent(
                communicationType, serverId, attemptNumber, maxAttempts, delaySeconds
        );

        // Event synchron feuern - in Bukkit/Paper ist callEvent bereits synchron
        pluginManager.callEvent(event);

        // Prüfen ob das Event gecancelt wurde oder einen spezifischen Rückgabewert hat
        // Falls das Event Cancellable implementiert:
        return !event.isCancelled();

    }

    /**
     * Feuert ein Authentication Failed Event
     */
    public void fireAuthenticationFailed(String authToken, String errorMessage, int responseCode) {
        EchoCloudAuthenticationFailedEvent event = new EchoCloudAuthenticationFailedEvent(
                communicationType, serverId, authToken, errorMessage, responseCode
        );
        pluginManager.callEvent(event);
    }

    /**
     * Feuert ein Redis Channel Event (nur für Redis-Kommunikation)
     */
    public void fireRedisChannelEvent(String channelName, String operation, String message) {
        if ("redis".equals(communicationType)) {
            EchoCloudRedisChannelEvent event = new EchoCloudRedisChannelEvent(
                    communicationType, serverId, channelName, operation, message
            );
            pluginManager.callEvent(event);
        }
    }

    /**
     * Feuert ein WebSocket Status Event (nur für WebSocket-Kommunikation)
     */
    public void fireWebSocketStatus(String status, int statusCode, String details) {
        if ("websocket".equals(communicationType)) {
            EchoCloudWebSocketStatusEvent event = new EchoCloudWebSocketStatusEvent(
                    communicationType, serverId, status, statusCode, details
            );
            pluginManager.callEvent(event);
        }
    }

    /**
     * Hilfsmethode um den Kommunikationstyp zu bekommen
     */
    public String getCommunicationType() {
        return communicationType;
    }

    /**
     * Hilfsmethode um die Server-ID zu bekommen
     */
    public String getServerId() {
        return serverId;
    }

    /**
     * Hilfsmethode um den Server zu bekommen
     */
    public Server getServer() {
        return server;
    }

    /**
     * Hilfsmethode um den PluginManager zu bekommen
     */
    public PluginManager getPluginManager() {
        return pluginManager;
    }
}