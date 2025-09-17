package dev.echocloud.Events;

import org.bukkit.event.HandlerList;

// Event für Shutdown-Heartbeat
public class EchoCloudShutdownEvent extends EchoCloudEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String shutdownReason;
    private final boolean graceful;

    public EchoCloudShutdownEvent(String communicationType, String serverId,
                                  String shutdownReason, boolean graceful) {
        super(communicationType, serverId);
        this.shutdownReason = shutdownReason;
        this.graceful = graceful;
    }

    public String shutdownReason() {
        return shutdownReason;
    }

    public boolean graceful() {
        return graceful;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}