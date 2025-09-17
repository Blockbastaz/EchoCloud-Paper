package dev.echocloud.Events;

import org.bukkit.event.HandlerList;

// Event für Log-Ereignisse
public class EchoCloudLogEvent extends EchoCloudEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String playerName;
    private final String uuid;
    private final String action;
    private final boolean forced;
    private final boolean successful;

    public EchoCloudLogEvent(String communicationType, String serverId,
                             String playerName, String uuid, String action,
                             boolean forced, boolean successful) {
        super(communicationType, serverId);
        this.playerName = playerName;
        this.uuid = uuid;
        this.action = action;
        this.forced = forced;
        this.successful = successful;
    }

    public String playerName() {
        return playerName;
    }

    public String uuid() {
        return uuid;
    }

    public String action() {
        return action;
    }

    public boolean forced() {
        return forced;
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