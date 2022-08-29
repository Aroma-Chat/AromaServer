package aromachat.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A text channel
 * @author Alessandro-Salerno
 */
public final class AromaTextChannel {
    /**
     * The channel's name
     */
    private final String name;
    /**
     * All message sent through the channel
     */
    private final List<Object> messages;
    /**
     * All users on the channel
     */
    private final Map<String, AromaConnection> users;

    public AromaTextChannel(String name) {
        this.name = name;
        this.messages = new ArrayList<>();
        this.users = new HashMap<>();
    }

    /**
     * Add a user to the channel
     * @param user the user
     */
    public void join(AromaConnection user) {
        // Broadcast notice to all already connected users
        AromaProtocol.Packet.Response.UserJoin userJoin = new AromaProtocol.Packet.Response.UserJoin();
        userJoin.setName(user.getUsername());
        this.silentBroadcast(userJoin);

        // Add the user
        users.put(user.getUsername(), user);
    }

    /**
     * Remove a user from the channel
     * @param user the user
     */
    public void leave(AromaConnection user) {
        // Add the user
        if (!users.containsKey(user.getUsername())) return;
        users.remove(user.getUsername());

        // Broadcast the notice to all remaining users
        AromaProtocol.Packet.Response.UserLeave userLeave = new AromaProtocol.Packet.Response.UserLeave();
        userLeave.setName(user.getUsername());
        this.silentBroadcast(userLeave);
    }

    /**
     * Broadcast a message to all users and save it
     * @param object the message
     */
    public void broadcast(Object object) {
        // Broadcast the message
        this.silentBroadcast(object);

        // Save the message
        this.messages.add(object);
    }

    /**
     * Broadcast a message to all users
     * @param object the message
     */
    private void silentBroadcast(Object object) {
        synchronized (this.users) {
            this.users.values().stream()
                    .parallel()
                    .forEach(user -> user.send(object));
        }
    }

    public String getName() {
        return this.name;
    }

    public List<Object> getMessages() {
        return this.messages;
    }
}
