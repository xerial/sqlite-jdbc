/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sqlite.date;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * FastDateFormat is a fast and thread-safe version of {@link java.text.SimpleDateFormat}.
 *
 * <p>To obtain an instance of FastDateFormat, use one of the static factory methods: {@link
 * #getInstance(String, TimeZone, Locale)}, {@link #getDateInstance(int, TimeZone, Locale)}, {@link
 * #getTimeInstance(int, TimeZone, Locale)}, or {@link #getDateTimeInstance(int, int, TimeZone,
 * Locale)}
 *
 * <p>Since FastDateFormat is thread safe, you can use a static member instance: <code>
 *   private static final FastDateFormat DATE_FORMATTER = FastDateFormat.getDateTimeInstance(FastDateFormat.LONG, FastDateFormat.SHORT);
 * </code>
 *
 * <p>This class can be used as a direct replacement to {@code SimpleDateFormat} in most formatting
 * and parsing situations. This class is especially useful in multi-threaded server environments.
 * {@code SimpleDateFormat} is not thread-safe in any JDK version, nor will it be as Sun have closed
 * the bug/RFE.
 *
 * <p>All patterns are compatible with SimpleDateFormat (except time zones and some year patterns -
 * see below).
 *
 * <p>Since 3.2, FastDateFormat supports parsing as well as printing.
 *
 * <p>Java 1.4 introduced a new pattern letter, {@code 'Z'}, to represent time zones in RFC822
 * format (eg. {@code +0800} or {@code -1100}). This pattern letter can be used here (on all JDK
 * versions).
 *
 * <p>In addition, the pattern {@code 'ZZ'} has been made to represent ISO 8601 full format time
 * zones (eg. {@code +08:00} or {@code -11:00}). This introduces a minor incompatibility with Java
 * 1.4, but at a gain of useful functionality.
 *
 * <p>Javadoc cites for the year pattern: <i>For formatting, if the number of pattern letters is 2,
 * the year is truncated to 2 digits; otherwise it is interpreted as a number.</i> Starting with
 * Java 1.7 a pattern of 'Y' or 'YYY' will be formatted as '2003', while it was '03' in former Java
 * versions. FastDateFormat implements the behavior of Java 7.
 *
 * @since 2.0
 * @version $Id$
 */
public class FastDateFormat extends Format implements DateParser, DatePrinter {
    /**
     * Required for serialization support.
     *
     * @see java.io.Serializable
     */
    private static final long serialVersionUID = 2L;

    /** FULL locale dependent date or time style. */
    public static final int FULL = DateFormat.FULL;
    /** LONG locale dependent date or time style. */
    public static final int LONG = DateFormat.LONG;
    /** MEDIUM locale dependent date or time style. */
    public static final int MEDIUM = DateFormat.MEDIUM;
    /** SHORT locale dependent date or time style. */
    public static final int SHORT = DateFormat.SHORT;

    private static final FormatCache<FastDateFormat> cache =
            new FormatCache<FastDateFormat>() {
                @Override
                protected FastDateFormat createInstance(
                        final String pattern, final TimeZone timeZone, final Locale locale) {
                    return new FastDateFormat(pattern, timeZone, locale);
                }
            };

    private final FastDatePrinter printer;
    private final FastDateParser parser;

    // -----------------------------------------------------------------------
    /**
     * Gets a formatter instance using the default pattern in the default locale.
     *
     * @return a date/time formatter
     */
    public static FastDateFormat getInstance() {
        return cache.getInstance();
    }

    /**
     * Gets a formatter instance using the specified pattern in the default locale.
     *
     * @param pattern {@link java.text.SimpleDateFormat} compatible pattern
     * @return a pattern based date/time formatter
     * @throws IllegalArgumentException if pattern is invalid
     */
    public static FastDateFormat getInstance(final String pattern) {
        return cache.getInstance(pattern, null, null);
    }

    /**
     * Gets a formatter instance using the specified pattern and time zone.
     *
     * @param pattern {@link java.text.SimpleDateFormat} compatible pattern
     * @param timeZone optional time zone, overrides time zone of formatted date
     * @return a pattern based date/time formatter
     * @throws IllegalArgumentException if pattern is invalid
     */
    public static FastDateFormat getInstance(final String pattern, final TimeZone timeZone) {
        return cache.getInstance(pattern, timeZone, null);
    }

    /**
     * Gets a formatter instance using the specified pattern and locale.
     *
     * @param pattern {@link java.text.SimpleDateFormat} compatible pattern
     * @param locale optional locale, overrides system locale
     * @return a pattern based date/time formatter
     * @throws IllegalArgumentException if pattern is invalid
     */
    public static FastDateFormat getInstance(final String pattern, final Locale locale) {
        return cache.getInstance(pattern, null, locale);
    }

    /**
     * Gets a formatter instance using the specified pattern, time zone and locale.
     *
     * @param pattern {@link java.text.SimpleDateFormat} compatible pattern
     * @param timeZone optional time zone, overrides time zone of formatted date
     * @param locale optional locale, overrides system locale
     * @return a pattern based date/time formatter
     * @throws IllegalArgumentException if pattern is invalid or {@code null}
     */
    public static FastDateFormat getInstance(
            final String pattern, final TimeZone timeZone, final Locale locale) {
        return cache.getInstance(pattern, timeZone, locale);
    }

    // -----------------------------------------------------------------------
    /**
     * Gets a date formatter instance using the specified style in the default time zone and locale.
     *
     * @param style date style: FULL, LONG, MEDIUM, or SHORT
     * @return a localized standard date formatter
     * @throws IllegalArgumentException if the Locale has no date pattern defined
     * @since 2.1
     */
    public static FastDateFormat getDateInstance(final int style) {
        return cache.getDateInstance(style, null, null);
    }

    /**
     * Gets a date formatter instance using the specified style and locale in the default time zone.
     *
     * @param style date style: FULL, LONG, MEDIUM, or SHORT
     * @param locale optional locale, overrides system locale
     * @return a localized standard date formatter
     * @throws IllegalArgumentException if the Locale has no date pattern defined
     * @since 2.1
     */
    public static FastDateFormat getDateInstance(final int style, final Locale locale) {
        return cache.getDateInstance(style, null, locale);
    }

    /**
     * Gets a date formatter instance using the specified style and time zone in the default locale.
     *
     * @param style date style: FULL, LONG, MEDIUM, or SHORT
     * @param timeZone optional time zone, overrides time zone of formatted date
     * @return a localized standard date formatter
     * @throws IllegalArgumentException if the Locale has no date pattern defined
     * @since 2.1
     */
    public static FastDateFormat getDateInstance(final int style, final TimeZone timeZone) {
        return cache.getDateInstance(style, timeZone, null);
    }

    /**
     * Gets a date formatter instance using the specified style, time zone and locale.
     *
     * @param style date style: FULL, LONG, MEDIUM, or SHORT
     * @param timeZone optional time zone, overrides time zone of formatted date
     * @param locale optional locale, overrides system locale
     * @return a localized standard date formatter
     * @throws IllegalArgumentException if the Locale has no date pattern defined
     */
    public static FastDateFormat getDateInstance(
            final int style, final TimeZone timeZone, final Locale locale) {
        return cache.getDateInstance(style, timeZone, locale);
    }

    // -----------------------------------------------------------------------
    /**
     * Gets a time formatter instance using the specified style in the default time zone and locale.
     *
     * @param style time style: FULL, LONG, MEDIUM, or SHORT
     * @return a localized standard time formatter
     * @throws IllegalArgumentException if the Locale has no time pattern defined
     * @since 2.1
     */
    public static FastDateFormat getTimeInstance(final int style) {
        return cache.getTimeInstance(style, null, null);
    }

    /**
     * Gets a time formatter instance using the specified style and locale in the default time zone.
     *
     * @param style time style: FULL, LONG, MEDIUM, or SHORT
     * @param locale optional locale, overrides system locale
     * @return a localized standard time formatter
     * @throws IllegalArgumentException if the Locale has no time pattern defined
     * @since 2.1
     */
    public static FastDateFormat getTimeInstance(final int style, final Locale locale) {
        return cache.getTimeInstance(style, null, locale);
    }

    /**
     * Gets a time formatter instance using the specified style and time zone in the default locale.
     *
     * @param style time style: FULL, LONG, MEDIUM, or SHORT
     * @param timeZone optional time zone, overrides time zone of formatted time
     * @return a localized standard time formatter
     * @throws IllegalArgumentException if the Locale has no time pattern defined
     * @since 2.1
     */
    public static FastDateFormat getTimeInstance(final int style, final TimeZone timeZone) {
        return cache.getTimeInstance(style, timeZone, null);
    }

    /**
     * Gets a time formatter instance using the specified style, time zone and locale.
     *
     * @param style time style: FULL, LONG, MEDIUM, or SHORT
     * @param timeZone optional time zone, overrides time zone of formatted time
     * @param locale optional locale, overrides system locale
     * @return a localized standard time formatter
     * @throws IllegalArgumentException if the Locale has no time pattern defined
     */
    public static FastDateFormat getTimeInstance(
            final int style, final TimeZone timeZone, final Locale locale) {
        return cache.getTimeInstance(style, timeZone, locale);
    }

    // -----------------------------------------------------------------------
    /**
     * Gets a date/time formatter instance using the specified style in the default time zone and
     * locale.
     *
     * @param dateStyle date style: FULL, LONG, MEDIUM, or SHORT
     * @param timeStyle time style: FULL, LONG, MEDIUM, or SHORT
     * @return a localized standard date/time formatter
     * @throws IllegalArgumentException if the Locale has no date/time pattern defined
     * @since 2.1
     */
    public static FastDateFormat getDateTimeInstance(final int dateStyle, final int timeStyle) {
        return cache.getDateTimeInstance(dateStyle, timeStyle, null, null);
    }

    /**
     * Gets a date/time formatter instance using the specified style and locale in the default time
     * zone.
     *
     * @param dateStyle date style: FULL, LONG, MEDIUM, or SHORT
     * @param timeStyle time style: FULL, LONG, MEDIUM, or SHORT
     * @param locale optional locale, overrides system locale
     * @return a localized standard date/time formatter
     * @throws IllegalArgumentException if the Locale has no date/time pattern defined
     * @since 2.1
     */
    public static FastDateFormat getDateTimeInstance(
            final int dateStyle, final int timeStyle, final Locale locale) {
        return cache.getDateTimeInstance(dateStyle, timeStyle, null, locale);
    }

    /**
     * Gets a date/time formatter instance using the specified style and time zone in the default
     * locale.
     *
     * @param dateStyle date style: FULL, LONG, MEDIUM, or SHORT
     * @param timeStyle time style: FULL, LONG, MEDIUM, or SHORT
     * @param timeZone optional time zone, overrides time zone of formatted date
     * @return a localized standard date/time formatter
     * @throws IllegalArgumentException if the Locale has no date/time pattern defined
     * @since 2.1
     */
    public static FastDateFormat getDateTimeInstance(
            final int dateStyle, final int timeStyle, final TimeZone timeZone) {
        return getDateTimeInstance(dateStyle, timeStyle, timeZone, null);
    }
    /**
     * Gets a date/time formatter instance using the specified style, time zone and locale.
     *
     * @param dateStyle date style: FULL, LONG, MEDIUM, or SHORT
     * @param timeStyle time style: FULL, LONG, MEDIUM, or SHORT
     * @param timeZone optional time zone, overrides time zone of formatted date
     * @param locale optional locale, overrides system locale
     * @return a localized standard date/time formatter
     * @throws IllegalArgumentException if the Locale has no date/time pattern defined
     */
    public static FastDateFormat getDateTimeInstance(
            final int dateStyle,
            final int timeStyle,
            final TimeZone timeZone,
            final Locale locale) {
        return cache.getDateTimeInstance(dateStyle, timeStyle, timeZone, locale);
    }

    // Constructor
    // -----------------------------------------------------------------------
    /**
     * Constructs a new FastDateFormat.
     *
     * @param pattern {@link java.text.SimpleDateFormat} compatible pattern
     * @param timeZone non-null time zone to use
     * @param locale non-null locale to use
     * @throws NullPointerException if pattern, timeZone, or locale is null.
     */
    protected FastDateFormat(final String pattern, final TimeZone timeZone, final Locale locale) {
        this(pattern, timeZone, locale, null);
    }

    // Constructor
    // -----------------------------------------------------------------------
    /**
     * Constructs a new FastDateFormat.
     *
     * @param pattern {@link java.text.SimpleDateFormat} compatible pattern
     * @param timeZone non-null time zone to use
     * @param locale non-null locale to use
     * @param centuryStart The start of the 100 year period to use as the "default century" for 2
     *     digit year parsing. If centuryStart is null, defaults to now - 80 years
     * @throws NullPointerException if pattern, timeZone, or locale is null.
     */
    protected FastDateFormat(
            final String pattern,
            final TimeZone timeZone,
            final Locale locale,
            final Date centuryStart) {
        printer = new FastDatePrinter(pattern, timeZone, locale);
        parser = new FastDateParser(pattern, timeZone, locale, centuryStart);
    }

    // Format methods
    // -----------------------------------------------------------------------
    /**
     * Formats a {@code Date}, {@code Calendar} or {@code Long} (milliseconds) object.
     *
     * @param obj the object to format
     * @param toAppendTo the buffer to append to
     * @param pos the position - ignored
     * @return the buffer passed in
     */
    @Override
    public StringBuffer format(
            final Object obj, final StringBuffer toAppendTo, final FieldPosition pos) {
        return printer.format(obj, toAppendTo, pos);
    }

    /**
     * Formats a millisecond {@code long} value.
     *
     * @param millis the millisecond value to format
     * @return the formatted string
     * @since 2.1
     */
    public String format(final long millis) {
        return printer.format(millis);
    }

    /**
     * Formats a {@code Date} object using a {@code GregorianCalendar}.
     *
     * @param date the date to format
     * @return the formatted string
     */
    public String format(final Date date) {
        return printer.format(date);
    }

    /**
     * Formats a {@code Calendar} object.
     *
     * @param calendar the calendar to format
     * @return the formatted string
     */
    public String format(final Calendar calendar) {
        return printer.format(calendar);
    }

    /**
     * Formats a millisecond {@code long} value into the supplied {@code StringBuffer}.
     *
     * @param millis the millisecond value to format
     * @param buf the buffer to format into
     * @return the specified string buffer
     * @since 2.1
     */
    public StringBuffer format(final long millis, final StringBuffer buf) {
        return printer.format(millis, buf);
    }

    /**
     * Formats a {@code Date} object into the supplied {@code StringBuffer} using a {@code
     * GregorianCalendar}.
     *
     * @param date the date to format
     * @param buf the buffer to format into
     * @return the specified string buffer
     */
    public StringBuffer format(final Date date, final StringBuffer buf) {
        return printer.format(date, buf);
    }

    /**
     * Formats a {@code Calendar} object into the supplied {@code StringBuffer}.
     *
     * @param calendar the calendar to format
     * @param buf the buffer to format into
     * @return the specified string buffer
     */
    public StringBuffer format(final Calendar calendar, final StringBuffer buf) {
        return printer.format(calendar, buf);
    }

    // Parsing
    // -----------------------------------------------------------------------

    /* (non-Javadoc)
     * @see DateParser#parse(java.lang.String)
     */
    public Date parse(final String source) throws ParseException {
        return parser.parse(source);
    }

    /* (non-Javadoc)
     * @see DateParser#parse(java.lang.String, java.text.ParsePosition)
     */
    public Date parse(final String source, final ParsePosition pos) {
        return parser.parse(source, pos);
    }

    /* (non-Javadoc)
     * @see java.text.Format#parseObject(java.lang.String, java.text.ParsePosition)
     */
    public Object parseObject(final String source, final ParsePosition pos) {
        return parser.parseObject(source, pos);
    }

    // Accessors
    // -----------------------------------------------------------------------
    /**
     * Gets the pattern used by this formatter.
     *
     * @return the pattern, {@link java.text.SimpleDateFormat} compatible
     */
    public String getPattern() {
        return printer.getPattern();
    }

    /**
     * Gets the time zone used by this formatter.
     *
     * <p>This zone is always used for {@code Date} formatting.
     *
     * @return the time zone
     */
    public TimeZone getTimeZone() {
        return printer.getTimeZone();
    }

    /**
     * Gets the locale used by this formatter.
     *
     * @return the locale
     */
    public Locale getLocale() {
        return printer.getLocale();
    }

    /**
     * Gets an estimate for the maximum string length that the formatter will produce.
     *
     * <p>The actual formatted length will almost always be less than or equal to this amount.
     *
     * @return the maximum formatted length
     */
    public int getMaxLengthEstimate() {
        return printer.getMaxLengthEstimate();
    }

    // Basics
    // -----------------------------------------------------------------------
    /**
     * Compares two objects for equality.
     *
     * @param obj the object to compare to
     * @return {@code true} if equal
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof FastDateFormat == false) {
            return false;
        }
        final FastDateFormat other = (FastDateFormat) obj;
        // no need to check parser, as it has same invariants as printer
        return printer.equals(other.printer);
    }

    /**
     * Returns a hashcode compatible with equals.
     *
     * @return a hashcode compatible with equals
     */
    @Override
    public int hashCode() {
        return printer.hashCode();
    }

    /**
     * Gets a debugging string version of this formatter.
     *
     * @return a debugging string
     */
    @Override
    public String toString() {
        return "FastDateFormat["
                + printer.getPattern()
                + ","
                + printer.getLocale()
                + ","
                + printer.getTimeZone().getID()
                + "]";
    }

    /**
     * Performs the formatting by applying the rules to the specified calendar.
     *
     * @param calendar the calendar to format
     * @param buf the buffer to format into
     * @return the specified string buffer
     */
    protected StringBuffer applyRules(final Calendar calendar, final StringBuffer buf) {
        return printer.applyRules(calendar, buf);
    }
}
