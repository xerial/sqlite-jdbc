package org.sqlite.date;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.sqlite.date.FastDateParser.escapeRegex;
import static org.sqlite.date.FastDateParser.getDisplayNames;

/** A strategy that handles a text field in the parsing pattern */
public class CaseInsensitiveTextStrategy extends DateParsingStrategy {
    private final int field;
    private final Locale locale;
    private final Map<String, Integer> lKeyValues;

    /**
     * Construct a Strategy that parses a Text field
     *
     * @param field The Calendar field
     * @param definingCalendar The Calendar to use
     * @param locale The Locale to use
     */
    CaseInsensitiveTextStrategy(
            final int field, final Calendar definingCalendar, final Locale locale) {
        this.field = field;
        this.locale = locale;
        final Map<String, Integer> keyValues = getDisplayNames(field, definingCalendar, locale);
        this.lKeyValues = new HashMap<String, Integer>();

        for (final Map.Entry<String, Integer> entry : keyValues.entrySet()) {
            lKeyValues.put(entry.getKey().toLowerCase(locale), entry.getValue());
        }
    }

    /** {@inheritDoc} */
    @Override
    boolean addRegex(final FastDateParser parser, final StringBuilder regex) {
        regex.append("((?iu)");
        for (final String textKeyValue : lKeyValues.keySet()) {
            escapeRegex(regex, textKeyValue, false).append('|');
        }
        regex.setCharAt(regex.length() - 1, ')');
        return true;
    }

    /** {@inheritDoc} */
    @Override
    void setCalendar(final FastDateParser parser, final Calendar cal, final String value) {
        final Integer iVal = lKeyValues.get(value.toLowerCase(locale));
        if (iVal == null) {
            final StringBuilder sb = new StringBuilder(value);
            sb.append(" not in (");
            for (final String textKeyValue : lKeyValues.keySet()) {
                sb.append(textKeyValue).append(' ');
            }
            sb.setCharAt(sb.length() - 1, ')');
            throw new IllegalArgumentException(sb.toString());
        }
        cal.set(field, iVal.intValue());
    }
}
