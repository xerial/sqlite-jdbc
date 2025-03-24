package org.sqlite.date;

import java.util.Calendar;

/** A strategy to parse a single field from the parsing pattern */
public abstract class DateParsingStrategy {

    /**
     * Is this field a number? The default implementation returns false.
     *
     * @return true, if field is a number
     */
    boolean isNumber() {
        return false;
    }

    /**
     * Set the Calendar with the parsed field.
     *
     * <p>The default implementation does nothing.
     *
     * @param parser The parser calling this strategy
     * @param cal The <code>Calendar</code> to set
     * @param value The parsed field to translate and set in cal
     */
    void setCalendar(final FastDateParser parser, final Calendar cal, final String value) {}

    /**
     * Generate a <code>Pattern</code> regular expression to the <code>StringBuilder</code>
     * which will accept this field
     *
     * @param parser The parser calling this strategy
     * @param regex The <code>StringBuilder</code> to append to
     * @return true, if this field will set the calendar; false, if this field is a constant
     *     value
     */
    abstract boolean addRegex(FastDateParser parser, StringBuilder regex);
}
