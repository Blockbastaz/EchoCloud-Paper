package dev.echocloud.Cloud;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import redis.clients.jedis.Jedis;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CloudStorage {

    private final String baseUrl;
    private final String serverId;
    private final String authToken;
    private final CloudLogger logger;
    private final Gson gson;
    private final String communicationType;

    private final HttpClient httpClient;

    private Jedis redisClient;
    private final String redisPassword;
    private final int redisDatabase;

    public CloudStorage(String baseUrl, String serverId, String authToken, CloudLogger logger) {
        this(baseUrl, serverId, authToken, logger, "http", "", 0);
    }

    public CloudStorage(String baseUrl, String serverId, String authToken, CloudLogger logger,
                        String redisPassword, int redisDatabase) {
        this(baseUrl, serverId, authToken, logger, "redis", redisPassword, redisDatabase);
    }

    private CloudStorage(String baseUrl, String serverId, String authToken, CloudLogger logger,
                         String communicationType, String redisPassword, int redisDatabase) {
        this.baseUrl = baseUrl;
        this.serverId = serverId;
        this.authToken = authToken;
        this.logger = logger;
        this.gson = new Gson();
        this.communicationType = communicationType.toLowerCase();
        this.redisPassword = redisPassword;
        this.redisDatabase = redisDatabase;

        if ("http".equals(this.communicationType) || "websocket".equals(this.communicationType)) {
            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
        } else {
            this.httpClient = null;
            initializeRedis();
        }
    }

    public static CloudStorage fromCommunication(CloudCommunication communication, CloudLogger logger) {
        if (communication instanceof RedisCommunication) {
            RedisCommunication redis = (RedisCommunication) communication;
            return new CloudStorage(
                    communication.baseUrl,
                    communication.serverId,
                    communication.authToken,
                    logger,
                    "redis",
                    "",
                    0
            );
        } else {
            return new CloudStorage(
                    communication.baseUrl,
                    communication.serverId,
                    communication.authToken,
                    logger
            );
        }
    }

    private void initializeRedis() {
        try {
            String[] parts = baseUrl.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 6379;

            redisClient = new Jedis(host, port);

            if (redisPassword != null && !redisPassword.isEmpty()) {
                redisClient.auth(redisPassword);
            }

            if (redisDatabase != 0) {
                redisClient.select(redisDatabase);
            }

            logger.info("[CloudStorage] Redis-Verbindung initialisiert: {}:{} (DB: {})", host, port, redisDatabase);
        } catch (Exception e) {
            logger.error("[CloudStorage] Fehler beim Initialisieren der Redis-Verbindung: " + e.getMessage());
        }
    }

    /**
     * Speichert Daten in der Cloud
     */
    public CompletableFuture<Boolean> store(String key, Object data) {
        if ("redis".equals(communicationType)) {
            return storeViaRedis(key, data);
        } else {
            return storeViaHttp(key, data);
        }
    }

    private CompletableFuture<Boolean> storeViaHttp(String key, Object data) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("action", "store");
                requestBody.put("key", key);
                requestBody.put("data", data);

                String jsonBody = gson.toJson(requestBody);
                String url = baseUrl + "/api/storage/" + serverId + "/" + authToken;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(Duration.ofSeconds(30))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    logger.debug("[CloudStorage] HTTP: Daten erfolgreich gespeichert - Key: " + key);
                    return true;
                } else {
                    logger.error("[CloudStorage] HTTP: Fehler beim Speichern - Key: " + key + ", Status: " + response.statusCode());
                    return false;
                }

            } catch (Exception e) {
                logger.error("[CloudStorage] HTTP: Exception beim Speichern - Key: " + key + ", Error: " + e.getMessage());
                return false;
            }
        });
    }

    private CompletableFuture<Boolean> storeViaRedis(String key, Object data) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (redisClient == null) {
                    initializeRedis();
                }

                String storageKey = "server:" + serverId + ":" + key;
                String jsonData = gson.toJson(data);

                redisClient.set(storageKey, jsonData);

                logger.debug("[CloudStorage] Redis: Daten erfolgreich gespeichert - Key: " + key);
                return true;

            } catch (Exception e) {
                logger.error("[CloudStorage] Redis: Exception beim Speichern - Key: " + key + ", Error: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Ruft Daten aus der Cloud ab
     */
    public <T> CompletableFuture<T> get(String key, Class<T> clazz) {
        if ("redis".equals(communicationType)) {
            return getViaRedis(key, clazz);
        } else {
            return getViaHttp(key, clazz);
        }
    }

    private <T> CompletableFuture<T> getViaHttp(String key, Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("action", "get");
                requestBody.put("key", key);

                String jsonBody = gson.toJson(requestBody);
                String url = baseUrl + "/api/storage/" + serverId + "/" + authToken;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(Duration.ofSeconds(30))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    StorageResponse storageResponse = gson.fromJson(response.body(), StorageResponse.class);

                    if (storageResponse != null && storageResponse.data != null) {
                        String dataJson = gson.toJson(storageResponse.data);
                        T result = gson.fromJson(dataJson, clazz);
                        logger.debug("[CloudStorage] HTTP: Daten erfolgreich abgerufen - Key: " + key);
                        return result;
                    }
                }

                logger.debug("[CloudStorage] HTTP: Keine Daten gefunden - Key: " + key);
                return null;

            } catch (Exception e) {
                logger.error("[CloudStorage] HTTP: Exception beim Abrufen - Key: " + key + ", Error: " + e.getMessage());
                return null;
            }
        });
    }

    private <T> CompletableFuture<T> getViaRedis(String key, Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (redisClient == null) {
                    initializeRedis();
                }

                String storageKey = "server:" + serverId + ":" + key;
                String jsonData = redisClient.get(storageKey);

                if (jsonData != null && !jsonData.isEmpty()) {
                    T result = gson.fromJson(jsonData, clazz);
                    logger.debug("[CloudStorage] Redis: Daten erfolgreich abgerufen - Key: " + key);
                    return result;
                } else {
                    logger.debug("[CloudStorage] Redis: Keine Daten gefunden - Key: " + key);
                    return null;
                }

            } catch (Exception e) {
                logger.error("[CloudStorage] Redis: Exception beim Abrufen - Key: " + key + ", Error: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Ruft Daten als Map ab
     */
    public CompletableFuture<Map<String, Object>> getAsMap(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if ("redis".equals(communicationType)) {
                    if (redisClient == null) {
                        initializeRedis();
                    }

                    String storageKey = "server:" + serverId + ":" + key;
                    String jsonData = redisClient.get(storageKey);

                    if (jsonData != null && !jsonData.isEmpty()) {
                        Map<String, Object> result = gson.fromJson(jsonData, new TypeToken<Map<String, Object>>(){}.getType());
                        logger.debug("[CloudStorage] Redis: Daten als Map abgerufen - Key: " + key);
                        return result;
                    }
                    return null;
                } else {
                    Map<String, Object> requestBody = new HashMap<>();
                    requestBody.put("action", "get");
                    requestBody.put("key", key);

                    String jsonBody = gson.toJson(requestBody);
                    String url = baseUrl + "/api/storage/" + serverId + "/" + authToken;

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .timeout(Duration.ofSeconds(30))
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        StorageResponse storageResponse = gson.fromJson(response.body(), StorageResponse.class);
                        if (storageResponse != null && storageResponse.data != null) {
                            logger.debug("[CloudStorage] HTTP: Daten als Map abgerufen - Key: " + key);
                            return (Map<String, Object>) storageResponse.data;
                        }
                    }
                    return null;
                }
            } catch (Exception e) {
                logger.error("[CloudStorage] Exception beim Abrufen als Map - Key: " + key + ", Error: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Löscht Daten aus der Cloud
     */
    public CompletableFuture<Boolean> delete(String key) {
        if ("redis".equals(communicationType)) {
            return deleteViaRedis(key);
        } else {
            return deleteViaHttp(key);
        }
    }

    private CompletableFuture<Boolean> deleteViaHttp(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("action", "delete");
                requestBody.put("key", key);

                String jsonBody = gson.toJson(requestBody);
                String url = baseUrl + "/api/storage/" + serverId + "/" + authToken;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(Duration.ofSeconds(30))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    logger.debug("[CloudStorage] HTTP: Daten erfolgreich gelöscht - Key: " + key);
                    return true;
                } else {
                    logger.error("[CloudStorage] HTTP: Fehler beim Löschen - Key: " + key + ", Status: " + response.statusCode());
                    return false;
                }

            } catch (Exception e) {
                logger.error("[CloudStorage] HTTP: Exception beim Löschen - Key: " + key + ", Error: " + e.getMessage());
                return false;
            }
        });
    }

    private CompletableFuture<Boolean> deleteViaRedis(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (redisClient == null) {
                    initializeRedis();
                }

                String storageKey = "server:" + serverId + ":" + key;
                Long deletedCount = redisClient.del(storageKey);

                boolean success = deletedCount > 0;
                if (success) {
                    logger.debug("[CloudStorage] Redis: Daten erfolgreich gelöscht - Key: " + key);
                } else {
                    logger.debug("[CloudStorage] Redis: Keine Daten zum Löschen gefunden - Key: " + key);
                }
                return success;

            } catch (Exception e) {
                logger.error("[CloudStorage] Redis: Exception beim Löschen - Key: " + key + ", Error: " + e.getMessage());
                return false;
            }
        });
    }

    public boolean storeSync(String key, Object data) {
        try {
            return store(key, data).get();
        } catch (Exception e) {
            logger.error("[CloudStorage] Sync Store Fehler - Key: " + key + ", Error: " + e.getMessage());
            return false;
        }
    }

    public <T> T getSync(String key, Class<T> clazz) {
        try {
            return get(key, clazz).get();
        } catch (Exception e) {
            logger.error("[CloudStorage] Sync Get Fehler - Key: " + key + ", Error: " + e.getMessage());
            return null;
        }
    }

    public Map<String, Object> getAsMapSync(String key) {
        try {
            return getAsMap(key).get();
        } catch (Exception e) {
            logger.error("[CloudStorage] Sync GetAsMap Fehler - Key: " + key + ", Error: " + e.getMessage());
            return null;
        }
    }

    public boolean deleteSync(String key) {
        try {
            return delete(key).get();
        } catch (Exception e) {
            logger.error("[CloudStorage] Sync Delete Fehler - Key: " + key + ", Error: " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<Boolean> storeString(String key, String value) {
        return store(key, value);
    }

    public CompletableFuture<String> getString(String key) {
        return get(key, String.class);
    }

    public CompletableFuture<Boolean> storeNumber(String key, Number number) {
        return store(key, number);
    }

    public CompletableFuture<Double> getDouble(String key) {
        return get(key, Double.class);
    }

    public CompletableFuture<Integer> getInteger(String key) {
        return get(key, Integer.class);
    }

    public CompletableFuture<Boolean> storeMap(String key, Map<String, Object> map) {
        return store(key, map);
    }

    public CompletableFuture<Map<String, Object>> getMap(String key) {
        return getAsMap(key);
    }

    /**
     * Schließt die Verbindungen
     */
    public void close() {
        if (redisClient != null) {
            try {
                redisClient.close();
                logger.debug("[CloudStorage] Redis-Verbindung geschlossen");
            } catch (Exception e) {
                logger.error("[CloudStorage] Fehler beim Schließen der Redis-Verbindung: " + e.getMessage());
            }
        }
    }

    // Response-Klasse für HTTP-API
    private static class StorageResponse {
        String status;
        String action;
        String key;
        Object data;
        String message;
    }
}