package dev.echocloud;

import dev.echocloud.Cloud.CloudLogger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PluginConfig {

    private final Path configPath;
    private final CloudLogger logger;
    private final Yaml yaml;
    private final Path dataDirectory;

    private Map<String, Object> configValues;

    public PluginConfig(Path dataDirectory, CloudLogger logger) throws IOException {
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        if (!Files.exists(dataDirectory)) {
            Files.createDirectories(dataDirectory);
            logger.info("[EchoCloud] Plugin-Datenverzeichnis erstellt: {}", dataDirectory);
        }

        this.configPath = dataDirectory.resolve("settings.yml");

        // LoaderOptions für SafeConstructor
        LoaderOptions loaderOptions = new LoaderOptions();

        // DumperOptions für Output-Formatierung
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);

        // Yaml mit korrekten Parametern initialisieren
        this.yaml = new Yaml(new SafeConstructor(loaderOptions), new Representer(dumperOptions), dumperOptions);

        this.load();
    }

    private void load() throws IOException {
        if (!Files.exists(configPath)) {
            logger.warn("[EchoCloud] Settings.yml nicht gefunden. Erstelle Standardkonfiguration...");
            createDefaultConfig();
        }

        try (InputStream in = Files.newInputStream(configPath)) {
            configValues = yaml.load(in);
            if (configValues == null) {
                configValues = new HashMap<>();
            }
        }
        logger.info("[EchoCloud] Settings.yml erfolgreich geladen von: {}", configPath);
    }

    private void createDefaultConfig() throws IOException {
        Map<String, Object> defaultConfig = new LinkedHashMap<>();
        String serverId = this.detectServerId(this.dataDirectory);
        this.logger.info("[EchoCloud] Erkannte serverId = {}", serverId);

        Map<String, Object> communicationConfig = new LinkedHashMap<>();
        communicationConfig.put("type", "redis");
        communicationConfig.put("reconnectInterval", 30);
        communicationConfig.put("maxReconnectAttempts", 5);
        defaultConfig.put("communication", communicationConfig);

        Map<String, Object> apiConfig = new LinkedHashMap<>();
        apiConfig.put("host", "localhost");
        apiConfig.put("port", 8080);
        apiConfig.put("serverId", serverId);
        apiConfig.put("authToken", "CHANGE_ME_TO_YOUR_AUTH_TOKEN");
        apiConfig.put("useHttps", true);
        defaultConfig.put("websocket", apiConfig);

        Map<String, Object> redisConfig = new LinkedHashMap<>();
        redisConfig.put("host", "127.0.0.1");
        redisConfig.put("port", 6379);
        redisConfig.put("password", "");
        redisConfig.put("database", 0);
        redisConfig.put("channel", "echocloud:all");
        defaultConfig.put("redis", redisConfig);

        Map<String, Object> loggingConfig = new LinkedHashMap<>();
        loggingConfig.put("logPlayerJoins", true);
        loggingConfig.put("logPlayerLeaves", true);
        loggingConfig.put("logServerSwitches", true);
        loggingConfig.put("debugMode", false);
        defaultConfig.put("logging", loggingConfig);

        Map<String, Object> heartbeatConfig = new LinkedHashMap<>();
        heartbeatConfig.put("interval", 10);
        heartbeatConfig.put("timeout", 20);
        heartbeatConfig.put("sendPlayerCount", true);
        heartbeatConfig.put("sendPerformanceData", true);
        defaultConfig.put("heartbeat", heartbeatConfig);

        this.configValues = defaultConfig;
        this.save();
        this.logger.info("[EchoCloud] Standardkonfiguration erstellt. Bitte passe die Werte in settings.yml an!");
        this.logger.warn("[EchoCloud] WICHTIG: Setze deinen AuthToken und Server-Details bevor du das Plugin verwendest!");
    }

    public void save() throws IOException {
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            yaml.dump(configValues, writer);
        }
        logger.info("[EchoCloud] Settings gespeichert in: {}", configPath);
    }

    public Object get(String key) {
        return getNestedValue(configValues, key);
    }

    public String getString(String key, String def) {
        Object value = getNestedValue(configValues, key);
        return value != null ? value.toString() : def;
    }

    public int getInt(String key, int def) {
        Object value = getNestedValue(configValues, key);
        if (value instanceof Number num) {
            return num.intValue();
        }
        return def;
    }

    public boolean getBoolean(String key, boolean def) {
        Object value = getNestedValue(configValues, key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return def;
    }

    public void set(String key, Object value) {
        setNestedValue(configValues, key, value);
    }

    private String detectServerId(Path dataDirectory) {
        try {
            Path current = dataDirectory.toAbsolutePath();

            while (current != null && current.getParent() != null) {
                String currentName = current.getFileName().toString();

                if ("plugins".equalsIgnoreCase(currentName)) {
                    Path serverDir = current.getParent();
                    if (serverDir != null && serverDir.getFileName() != null) {
                        String serverName = serverDir.getFileName().toString();
                        logger.info("[EchoCloud] Server-Verzeichnis erkannt: {}", serverName);
                        return serverName;
                    }
                }

                current = current.getParent();
            }

            Path parent = dataDirectory.getParent();
            if (parent != null && parent.getParent() != null) {
                Path grandParent = parent.getParent();
                if (grandParent.getFileName() != null) {
                    String fallbackName = grandParent.getFileName().toString();
                    logger.info("[EchoCloud] Fallback Server-Name: {}", fallbackName);
                    return fallbackName;
                }
            }

        } catch (Exception e) {
            logger.warn("[EchoCloud] Fehler bei der Server-ID Erkennung: {}", e.getMessage());
        }

        logger.warn("[EchoCloud] Konnte Server-ID nicht automatisch ermitteln");
        return "paper-server"; // Geändert von "velocity-server" zu "paper-server"
    }

    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> map, String key) {
        if (!key.contains(".")) {
            return map.get(key);
        }

        String[] parts = key.split("\\.", 2);
        Object value = map.get(parts[0]);

        if (value instanceof Map) {
            return getNestedValue((Map<String, Object>) value, parts[1]);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private void setNestedValue(Map<String, Object> map, String key, Object value) {
        if (!key.contains(".")) {
            map.put(key, value);
            return;
        }

        String[] parts = key.split("\\.", 2);
        Object existingValue = map.get(parts[0]);

        Map<String, Object> nestedMap;
        if (existingValue instanceof Map) {
            nestedMap = (Map<String, Object>) existingValue;
        } else {
            nestedMap = new LinkedHashMap<>();
            map.put(parts[0], nestedMap);
        }

        setNestedValue(nestedMap, parts[1], value);
    }

    public Path getConfigDirectory() {
        return configPath.getParent();
    }

    public Path getConfigPath() {
        return configPath;
    }
}