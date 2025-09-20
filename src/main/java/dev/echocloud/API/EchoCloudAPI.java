package dev.echocloud.API;

import dev.echocloud.Cloud.CloudCommunication;

/**
 * EchoCloud API Interface für externe Plugins
 * Stellt Zugriff auf die CloudCommunication-Instanz bereit
 */
public interface EchoCloudAPI {

    /**
     * Gibt die CloudCommunication-Instanz zurück
     * @return CloudCommunication-Instanz oder null wenn nicht verfügbar
     */
    CloudCommunication getCommunication();

    /**
     * Prüft ob die Cloud-Verbindung aktiv ist
     * @return true wenn verbunden, false sonst
     */
    boolean isConnected();

    /**
     * Gibt die Server-ID zurück
     * @return Server-ID oder "UNKNOWN" wenn nicht konfiguriert
     */
    String getServerId();
}