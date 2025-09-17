package dev.echocloud.Events;

import org.bukkit.event.HandlerList;

// Event für empfangene Nachrichten
public class EchoCloudMessageReceivedEvent extends EchoCloudEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String messageContent;
    private final String messageType;
    private final String senderServerId;

    public EchoCloudMessageReceivedEvent(String communicationType, String serverId,
                                         String messageContent, String messageType, String senderServerId) {
        super(communicationType, serverId);
        this.messageContent = messageContent;
        this.messageType = messageType;
        this.senderServerId = senderServerId;
    }

    public String messageContent() {
        return messageContent;
    }

    public String messageType() {
        return messageType;
    }

    public String senderServerId() {
        return senderServerId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}