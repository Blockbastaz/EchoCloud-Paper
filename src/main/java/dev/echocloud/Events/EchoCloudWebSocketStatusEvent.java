package dev.echocloud.Events;

import org.bukkit.event.HandlerList;

// Event für WebSocket-spezifische Operationen
public class EchoCloudWebSocketStatusEvent extends EchoCloudEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String status; // "open", "closed", "error", "message_sent", "message_failed"
    private final int statusCode;
    private final String details;

    public EchoCloudWebSocketStatusEvent(String communicationType, String serverId,
                                         String status, int statusCode, String details) {
        super(communicationType, serverId);
        this.status = status;
        this.statusCode = statusCode;
        this.details = details;
    }

    public String status() {
        return status;
    }

    public int statusCode() {
        return statusCode;
    }

    public String details() {
        return details;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}