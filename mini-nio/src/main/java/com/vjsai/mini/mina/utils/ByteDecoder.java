package com.vjsai.mini.mina.utils;

public class ByteDecoder {
    public static int decodeInt(final byte[] source, int nIndex, ByteOrder byteOrder) {
        int result = 0;

        if (byteOrder == ByteOrder.Little_Endian) {
            result = (((source[nIndex + 3] & 0xff) << 24)
                    | ((source[nIndex + 2] & 0xff) << 16)
                    | ((source[nIndex + 1] & 0xff) << 8) | (source[nIndex] & 0xff));
        } else {
            result = (int) (((source[nIndex] & 0xff) << 24)
                    | ((source[nIndex + 1] & 0xff) << 16)
                    | ((source[nIndex + 2] & 0xff) << 8) | (source[nIndex + 3] & 0xff));
        }
        return result;
    }
}
