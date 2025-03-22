package org.sqlite.date;

import java.util.Calendar;

/** A strategy that handles a number field in the parsing pattern */
public class NumberDateParsingStrategy extends DateParsingStrategy {
    private final int field;

    /**
     * Construct a Strategy that parses a Number field
     *
     * @param field The Calendar field
     */
    NumberDateParsingStrategy(final int field) {
        this.field = field;
    }

    /** {@inheritDoc} */
    @Override
    boolean isNumber() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    boolean addRegex(final FastDateParser parser, final StringBuilder regex) {
        // See LANG-954: We use {Nd} rather than {IsNd} because Android does not support the Is
        // prefix
        if (parser.isNextNumber()) {
            regex.append("(\\p{Nd}{").append(parser.getFieldWidth()).append("}+)");
        } else {
            regex.append("(\\p{Nd}++)");
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    void setCalendar(final FastDateParser parser, final Calendar cal, final String value) {
        cal.set(field, modify(Integer.parseInt(value)));
    }

    /**
     * Make any modifications to parsed integer
     *
     * @param iValue The parsed integer
     * @return The modified value
     */
    int modify(final int iValue) {
        return iValue;
    }
}
