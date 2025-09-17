package dev.echocloud.Events;

import org.bukkit.event.Event;

// Base Event Klasse für alle EchoCloud Events (optional, für gemeinsame Properties)
public abstract class EchoCloudEvent extends Event {
    protected final String communicationType;
    protected final String serverId;
    protected final long timestamp;

    public EchoCloudEvent(String communicationType, String serverId) {
        this.communicationType = communicationType;
        this.serverId = serverId;
        this.timestamp = System.currentTimeMillis();
    }

    public String communicationType() {
        return communicationType;
    }

    public String serverId() {
        return serverId;
    }

    public long timestamp() {
        return timestamp;
    }
}

