package com.vjsai.mini.mina.filters;

import com.vjsai.mini.mina.constants.SessionStateConstants;
import com.vjsai.mini.mina.session.SocketSessionState;
import com.vjsai.mini.mina.utils.ByteDecoder;
import com.vjsai.mini.mina.utils.ByteOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Tcp byte stream filter
 */
public class DefaultTcpFilter extends BaseFilterChain implements SessionStateConstants {
    private final static Logger logger = LoggerFactory.getLogger(DefaultTcpFilter.class);

    @Override
    public byte[] filterReceive(SocketSessionState session, byte[] filterBytes) throws IOException {

        SocketChannel channel = session.getSocketChannel();
        byte[] readBuffer = session.getReadBuffer();
        int bufferSize = session.getBufferSize();

        boolean exceptionProtocol = false;
        boolean isRead = false;
        long activeTimeStamp = System.currentTimeMillis();

        try {
            for(int i =0; i < 2; i++) {
                int bufferPosition = session.getBufferPosition();
                int nextReadByteLength = this.nextReadByteLength(readBuffer, bufferPosition);
                //if we dont have any bytes to read breakout
                if (nextReadByteLength == 0) {
                    break;
                } else if (nextReadByteLength < 0 || nextReadByteLength > bufferSize - BOUND_BYTES_NUMBER) {
                    session.resetBuffer();
                    exceptionProtocol = true;
                    break;
                }

                ByteBuffer dstByteBuffer = ByteBuffer.allocate(nextReadByteLength);

                int readByteLength = channel.read(dstByteBuffer);
                //if read bytes exist change to read mode
                if (readByteLength > 0) {
                    isRead = true;
                    activeTimeStamp = System.currentTimeMillis();
                } else if (readByteLength == 0) {
                    break;
                } else {
                    throw new IOException();
                }
                //now flip the buffer mode
                dstByteBuffer.flip();
                byte[] byteDst = new byte[readByteLength];
                dstByteBuffer.get(byteDst);
                System.arraycopy(byteDst, 0, readBuffer, bufferPosition, readByteLength);
                bufferPosition += readByteLength;
                session.setBufferPosition(bufferPosition);

            }
        } catch (IOException e) {
            logger.warn("receive bytes: " + session.toString(), e);
            throw e;
        } finally {
            if (isRead) {
                session.setLastActiveTimeStamp(activeTimeStamp);
            }
        }

        byte[] fullProtocol = null;

        if (exceptionProtocol) {
            return fullProtocol;
        }

        int curProtocolLen = ByteDecoder.decodeInt(readBuffer, 0, ByteOrder.Little_Endian);
        if (curProtocolLen > 0 && (curProtocolLen == session.getBufferPosition() - BOUND_BYTES_NUMBER)) {
            fullProtocol = new byte[curProtocolLen];
            System.arraycopy(readBuffer, BOUND_BYTES_NUMBER, fullProtocol, 0, curProtocolLen);
            session.resetBuffer();
        } else if (curProtocolLen > 0 && (curProtocolLen < session.getBufferPosition() - BOUND_BYTES_NUMBER)) {
            session.resetBuffer();
        } else if (curProtocolLen <= 0) {
            session.resetBuffer();
        }
        return fullProtocol;
    }

    /**
     * @param protocolBuffer
     * @param pos
     * @return
     */
    private int nextReadByteLength(byte[] protocolBuffer, int pos) {
        if (pos == 0) {
            return 4;
        } else if (pos == 1) {
            return 3;
        } else if (pos == 2) {
            return 2;
        } else if (pos == 3) {
            return 1;
        } else if (pos >= BOUND_BYTES_NUMBER) {
            int protocolLen = ByteDecoder.decodeInt(protocolBuffer, 0, ByteOrder.Little_Endian);
            return protocolLen - (pos - BOUND_BYTES_NUMBER);
        } else {
            return 0;
        }
    }

}
