package dev.echocloud.Events;

import org.bukkit.event.HandlerList;

// Event für Server-zu-Server Kommunikation
public class EchoCloudServerCommunicationEvent extends EchoCloudEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String targetServerId;
    private final String messageType;
    private final String payload;
    private final boolean successful;

    public EchoCloudServerCommunicationEvent(String communicationType, String serverId,
                                             String targetServerId, String messageType,
                                             String payload, boolean successful) {
        super(communicationType, serverId);
        this.targetServerId = targetServerId;
        this.messageType = messageType;
        this.payload = payload;
        this.successful = successful;
    }

    public String targetServerId() {
        return targetServerId;
    }

    public String messageType() {
        return messageType;
    }

    public String payload() {
        return payload;
    }

    public boolean successful() {
        return successful;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}