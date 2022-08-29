package aromachat.server;

import java.util.ArrayList;
import java.util.List;

/**
 * The configuration for an Aroma Server
 * @author Alessandro-Salerno
 */
public final class AromaConfiguration {
    /**
     * The address on which the server is hosted
     */
    private String host;
    /**
     * The server's name
     */
    private String serverName;
    /**
     * The list of text channels available to users
     */
    private final List<String> textChannels = new ArrayList<>();

    public String getHost() {
        return this.host;
    }

    public String getServerName() {
        return this.serverName;
    }

    public List<String> getTextChannelNames() {
        return this.textChannels;
    }
}
