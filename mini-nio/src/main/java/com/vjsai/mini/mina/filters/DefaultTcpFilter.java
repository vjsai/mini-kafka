package com.vjsai.mini.mina.filters;

import com.vjsai.mini.mina.constants.SessionStateConstants;
import com.vjsai.mini.mina.exceptions.NioBaseWriteException;
import com.vjsai.mini.mina.session.SocketSessionState;
import com.vjsai.mini.mina.utils.ByteDecoder;
import com.vjsai.mini.mina.utils.ByteEncoder;
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
                //Set the position in the buffer
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
        //decode basing on ByteOrder
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

    @Override
    public void writeFilter(SocketSessionState session, byte[] writeBytes) throws NioBaseWriteException, IOException {
        //check for null
        if (writeBytes == null) {
            throw new NioBaseWriteException("writeBytes cannot be NULL");
        }
        //check if writeBytes exceed session
        if (writeBytes.length > session.getBufferSize() - BOUND_BYTES_NUMBER) {
            throw new NioBaseWriteException("writeBytes length exceeded session size");
        }

        int writeBytesLength = writeBytes.length;
        byte[] lengthBytes = new byte[BOUND_BYTES_NUMBER];
        ByteEncoder.encodeInt(lengthBytes, 0, ByteOrder.Little_Endian, writeBytesLength);

        byte[] protocol = new byte[writeBytesLength + BOUND_BYTES_NUMBER];

        // length bytes
        protocol[0] = lengthBytes[0];
        protocol[1] = lengthBytes[1];
        protocol[2] = lengthBytes[2];
        protocol[3] = lengthBytes[3];

        System.arraycopy(writeBytes, 0, protocol, BOUND_BYTES_NUMBER, writeBytesLength);

        ByteBuffer dstByteBuffer = ByteBuffer.allocate(writeBytesLength + BOUND_BYTES_NUMBER);
        dstByteBuffer.put(protocol);
        dstByteBuffer.flip();
        int writeLength = 0;

        long activeTimeStamp = System.currentTimeMillis();

        try {
            //while the buffer is still left keep on writing to socket
            while (dstByteBuffer.hasRemaining()) {
                writeLength += session.getSocketChannel().write(dstByteBuffer);
            }
            activeTimeStamp = System.currentTimeMillis();
        } catch (IOException e) {
            session.setTimeoutFlag();
            logger.warn("write bytes: " + session.toString(), e);
            throw e;
        }

        if (writeLength > 0) {
            session.setLastActiveTimeStamp(activeTimeStamp);
        }
    }

    /**
     * Method to get length of the nextByte thats coming in
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
