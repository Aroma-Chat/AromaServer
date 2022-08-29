package aromachat.server;

import aromachat.server.util.ObjectUtility;
import com.google.gson.JsonObject;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * Handles a client's connection
 * @author Alessandro-Salerno
 */
@ServerEndpoint(value = AromaProtocol.PATH,
                encoders = AromaProtocol.Encoder.class,
                decoders = AromaProtocol.Decoder.class)
public class AromaConnection {
    /**
     * The WebSocket Session
     */
    private Session wsSession;
    /**
     * The connected client's username
     */
    private String username;
    /**
     * The text channel the client is connected to
     */
    private AromaTextChannel textChannel;
    /**
     * If the user has joined or not
     */
    private boolean joined = false;

    /**
     * What happens when the connection opens
     * @param session the session
     * @throws IOException if something goes wrong while closing the connection in the event of an error
     */
    @OnOpen
    public void onOpen(Session session)
            throws IOException {

        // Check that the connection has been established correctly
        if (!(session.getRequestParameterMap().containsKey(AromaProtocol.Parameter.USERNAME)
              && session.getRequestParameterMap().containsKey(AromaProtocol.Parameter.PROTOCOL_VERSION))) {
            session.close(new CloseReason(CloseReason.CloseCodes.PROTOCOL_ERROR,
                                          "Missing parameters in connection request"));
            return;
        }

        // Check the client's Aroma Protocol version
        if (!session.getRequestParameterMap()
                     .get(AromaProtocol.Parameter.PROTOCOL_VERSION).get(0)
                     .equals(AromaProtocol.VERSION)) {
            session.close(new CloseReason(CloseReason.CloseCodes.PROTOCOL_ERROR,
                                          "Incompatible Aroma Protocol version "
                                                      + session.getRequestParameterMap()
                                                                .get(AromaProtocol.Parameter.PROTOCOL_VERSION).get(0)
                                                      + ". This server requires version "
                                                      + AromaProtocol.VERSION));
            return;
        }

        // Save values into fields
        this.wsSession = session;
        this.username = session.getRequestParameterMap()
                                .get(AromaProtocol.Parameter.USERNAME).get(0);

        // Check that the username is not already in use by another client
        if (AromaServer.getInstance().getConnections().containsKey(this.username)) {
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT,
                                          "Username \"" + this.username + "\" is already in use."));
            return;
        }

        // Broadcast login notification
        AromaProtocol.Packet.Response.UserLogin userLogin = new AromaProtocol.Packet.Response.UserLogin();
        userLogin.setName(this.username);
        AromaServer.getInstance().broadcast(userLogin);

        // Register new connection with the server
        AromaServer.getInstance().register(this);

        // Send login information to the client
        AromaProtocol.Packet.Response.Login login = new AromaProtocol.Packet.Response.Login();
        login.setServerName(AromaServer.getConfiguration().getServerName());
        login.setTextChannels(AromaServer.getConfiguration().getTextChannelNames());
        this.send(login);

        // Console output
        System.out.println(this.username + " logged in");
    }

    /**
     * What happens when a message is received
     * @param session the session
     * @param message the received message
     */
    @OnMessage
    public void onMessage(Session session, JsonObject message) {
        // Forward the message to the right event listener
        new ObjectUtility(this).forEachMethodWithAnnotation(AromaEvent.class,
                                                               method -> {
                                                                    // Forward the message to the right handler
                                                                    if (method.getAnnotation(AromaEvent.class)
                                                                               .value()
                                                                                .equals(message.get(AromaProtocol.Packet.TYPE)
                                                                                                .getAsString())) {
                                                                        try { method.invoke(this, message); }
                                                                        catch (IllegalAccessException |
                                                                                 InvocationTargetException e) {
                                                                            throw new RuntimeException(e);
                                                                        }
                                                                    }
                                                               });
    }

    /**
     * What happens when the connection is closed
     * @param session the session
     */
    @OnClose
    public void onClose(Session session) {
        // If the client has not joined yet, then there's no reason to continue
        if (!this.joined) return;

        // Disconnect the client from the server
        AromaServer.getInstance().disconnect(this);

        // Disconnect the client from the text channel
        if (this.textChannel != null) {
            this.textChannel.leave(this);
        }

        // Broadcast logout notification
        AromaProtocol.Packet.Response.UserLogout userLogout = new AromaProtocol.Packet.Response.UserLogout();
        userLogout.setName(this.username);
        AromaServer.getInstance().broadcast(userLogout);

        System.out.println(this.username + " logged out");
    }

    /**
     * Event listener for a user message
     * @param jsonObject the message
     */
    @AromaEvent(AromaProtocol.Event.USER_MESSAGE)
    public void onUserMessage(JsonObject jsonObject) {
        // If the user has yet to join a text channel, then the message is discarded
        if (this.textChannel == null) return;

        // Broadcast the message to all clients connected to the channel
        AromaProtocol.Packet.Response.UserMessage msg = new AromaProtocol.Packet.Response.UserMessage();
        msg.setSender(this.username);
        msg.setContent(jsonObject.get(AromaProtocol.Packet.Response.UserMessage.CONTENT).getAsString());
        this.textChannel.broadcast(msg);
    }

    /**
     * What happens when a user asks to join a channel
     * @param jsonObject the request
     */
    @AromaEvent(AromaProtocol.Event.JOIN)
    public void onJoin(JsonObject jsonObject) {
        // Get the channel's name from the request
        String channelName = jsonObject.get(AromaProtocol.Packet.Request.Join.CHANNEL).getAsString();

        if (this.textChannel != null) {
            // Leave the current channel
            this.textChannel.leave(this);
        }

        // Join the requested channel
        this.textChannel = AromaServer.getInstance().getTextChannels().get(channelName);
        this.textChannel.join(this);

        // Send confirmation reply
        AromaProtocol.Packet.Response.Join join = new AromaProtocol.Packet.Response.Join();
        join.setName(channelName);
        join.setMessages(this.textChannel.getMessages());
        this.send(join);

        System.out.println(this.username + " joined " + channelName);
    }

    /**
     * What happens when a user asks to leave a channel
     * @param jsonObject the request
     */
    @AromaEvent(AromaProtocol.Event.LEAVE)
    public void onLeave(JsonObject jsonObject) {
        // If the user is not connected to a text channel, then it makes not sense to leave
        if (this.textChannel == null) return;

        // Send confirmation reply
        AromaProtocol.Packet.Response.Leave leave = new AromaProtocol.Packet.Response.Leave();
        leave.setName(this.textChannel.getName());
        this.send(leave);

        // Leave channel
        String channelName = this.textChannel.getName();
        this.textChannel.leave(this);
        this.textChannel = null;

        System.out.println(this.username + " left " + channelName);
    }

    /**
     * Send an object
     * @param object the object
     */
    public void send(Object object) {
        try { this.wsSession.getBasicRemote().sendObject(object); }
        catch (IOException |
                EncodeException e) {
            throw new RuntimeException(e);
        }
    }

    public Session getSession() {
        return this.wsSession;
    }

    public String getUsername() {
        return this.username;
    }

    public boolean isJoined() {
        return this.joined;
    }

    public void setJoined(boolean j) {
        this.joined = j;
    }
}
