package aromachat.server;

import com.google.gson.Gson;
import org.glassfish.tyrus.server.Server;

import java.io.FileReader;
import java.nio.charset.Charset;

public class Main {
    public static void main(String[] args) throws Exception {
        FileReader reader = new FileReader("aroma.json", Charset.defaultCharset());
        AromaServer.setConfiguration(new Gson().fromJson(reader, AromaConfiguration.class));
        AromaServer.getInstance(); // Make sure that the singleton instance is created

        Server server = new Server(AromaServer.getConfiguration().getHost(),
                                   AromaProtocol.DEFAULT_PORT,
                                   "",
                                   null,
                                   AromaConnection.class);

        server.start();
        while (true);
    }
}