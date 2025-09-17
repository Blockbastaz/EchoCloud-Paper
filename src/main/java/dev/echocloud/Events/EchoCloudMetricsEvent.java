package dev.echocloud.Events;

import org.bukkit.event.HandlerList;

// Event für Performance Metriken
public class EchoCloudMetricsEvent extends EchoCloudEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String metricType;
    private final double value;
    private final String unit;
    private final long measurementTime;

    public EchoCloudMetricsEvent(String communicationType, String serverId,
                                 String metricType, double value, String unit) {
        super(communicationType, serverId);
        this.metricType = metricType;
        this.value = value;
        this.unit = unit;
        this.measurementTime = System.currentTimeMillis();
    }

    public String metricType() {
        return metricType;
    }

    public double value() {
        return value;
    }

    public String unit() {
        return unit;
    }

    public long measurementTime() {
        return measurementTime;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}