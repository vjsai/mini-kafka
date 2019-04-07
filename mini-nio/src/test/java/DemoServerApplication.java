import com.vjsai.mini.mina.nio.NioSocketServerAcceptor;

import java.net.InetSocketAddress;

/**
 * demo server application to illustrate how to create server with this mini-nio module
 */
public class DemoServerApplication {

	public static void main(String[] args) {

		try {
			NioSocketServerAcceptor nioSocketServerAccepter = new NioSocketServerAcceptor();
			nioSocketServerAccepter.setHandler(new ServerHandler());
			nioSocketServerAccepter.setSessionBufferSize(1024);
			nioSocketServerAccepter.setMaxSessionIdleTimeoutInMS(30000 * 1000);
 			nioSocketServerAccepter.bind(new InetSocketAddress("0.0.0.0", 5000));
			Thread.sleep(1000000);
			nioSocketServerAccepter.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}