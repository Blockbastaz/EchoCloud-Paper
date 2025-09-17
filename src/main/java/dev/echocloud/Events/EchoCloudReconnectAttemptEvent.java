package dev.echocloud.Events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class EchoCloudReconnectAttemptEvent extends EchoCloudEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final int attemptNumber;
    private final int maxAttempts;
    private final int delaySeconds;
    private boolean cancelled = false;
    private ReconnectResult result = new ReconnectResult(true);

    public EchoCloudReconnectAttemptEvent(String communicationType, String serverId,
                                          int attemptNumber, int maxAttempts, int delaySeconds) {
        super(communicationType, serverId);
        this.attemptNumber = attemptNumber;
        this.maxAttempts = maxAttempts;
        this.delaySeconds = delaySeconds;
    }

    public int attemptNumber() {
        return attemptNumber;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public int delaySeconds() {
        return delaySeconds;
    }

    public ReconnectResult getResult() {
        return result;
    }

    public void setResult(boolean allowed) {
        this.result = new ReconnectResult(allowed);
        this.cancelled = !allowed;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
        this.result = new ReconnectResult(!cancelled);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public static class ReconnectResult {
        private final boolean allowed;

        public ReconnectResult(boolean allowed) {
            this.allowed = allowed;
        }

        public boolean isAllowed() {
            return allowed;
        }
    }
}