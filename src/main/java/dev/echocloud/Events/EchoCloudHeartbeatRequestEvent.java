package dev.echocloud.Events;

import org.bukkit.event.HandlerList;

// Event für empfangene Heartbeat-Requests
public class EchoCloudHeartbeatRequestEvent extends EchoCloudEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String targetServerId;
    private final String requestData;

    public EchoCloudHeartbeatRequestEvent(String communicationType, String serverId,
                                          String targetServerId, String requestData) {
        super(communicationType, serverId);
        this.targetServerId = targetServerId;
        this.requestData = requestData;
    }

    public String targetServerId() {
        return targetServerId;
    }

    public String requestData() {
        return requestData;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}