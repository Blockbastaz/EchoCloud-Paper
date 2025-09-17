package dev.echocloud.Events;

import org.bukkit.event.HandlerList;

// Event für erfolgreiche Verbindung
public class EchoCloudConnectionEstablishedEvent extends EchoCloudEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String connectionUrl;
    private final boolean isReconnect;

    public EchoCloudConnectionEstablishedEvent(String communicationType, String serverId,
                                               String connectionUrl, boolean isReconnect) {
        super(communicationType, serverId);
        this.connectionUrl = connectionUrl;
        this.isReconnect = isReconnect;
    }

    public String connectionUrl() {
        return connectionUrl;
    }

    public boolean isReconnect() {
        return isReconnect;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}