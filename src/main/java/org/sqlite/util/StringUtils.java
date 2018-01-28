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

    /**
     * @param needle
     * @param haystack must NOT be <code>null</code>.
     * @return
     * @author Andy-2639
     */
    public static boolean inArray(char needle, char... haystack) {
        for (int i = 0; i < haystack.length; i++) {
            if (needle == haystack[i]) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param s must NOT be <code>null<code>.
     * @param esc
     * @param specials must NOT be <code>null</code>.
     * @return never <code>null</code>.
     * @author Andy-2639
     */
    public static String escape(String s, char esc, char... specials) {
        StringBuilder sb = new StringBuilder(2 * s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch == esc) || (StringUtils.inArray(ch, specials))) {
                sb.append(esc);
            }
            sb.append(ch);
        }
        if (s.length() == sb.length()) {
            return s;
        } else {
            return sb.toString();
        }
    }
}
