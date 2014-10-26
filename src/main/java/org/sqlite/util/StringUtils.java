package org.sqlite.util;

import java.util.List;

public class StringUtils {
    public static String join(List<String> list, String separator) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(String item : list) {
            if (first)
                first = false;
            else
                sb.append(separator);

            sb.append(item);
        }
        return sb.toString();
    }
}
