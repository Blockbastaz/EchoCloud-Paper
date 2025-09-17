package dev.echocloud.Events;

import org.bukkit.event.HandlerList;

// Event für Verbindungsabbruch
public class EchoCloudConnectionLostEvent extends EchoCloudEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String reason;
    private final Exception exception;
    private final int reconnectAttempt;

    public EchoCloudConnectionLostEvent(String communicationType, String serverId,
                                        String reason, Exception exception, int reconnectAttempt) {
        super(communicationType, serverId);
        this.reason = reason;
        this.exception = exception;
        this.reconnectAttempt = reconnectAttempt;
    }

    public String reason() {
        return reason;
    }

    public Exception exception() {
        return exception;
    }

    public int reconnectAttempt() {
        return reconnectAttempt;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}