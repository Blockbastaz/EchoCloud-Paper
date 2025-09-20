package dev.echocloud.Cloud;

import okhttp3.*;
import org.bukkit.Server;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class WebSocketCommunication extends CloudCommunication {
    private final OkHttpClient httpClient;
    private WebSocket webSocket;
    private int reconnectAttempts = 0;

    public WebSocketCommunication(String baseUrl, String serverId, String authToken, CloudLogger logger, boolean trustAllCerts, Server server) {
        super(baseUrl, serverId, authToken, logger, server);

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS);

        if (trustAllCerts) {
            try {
                TrustManager[] trustAll = new TrustManager[]{
                        new X509TrustManager() {
                            public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                            public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        }
                };
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAll, new SecureRandom());
                builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAll[0]);
                builder.hostnameVerifier((hostname, session) -> true);
            } catch (Exception e) {
                throw new RuntimeException("Konnte SSL-Kontext nicht initialisieren", e);
            }
        }

        this.httpClient = builder.build();
    }

    @Override
    public void connect() {
        String wsUrl = baseUrl.replace("http", "ws") + "/ws/" + serverId + "/" + authToken;
        Request request = new Request.Builder().url(wsUrl).build();

        this.webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                connected = true;
                reconnectAttempts = 0;
                logger.info("[EchoCloud] WebSocket verbunden: " + wsUrl);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                logger.debug("[EchoCloud] Nachricht empfangen: " + text);

                handleMessage(text);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                connected = false;
                logger.warn("[EchoCloud] WebSocket geschlossen: " + reason);
                scheduleReconnect();
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                connected = false;
                logger.error("[EchoCloud] WebSocket Fehler: " + t.getMessage());
                scheduleReconnect();
            }
        });
    }

    private void handleMessage(String message) {
        try {
            HeartbeatRequest heartbeatRequest = gson.fromJson(message, HeartbeatRequest.class);
            if ("heartbeat_request".equals(heartbeatRequest.type)) {
                logger.debug("[EchoCloud] Heartbeat-Request empfangen");

                sendHeartbeatResponse(heartbeatRequest);
                return;
            }
        } catch (Exception e) {
            logger.info("[EchoCloud] Nachricht: " + message);
        }
    }

    @Override
    public void sendHeartbeatResponse(HeartbeatRequest request) {
        if (!connected || webSocket == null) {
            logger.warn("[EchoCloud] Kann Heartbeat nicht senden - nicht verbunden");
            return;
        }

        HeartbeatResponse response = createHeartbeatResponse(request);
        String json = gson.toJson(response);

        if (webSocket.send(json)) {
            logger.debug("[EchoCloud] Heartbeat-Response gesendet");
        } else {
            logger.error("[EchoCloud] Fehler beim Senden der Heartbeat-Response");
        }
    }

    @Override
    public void sendShutdownHeartbeat() {
        if (webSocket == null) {
            logger.warn("[EchoCloud] Kann Shutdown-Heartbeat nicht senden - keine Verbindung");
            return;
        }

        try {
            HeartbeatResponse shutdownHeartbeat = createShutdownHeartbeat();
            String json = gson.toJson(shutdownHeartbeat);

            if (webSocket.send(json)) {
                logger.info("[EchoCloud] Shutdown-Heartbeat über WebSocket gesendet - Server ist jetzt OFFLINE");
            } else {
                logger.error("[EchoCloud] Fehler beim Senden des Shutdown-Heartbeats über WebSocket");
            }
        } catch (Exception e) {
            logger.error("[EchoCloud] Fehler beim Erstellen/Senden des Shutdown-Heartbeats: " + e.getMessage());
        }
    }

    private void scheduleReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            logger.error("[EchoCloud] Maximale Reconnect-Versuche erreicht ({}). Gebe auf.", maxReconnectAttempts);
            return;
        }

        reconnectAttempts++;
        logger.info("[EchoCloud] Versuche Reconnect in {}s... (Versuch {}/{})",
                reconnectInterval, reconnectAttempts, maxReconnectAttempts);

        scheduler.schedule(() -> {
            if (!connected) {
                connect();
            }
        }, reconnectInterval, TimeUnit.SECONDS);
    }

    @Override
    public void sendLogEvent(String playerName, String uuid, String action, boolean forced) {
        String url = baseUrl + "/api/logs/" + serverId + "/" + authToken;
        LogRequest log = new LogRequest(playerName, uuid, action, forced, Instant.now().toString());
        RequestBody body = RequestBody.create(gson.toJson(log), MediaType.parse("application/json"));

        Request request = new Request.Builder().url(url).post(body).build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.error("[EchoCloud] Fehler beim Senden von Log: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    logger.error("[EchoCloud] API Fehler: " + response.code());
                    logger.debug("[EchoCloud] Log gesendet: " + response.body().string());
                }
                response.close();
            }
        });
    }

    @Override
    public void disconnect() {
        connected = false;
        cloudStorage.close();
        if (webSocket != null) {
            webSocket.close(1000, "Client disconnect");
        }
    }
}