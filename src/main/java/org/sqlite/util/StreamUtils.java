package org.sqlite.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class StreamUtils {
    private static final int DEFAULT_BUF_SIZE = 0x800;

    public static String toString(InputStream in, Charset charset) throws IOException {
        if (in == null) {
            throw new IllegalArgumentException("input stream cannot be null");
        }
        if (charset == null) {
            throw new IllegalArgumentException("charset cannot be null");
        }

        InputStreamReader reader = new InputStreamReader(in);
        StringBuilder stringBuilder = new StringBuilder();

        char[] buf = new char[DEFAULT_BUF_SIZE];
        int nRead;
        while ((nRead = reader.read(buf)) != -1) {
            stringBuilder.append(buf, 0, nRead);
        }

        return stringBuilder.toString();
    }
}
