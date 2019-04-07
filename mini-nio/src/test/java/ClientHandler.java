import com.vjsai.mini.mina.api.IoHandler;
import com.vjsai.mini.mina.api.IoSession;
import com.vjsai.mini.mina.exceptions.NioBaseWriteException;

import java.io.IOException;

public class ClientHandler implements IoHandler {

    public void onSocketOpen(IoSession session) {
        System.out.println(session.toString() + " : TCP client session created");
        byte[] msg = "hello world from client".getBytes();
        try {
            session.write(msg);
        } catch (NioBaseWriteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onSocketClose(IoSession session) {
        System.out.println(session.toString() + "TCP client connection closed");
    }

    public void onMessage(IoSession session, byte[] message) {
        System.out.println("client received following data : " + session + "  " + new String(message));
    }
}
