package org.sqlite.date;

import static org.sqlite.date.FastDateParser.escapeRegex;

/** A strategy that copies the static or quoted field in the parsing pattern */
public class CopyQuotedDateParsingStrategy extends DateParsingStrategy {
    private final String formatField;

    /**
     * Construct a Strategy that ensures the formatField has literal text
     *
     * @param formatField The literal text to match
     */
    CopyQuotedDateParsingStrategy(final String formatField) {
        this.formatField = formatField;
    }

    /** {@inheritDoc} */
    @Override
    boolean isNumber() {
        char c = formatField.charAt(0);
        if (c == '\'') {
            c = formatField.charAt(1);
        }
        return Character.isDigit(c);
    }

    /** {@inheritDoc} */
    @Override
    boolean addRegex(final FastDateParser parser, final StringBuilder regex) {
        escapeRegex(regex, formatField, true);
        return false;
    }
}
