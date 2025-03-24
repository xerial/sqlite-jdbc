package org.sqlite.date;

import java.text.DateFormatSymbols;
import java.util.*;

import static org.sqlite.date.FastDateParser.escapeRegex;

/** A strategy that handles a timezone field in the parsing pattern */
public class TimeZoneDateParsingStrategy extends DateParsingStrategy {

    private final String validTimeZoneChars;
    private final SortedMap<String, TimeZone> tzNames =
            new TreeMap<String, TimeZone>(String.CASE_INSENSITIVE_ORDER);

    /** Index of zone id */
    private static final int ID = 0;
    /** Index of the long name of zone in standard time */
    private static final int LONG_STD = 1;
    /** Index of the short name of zone in standard time */
    private static final int SHORT_STD = 2;
    /** Index of the long name of zone in daylight saving time */
    private static final int LONG_DST = 3;
    /** Index of the short name of zone in daylight saving time */
    private static final int SHORT_DST = 4;

    /**
     * Construct a Strategy that parses a TimeZone
     *
     * @param locale The Locale
     */
    TimeZoneDateParsingStrategy(final Locale locale) {
        final String[][] zones = DateFormatSymbols.getInstance(locale).getZoneStrings();
        for (final String[] zone : zones) {
            if (zone[ID].startsWith("GMT")) {
                continue;
            }
            final TimeZone tz = TimeZone.getTimeZone(zone[ID]);
            if (!tzNames.containsKey(zone[LONG_STD])) {
                tzNames.put(zone[LONG_STD], tz);
            }
            if (!tzNames.containsKey(zone[SHORT_STD])) {
                tzNames.put(zone[SHORT_STD], tz);
            }
            if (tz.useDaylightTime()) {
                if (!tzNames.containsKey(zone[LONG_DST])) {
                    tzNames.put(zone[LONG_DST], tz);
                }
                if (!tzNames.containsKey(zone[SHORT_DST])) {
                    tzNames.put(zone[SHORT_DST], tz);
                }
            }
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("(GMT[+-]\\d{1,2}:\\d{2}").append('|');
        sb.append("[+-]\\d{4}").append('|');
        for (final String id : tzNames.keySet()) {
            escapeRegex(sb, id, false).append('|');
        }
        sb.setCharAt(sb.length() - 1, ')');
        validTimeZoneChars = sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    boolean addRegex(final FastDateParser parser, final StringBuilder regex) {
        regex.append(validTimeZoneChars);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    void setCalendar(final FastDateParser parser, final Calendar cal, final String value) {
        TimeZone tz;
        if (value.charAt(0) == '+' || value.charAt(0) == '-') {
            tz = TimeZone.getTimeZone("GMT" + value);
        } else if (value.startsWith("GMT")) {
            tz = TimeZone.getTimeZone(value);
        } else {
            tz = tzNames.get(value);
            if (tz == null) {
                throw new IllegalArgumentException(value + " is not a supported timezone name");
            }
        }
        cal.setTimeZone(tz);
    }
}
