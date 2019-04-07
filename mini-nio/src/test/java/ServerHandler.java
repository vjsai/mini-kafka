import com.vjsai.mini.mina.api.IoHandler;
import com.vjsai.mini.mina.api.IoSession;
import com.vjsai.mini.mina.exceptions.NioBaseWriteException;

import java.io.IOException;

public class ServerHandler implements IoHandler {
	public void onSocketOpen(IoSession session) {
		System.out.println(" Server :  opened new socket " + session.toString());
		byte[] msg = "hello client how are you".getBytes();
		try {
			session.write(msg);
		} catch (NioBaseWriteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void onSocketClose(IoSession session) {
		System.out.println( "Server: closed follwoing socket " + session.toString());
	}

	public void onMessage(IoSession session, byte[] message) {
		String msg = new String(message);
		System.out.println("Server received : " + session + "  " + msg);
	}
}