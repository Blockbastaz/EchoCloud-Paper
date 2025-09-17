package dev.echocloud.Events;

import org.bukkit.event.HandlerList;

// Event für gesendete Heartbeat-Responses
public class EchoCloudHeartbeatResponseEvent extends EchoCloudEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final boolean successful;
    private final String responseData;

    public EchoCloudHeartbeatResponseEvent(String communicationType, String serverId,
                                           boolean successful, String responseData) {
        super(communicationType, serverId);
        this.successful = successful;
        this.responseData = responseData;
    }

    public boolean successful() {
        return successful;
    }

    public String responseData() {
        return responseData;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}