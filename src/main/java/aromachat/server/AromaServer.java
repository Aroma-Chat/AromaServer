package aromachat.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * An Aroma Server
 * @apiNote singleton class
 * @author Alessandro-Salerno
 */
public final class AromaServer {
    /**
     * The class'es instance
     */
    private static AromaServer instance;
    /**
     * The server configuration
     */
    private static AromaConfiguration config;

    /**
     * All available text channels
     */
    private final Map<String, AromaTextChannel> textChannels;
    /**
     * All connected users
     */
    private final Map<String, AromaConnection> connections;

    private AromaServer() {
        if (AromaServer.getConfiguration() == null)
            throw new RuntimeException(new NullPointerException("Configuration must be loaded before instantiating new server."));

        // Allocate
        this.textChannels = new HashMap<>();
        this.connections = new HashMap<>();

        // Update text channels
        AromaServer.getConfiguration()
                    .getTextChannelNames()
                     .stream()
                     .parallel()
                     .forEach(channel -> this.textChannels.put(channel,
                                                               new AromaTextChannel(channel)));
    }

    /**
     * @apiNote singleton method
     * @return the singleton instance
     */
    public static AromaServer getInstance() {
        if (instance == null)
            instance = new AromaServer();

        return instance;
    }

    public static void setConfiguration(AromaConfiguration configuration) {
        if (instance == null)
            AromaServer.config = configuration;
    }

    public static AromaConfiguration getConfiguration() {
        return AromaServer.config;
    }

    /**
     * Broadcast a message to all users
     * @param object the message
     */
    public void broadcast(Object object) {
        synchronized (this.connections) {
            this.connections.values().stream()
                                      .parallel()
                                      .forEach(connection -> connection.send(object));
        }
    }

    /**
     * Register a new user
     * @param connection the user
     */
    public void register(AromaConnection connection) {
        synchronized (this.connections) {
            this.connections.put(connection.getUsername(), connection);
            connection.setJoined(true);
        }
    }

    /**
     * Disconnect an existing user
     * @param connection the user
     */
    public void disconnect(AromaConnection connection) {
        synchronized (this.connections) {
            this.connections.remove(connection.getUsername());
            connection.setJoined(false);
        }
    }

    public Map<String, AromaTextChannel> getTextChannels() {
        return this.textChannels;
    }

    public Map<String, AromaConnection> getConnections() {
        return this.connections;
    }
}
