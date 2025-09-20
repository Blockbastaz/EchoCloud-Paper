<p align="center">
  <img src="https://github.com/Blockbastaz/EchoCloud/blob/main/data/logo.png" alt="EchoCloud Logo" width="200"/>
</p>

<h1 align="center">EchoCloud Paper Integration</h1>

<p align="center">
  <strong>Verbinde deine Paper-Server mit der EchoCloud Plattform</strong><br/>
  <em>Echtzeit-Kommunikation • Zentrale Kontrolle • Skalierbar</em>
</p>

---

## ⚡ Übersicht

Die **EchoCloud Paper Integration** verbindet deine Paper-Server direkt mit der [EchoCloud](https://github.com/Blockbastaz/EchoCloud) Plattform.  
Sie ermöglicht eine sichere bidirektionale Kommunikation zwischen EchoCloud und deinem Server, inklusive Eventsystem, Logging, automatischer Status-Übermittlung und Remote-Befehlen.

> 🧠 Diese Integration ist ein **Plugin für Paper**, das mit der EchoCloud API kommuniziert und Events, Logs sowie Metriken an die Cloud sendet.

---

## 📦 Features

- 🔌 **CloudCommunication** – Sichere WebSocket- und Redis-Kommunikation mit EchoCloud
- 📡 **Eventsystem** – Vollständig typisiertes Eventsystem für Paper (Bukkit-Events)
- 🧠 **Heartbeat-System** – Automatische Statusübermittlung und Crash-Erkennung
- 📋 **CloudLogger** – Zentrale Log-Erfassung aller Server-Ereignisse
- ⚡ **Live-Kommandos** – EchoCloud kann Serverbefehle remote ausführen
- 📈 **Metrics-Events** – Übermittlung von Spielerzahlen, TPS, Ping u.v.m.
- 🔐 **Authentifizierung** – Token-basierte Absicherung der Kommunikation

---

## ⚙️ Voraussetzungen

- Paper 1.21+
- Java 17+
- Verbindung zu einer laufenden [EchoCloud-Instanz](https://github.com/Blockbastaz/EchoCloud)

---

## 🚀 Installation

1. Baue das Projekt als JAR (`gradle build` oder `mvn package`)
2. Kopiere die fertige JAR in den `plugins/`-Ordner deines Paper-Servers
3. Starte den Server neu

---

## ⚙️ Konfiguration

Die grundlegenden Einstellungen befinden sich unter `plugins/echocloud`. In `settings.yml`.

---

## ⚡ Events

Die Integration stellt ein umfangreiches Eventsystem zur Verfügung, das als normale Bukkit-Events funktioniert.  
Sie werden wie gewohnt mit `@EventHandler` registriert:

| Event                                 | Beschreibung                                              |
|---------------------------------------|-----------------------------------------------------------|
| `EchoCloudAuthenticationFailedEvent`  | Authentifizierung bei der Cloud fehlgeschlagen            |
| `EchoCloudConnectionEstablishedEvent` | Verbindung zur Cloud erfolgreich aufgebaut                |
| `EchoCloudConnectionLostEvent`        | Verbindung zur Cloud verloren                             |
| `EchoCloudHeartbeatRequestEvent`      | Heartbeat-Anfrage von der Cloud empfangen                 |
| `EchoCloudHeartbeatResponseEvent`     | Heartbeat-Antwort an die Cloud gesendet                   |
| `EchoCloudLogEvent`                   | Log-Nachricht zur Cloud gesendet                          |
| `EchoCloudMessageReceivedEvent`       | Nachricht von der Cloud empfangen                         |
| `EchoCloudMetricsEvent`               | Metriken (z. B. Spieleranzahl, TPS) an die Cloud gesendet |
| `EchoCloudReconnectAttemptEvent`      | Ein erneuter Verbindungsversuch zur Cloud wird gestartet  |
| `EchoCloudRedisChannelEvent`          | Nachricht über Redis-Kanal empfangen                      |
| `EchoCloudServerCommunicationEvent`   | Serverseitige Kommunikation an/aus der Cloud              |
| `EchoCloudShutdownEvent`              | Verbindung zur Cloud beim Shutdown getrennt               |
| `EchoCloudWebSocketStatusEvent`       | Statusänderung der WebSocket-Verbindung erkannt           |

---


## 📡 Kommunikation

Die Integration nutzt sowohl **WebSocketCommunication** (bidirektional) als auch **RedisCommunication** (Broadcast) für extrem schnelle Übertragung an EchoCloud.

### Beispiel: Nachricht an EchoCloud senden

```java
CloudCommunication.sendMessage("Hello Cloud!");
```

### Beispiel: Event registrieren

```java
public class MyListener implements Listener {

    @EventHandler
    public void onConnect(EchoCloudConnectionEstablishedEvent event) {
        Bukkit.getLogger().info("Verbindung zur Cloud hergestellt!");
    }
}

// in onEnable():
Bukkit.getPluginManager().registerEvents(new MyListener(), this);

```

---

## ⚡ CLI Integration

Die Steuerung deines Velocity-Servers erfolgt über die [EchoCloud CLI](https://github.com/Blockbastaz/EchoCloud).
Dort kannst du:

* Server starten/stoppen
* Live-Befehle an Velocity senden
* Logs und Spieler-Events einsehen
* Verbindungsstatus prüfen

---

## 📞 Support

Eröffne ein Issue auf GitHub oder kontaktiere das Entwicklerteam von EchoCloud.



