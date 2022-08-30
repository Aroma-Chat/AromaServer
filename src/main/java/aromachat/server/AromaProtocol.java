package aromachat.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.websocket.DecodeException;
import javax.websocket.EncodeException;
import javax.websocket.EndpointConfig;
import java.util.List;

@SuppressWarnings("unused")
public final class AromaProtocol {
    public static final String VERSION = "0.0.4";
    public static final String PATH = "/aromachat/chat";
    public static final int DEFAULT_PORT = 80;

    public static class Event {
        public static final String LOGIN = "login";
        public static final String LOGOUT = "logout";
        public static final String JOIN = "join";
        public static final String LEAVE = "leave";
        public static final String USER_MESSAGE = "usermessage";
        public static final String USER_LOGIN = "userlogin";
        public static final String USER_LOGOUT = "userlogout";
        public static final String USER_JOIN = "userjoin";
        public static final String USER_LEAVE = "userleave";
    }

    public static abstract class Packet {
        public static final String TYPE = "type";

        private String type;

        public final String getType() {
            return this.type;
        }

        public final void setType(String t) {
            this.type = t;
        }

        public static class Response {
            public static final class Login extends Packet {
                private String serverName;
                private List<String> channels;

                public Login() {
                    this.setType(AromaProtocol.Event.LOGIN);
                }

                public void setServerName(String name) {
                    this.serverName = name;
                }

                public void setTextChannels(List<String> channelNames) {
                    this.channels = channelNames;
                }
            }

            public static class UserMessage extends Packet {
                public static final String SENDER = "sender";
                public static final String CONTENT = "content";
                private String sender;
                private String content;

                public UserMessage() {
                    this.setType(AromaProtocol.Event.USER_MESSAGE);
                }

                public void setSender(String s) {
                    sender = s;
                }

                public void setContent(String c) {
                    content = c;
                }
            }

            public static class UserLogin extends Packet {
                public static final String NAME = "name";

                private String name;

                public UserLogin() {
                    this.setType(Event.USER_LOGIN);
                }

                public void setName(String n) {
                    name = n;
                }
            }

            public static class UserLogout extends UserLogin {
                public UserLogout() {
                    this.setType(AromaProtocol.Event.USER_LOGOUT);
                }
            }

            public static class Join extends Packet {
                private String name;
                private List<?> messages;

                public Join() {
                    this.setType(AromaProtocol.Event.JOIN);
                }

                public void setName(String n) {
                    this.name = n;
                }

                public void setMessages(List<?> m) {
                    this.messages = m;
                }
            }

            public static class Leave extends Packet {
                private String name;

                public Leave() {
                    this.setType(AromaProtocol.Event.LEAVE);
                }

                public void setName(String n) {
                    this.name = n;
                }
            }

            public static class UserJoin extends Packet {
                private String name;

                public UserJoin() {
                    this.setType(AromaProtocol.Event.USER_JOIN);
                }

                public void setName(String n) {
                    this.name = n;
                }
            }

            public static class UserLeave extends UserJoin {
                public UserLeave() {
                    this.setType(AromaProtocol.Event.USER_LEAVE);
                }
            }
        }

        public static class Request {
            public static class Join {
                public static final String CHANNEL = "channel";
            }
        }
    }

    public static class Parameter {
        public static final String USERNAME = "username";
        public static final String PROTOCOL_VERSION = "protocol";
    }

    public static class Encoder implements javax.websocket.Encoder.Text<Object> {
        @Override
        public String encode(Object object) throws EncodeException {
            return new Gson().toJson(object);
        }

        @Override
        public void init(EndpointConfig endpointConfig) { }
        @Override
        public void destroy() { }
    }

    public static class Decoder implements javax.websocket.Decoder.Text<JsonObject> {

        @Override
        public JsonObject decode(String s) throws DecodeException {
            return new Gson().fromJson(s, JsonObject.class);
        }

        @Override
        public boolean willDecode(String s) {
            return s != null;
        }

        @Override
        public void init(EndpointConfig endpointConfig) { }
        @Override
        public void destroy() { }
    }
}
