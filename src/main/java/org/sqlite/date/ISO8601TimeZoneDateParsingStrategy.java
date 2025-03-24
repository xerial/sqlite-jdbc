package org.sqlite.date;

import java.util.Calendar;
import java.util.TimeZone;

public class ISO8601TimeZoneDateParsingStrategy extends DateParsingStrategy {
    // Z, +hh, -hh, +hhmm, -hhmm, +hh:mm or -hh:mm
    private final String pattern;

    /**
     * Construct a Strategy that parses a TimeZone
     *
     * @param pattern The Pattern
     */
    ISO8601TimeZoneDateParsingStrategy(String pattern) {
        this.pattern = pattern;
    }

    /** {@inheritDoc} */
    @Override
    boolean addRegex(FastDateParser parser, StringBuilder regex) {
        regex.append(pattern);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    void setCalendar(FastDateParser parser, Calendar cal, String value) {
        if (value.equals("Z")) {
            cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        } else {
            cal.setTimeZone(TimeZone.getTimeZone("GMT" + value));
        }
    }

    private static final DateParsingStrategy ISO_86011_DATE_PARSING_STRATEGY =
            new ISO8601TimeZoneDateParsingStrategy("(Z|(?:[+-]\\d{2}))");
    private static final DateParsingStrategy ISO_86012_DATE_PARSING_STRATEGY =
            new ISO8601TimeZoneDateParsingStrategy("(Z|(?:[+-]\\d{2}\\d{2}))");
    private static final DateParsingStrategy ISO_86013_DATE_PARSING_STRATEGY =
            new ISO8601TimeZoneDateParsingStrategy("(Z|(?:[+-]\\d{2}(?::)\\d{2}))");

    /**
     * Factory method for ISO8601TimeZoneStrategies.
     *
     * @param tokenLen a token indicating the length of the TimeZone String to be formatted.
     * @return a ISO8601TimeZoneStrategy that can format TimeZone String of length {@code
     *     tokenLen}. If no such strategy exists, an IllegalArgumentException will be thrown.
     */
    static DateParsingStrategy getStrategy(int tokenLen) {
        switch (tokenLen) {
            case 1:
                return ISO_86011_DATE_PARSING_STRATEGY;
            case 2:
                return ISO_86012_DATE_PARSING_STRATEGY;
            case 3:
                return ISO_86013_DATE_PARSING_STRATEGY;
            default:
                throw new IllegalArgumentException("invalid number of X");
        }
    }
}
