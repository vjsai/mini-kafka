package com.vjsai.mini.mina.utils;

public class ByteEncoder {
    public static int encodeInt(byte[] goal, int nIndex, ByteOrder byteOrder,
                                int datavalue) {
        if (byteOrder == ByteOrder.Little_Endian) {
            goal[nIndex] = (byte) (datavalue & 0xff);
            goal[nIndex + 1] = (byte) (datavalue >> 8 & 0xff);
            goal[nIndex + 2] = (byte) (datavalue >> 16 & 0xff);
            goal[nIndex + 3] = (byte) (datavalue >> 24 & 0xff);
        } else {
            //Big_endian
            goal[nIndex + 3] = (byte) (datavalue & 0xff);
            goal[nIndex + 2] = (byte) (datavalue >> 8 & 0xff);
            goal[nIndex + 1] = (byte) (datavalue >> 16 & 0xff);
            goal[nIndex] = (byte) (datavalue >> 24 & 0xff);
        }
        return 4;
    }
}
