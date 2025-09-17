package dev.echocloud.Events;

import org.bukkit.event.HandlerList;

// Event für Redis-spezifische Channel-Operationen
public class EchoCloudRedisChannelEvent extends EchoCloudEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String channelName;
    private final String operation; // "subscribe", "unsubscribe", "publish", "message_received"
    private final String message;

    public EchoCloudRedisChannelEvent(String communicationType, String serverId,
                                      String channelName, String operation, String message) {
        super(communicationType, serverId);
        this.channelName = channelName;
        this.operation = operation;
        this.message = message;
    }

    public String channelName() {
        return channelName;
    }

    public String operation() {
        return operation;
    }

    public String message() {
        return message;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}