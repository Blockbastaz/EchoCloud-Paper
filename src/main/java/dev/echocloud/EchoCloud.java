package dev.echocloud;

import dev.echocloud.API.EchoCloudAPI;
import dev.echocloud.Cloud.CloudCommunication;
import dev.echocloud.Cloud.CloudLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class EchoCloud extends JavaPlugin implements EchoCloudAPI {

    private static EchoCloud instance;
    private CloudCommunication communication;
    private PluginConfig config;
    private Instant startTime;
    private CloudLogger logger;

    @Override
    public void onEnable() {
        instance = this;
        startTime = Instant.now();
        this.logger = new CloudLogger(false);
        logger.info("[EchoCloud] Server startet...");

        try {
            config = new PluginConfig(getDataFolder().toPath(), logger);
            logger.info("[EchoCloud] Konfiguration erfolgreich geladen");
        } catch (IOException e) {
            logger.error("[EchoCloud] Kritischer Fehler: settings.yml fehlt oder konnte nicht geladen werden!");
            logger.error("[EchoCloud] Bitte importiere den Server im EchoCloud CLI neu.", e);
            getServer().shutdown();
            return;
        }

        String serverId = config.getString("websocket.serverId", null);
        String authToken = config.getString("websocket.authToken", null);
        String communicationType = config.getString("communication.type", "websocket");

        if (serverId == null || "UNKNOWN".equals(serverId)) {
            logger.error("[EchoCloud] ServerId ist nicht konfiguriert! Bitte settings.yml überprüfen.");
            getServer().shutdown();
            return;
        }

        if (authToken == null || "CHANGE_ME_TO_YOUR_AUTH_TOKEN".equals(authToken) || authToken.isEmpty()) {
            logger.error("[EchoCloud] AuthToken ist nicht konfiguriert! Bitte settings.yml überprüfen.");
            getServer().shutdown();
            return;
        }

        try {
            communication = CloudCommunication.createFromConfig(config, logger, getServer());

            boolean debugMode = config.getBoolean("logging.debugMode", false);
            logger.setDebug(debugMode);

            int heartbeatTimeout = config.getInt("heartbeat.timeout", 20);
            communication.setHeartbeatTimeout(heartbeatTimeout);

            communication.setMetricsProvider(new PaperMetricsProvider());

            communication.connect();
            logger.info("[EchoCloud] Kommunikation gestartet (Typ: {})", communicationType);

        } catch (Exception e) {
            logger.error("[EchoCloud] Fehler beim Initialisieren der Kommunikation", e);
            logger.debug(e.getMessage());
            getServer().shutdown();
            return;
        }

        getServer().getServicesManager().register(EchoCloudAPI.class, this, this, ServicePriority.Normal);

        logger.info("[EchoCloud] Plugin erfolgreich gestartet! Server-ID: {}", serverId);
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregister(EchoCloudAPI.class, this);
        if (getCommunication() != null) {
            getCommunication().shutdown();
        }
        instance = null;
    }

    private class PaperMetricsProvider implements CloudCommunication.ServerMetricsProvider {

        private final Instant startTime = Instant.now();

        @Override
        public double getTPS() {
            // TPS der letzten 1 Minute auslesen
            double[] tps = Bukkit.getServer().getTPS();
            return tps.length > 0 ? tps[0] : 20.0;
        }

        @Override
        public double getCPUUsage() {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
                double cpuLoad = sunBean.getProcessCpuLoad();
                if (cpuLoad >= 0) {
                    return cpuLoad * 100.0; // Prozent
                }
            }
            return -1; // nicht verfügbar
        }

        @Override
        public double getRAMUsage() {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            return usedMemory / 1024.0 / 1024.0; // in MB
        }

        @Override
        public List<String> getPlayersOnline() {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return players;
        }

        @Override
        public int getMaxPlayers() {
            return Bukkit.getMaxPlayers();
        }

        @Override
        public String getServerState() {
            return "ONLINE";
        }

        @Override
        public boolean isRunning() {
            return Bukkit.getServer().getServerTickManager().isRunningNormally();
        }

        @Override
        public String getStartTime() {
            return startTime.toString();
        }
    }

    public static EchoCloud getInstance() {
        return instance;
    }

    @Override
    public CloudCommunication getCommunication() {
        return communication;
    }

    @Override
    public boolean isConnected() {
        return communication != null && communication.isConnected();
    }

    @Override
    public String getServerId() {
        return config != null ? config.getString("websocket.serverId", "UNKNOWN") : "UNKNOWN";
    }
}
