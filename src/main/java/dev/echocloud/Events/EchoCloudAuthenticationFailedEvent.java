package dev.echocloud.Events;

import org.bukkit.event.HandlerList;

// Event für Authentifizierungsfehler
public class EchoCloudAuthenticationFailedEvent extends EchoCloudEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String authToken;
    private final String errorMessage;
    private final int responseCode;

    public EchoCloudAuthenticationFailedEvent(String communicationType, String serverId,
                                              String authToken, String errorMessage, int responseCode) {
        super(communicationType, serverId);
        this.authToken = authToken;
        this.errorMessage = errorMessage;
        this.responseCode = responseCode;
    }

    public String authToken() {
        return authToken;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public int responseCode() {
        return responseCode;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}