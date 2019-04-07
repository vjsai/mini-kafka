import com.vjsai.mini.mina.api.IoHandler;
import com.vjsai.mini.mina.nio.NioSocketConnector;

import java.net.InetSocketAddress;

/**
 * DemoClientApplication to illustrate how to connect to server
 */
public class DemoClientApplication {

    public static void main(String[] args) {
        try {
            ;
            /**
             * initialize NioSocketConnector instance
             */
            NioSocketConnector connector = new NioSocketConnector();
            /**
             *set session buffer size(in bytes)
             */
            connector.setSessionBufferSize(2048);
            connector.setMaxSessionIdleTimeoutinMS(30000 * 1000);
            /**
             * set callback handler
             */
            IoHandler handler = new ClientHandler();
            connector.setHandler(handler);
            /**
             * connect to the TCP Server
             */
            connector.connect(new InetSocketAddress("127.0.0.1", 5000));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}