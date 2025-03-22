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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FastDateParser is a fast and thread-safe version of {@link java.text.SimpleDateFormat}.
 *
 * <p>To obtain a proxy to a FastDateParser, use {@link FastDateFormat#getInstance(String, TimeZone,
 * Locale)} or another variation of the factory methods of {@link FastDateFormat}.
 *
 * <p>Since FastDateParser is thread safe, you can use a static member instance: <code>
 *     private static final DateParser DATE_PARSER = FastDateFormat.getInstance("yyyy-MM-dd");
 * </code>
 *
 * <p>This class can be used as a direct replacement for <code>SimpleDateFormat</code> in most
 * parsing situations. This class is especially useful in multi-threaded server environments. <code>
 * SimpleDateFormat</code> is not thread-safe in any JDK version, nor will it be as Sun has closed
 * the <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4228335">bug</a>/RFE.
 *
 * <p>Only parsing is supported by this class, but all patterns are compatible with
 * SimpleDateFormat.
 *
 * <p>The class operates in lenient mode, so for example a time of 90 minutes is treated as 1 hour
 * 30 minutes.
 *
 * <p>Timing tests indicate this class is as about as fast as SimpleDateFormat in single thread
 * applications and about 25% faster in multi-thread applications.
 *
 * @version $Id$
 * @since 3.2
 * @see FastDatePrinter
 */
public class FastDateParser implements DateParser, Serializable {
    /**
     * Required for serialization support.
     *
     * @see java.io.Serializable
     */
    private static final long serialVersionUID = 2L;

    static final Locale JAPANESE_IMPERIAL = new Locale("ja", "JP", "JP");

    // defining fields
    private final String pattern;
    private final TimeZone timeZone;
    private final Locale locale;
    private final int century;
    private final int startYear;

    // derived fields
    private transient Pattern parsePattern;
    private transient DateParsingStrategy[] strategies;

    // dynamic fields to communicate with Strategy
    private transient String currentFormatField;
    private transient DateParsingStrategy nextDateParsingStrategy;

    /**
     * Constructs a new FastDateParser. Use {@link FastDateFormat#getInstance(String, TimeZone,
     * Locale)} or another variation of the factory methods of {@link FastDateFormat} to get a
     * cached FastDateParser instance.
     *
     * @param pattern non-null {@link java.text.SimpleDateFormat} compatible pattern
     * @param timeZone non-null time zone to use
     * @param locale non-null locale
     */
    protected FastDateParser(final String pattern, final TimeZone timeZone, final Locale locale) {
        this(pattern, timeZone, locale, null);
    }

    /**
     * Constructs a new FastDateParser.
     *
     * @param pattern non-null {@link java.text.SimpleDateFormat} compatible pattern
     * @param timeZone non-null time zone to use
     * @param locale non-null locale
     * @param centuryStart The start of the century for 2 digit year parsing
     * @since 3.3
     */
    protected FastDateParser(
            final String pattern,
            final TimeZone timeZone,
            final Locale locale,
            final Date centuryStart) {
        this.pattern = pattern;
        this.timeZone = timeZone;
        this.locale = locale;

        final Calendar definingCalendar = Calendar.getInstance(timeZone, locale);
        int centuryStartYear;
        if (centuryStart != null) {
            definingCalendar.setTime(centuryStart);
            centuryStartYear = definingCalendar.get(Calendar.YEAR);
        } else if (locale.equals(JAPANESE_IMPERIAL)) {
            centuryStartYear = 0;
        } else {
            // from 80 years ago to 20 years from now
            definingCalendar.setTime(new Date());
            centuryStartYear = definingCalendar.get(Calendar.YEAR) - 80;
        }
        century = centuryStartYear / 100 * 100;
        startYear = centuryStartYear - century;

        init(definingCalendar);
    }

    /**
     * Initialize derived fields from defining fields. This is called from constructor and from
     * readObject (de-serialization)
     *
     * @param definingCalendar the {@link java.util.Calendar} instance used to initialize this
     *     FastDateParser
     */
    private void init(final Calendar definingCalendar) {

        final StringBuilder regex = new StringBuilder();
        final List<DateParsingStrategy> collector = new ArrayList<DateParsingStrategy>();

        final Matcher patternMatcher = formatPattern.matcher(pattern);
        if (!patternMatcher.lookingAt()) {
            throw new IllegalArgumentException(
                    "Illegal pattern character '"
                            + pattern.charAt(patternMatcher.regionStart())
                            + "'");
        }

        currentFormatField = patternMatcher.group();
        DateParsingStrategy currentDateParsingStrategy = getStrategy(currentFormatField, definingCalendar);
        for (; ; ) {
            patternMatcher.region(patternMatcher.end(), patternMatcher.regionEnd());
            if (!patternMatcher.lookingAt()) {
                nextDateParsingStrategy = null;
                break;
            }
            final String nextFormatField = patternMatcher.group();
            nextDateParsingStrategy = getStrategy(nextFormatField, definingCalendar);
            if (currentDateParsingStrategy.addRegex(this, regex)) {
                collector.add(currentDateParsingStrategy);
            }
            currentFormatField = nextFormatField;
            currentDateParsingStrategy = nextDateParsingStrategy;
        }
        if (patternMatcher.regionStart() != patternMatcher.regionEnd()) {
            throw new IllegalArgumentException(
                    "Failed to parse \""
                            + pattern
                            + "\" ; gave up at index "
                            + patternMatcher.regionStart());
        }
        if (currentDateParsingStrategy.addRegex(this, regex)) {
            collector.add(currentDateParsingStrategy);
        }
        currentFormatField = null;
        strategies = collector.toArray(new DateParsingStrategy[collector.size()]);
        parsePattern = Pattern.compile(regex.toString());
    }

    // Accessors
    // -----------------------------------------------------------------------
    /* (non-Javadoc)
     * @see org.apache.commons.lang3.time.DateParser#getPattern()
     */
    public String getPattern() {
        return pattern;
    }

    /* (non-Javadoc)
     * @see org.apache.commons.lang3.time.DateParser#getTimeZone()
     */
    public TimeZone getTimeZone() {
        return timeZone;
    }

    /* (non-Javadoc)
     * @see org.apache.commons.lang3.time.DateParser#getLocale()
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns the generated pattern (for testing purposes).
     *
     * @return the generated pattern
     */
    Pattern getParsePattern() {
        return parsePattern;
    }

    // Basics
    // -----------------------------------------------------------------------
    /**
     * Compare another object for equality with this object.
     *
     * @param obj the object to compare to
     * @return <code>true</code>if equal to this instance
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof FastDateParser)) {
            return false;
        }
        final FastDateParser other = (FastDateParser) obj;
        return pattern.equals(other.pattern)
                && timeZone.equals(other.timeZone)
                && locale.equals(other.locale);
    }

    /**
     * Return a hashcode compatible with equals.
     *
     * @return a hashcode compatible with equals
     */
    @Override
    public int hashCode() {
        return pattern.hashCode() + 13 * (timeZone.hashCode() + 13 * locale.hashCode());
    }

    /**
     * Get a string version of this formatter.
     *
     * @return a debugging string
     */
    @Override
    public String toString() {
        return "FastDateParser[" + pattern + "," + locale + "," + timeZone.getID() + "]";
    }

    // Serializing
    // -----------------------------------------------------------------------
    /**
     * Create the object after serialization. This implementation reinitializes the transient
     * properties.
     *
     * @param in ObjectInputStream from which the object is being deserialized.
     * @throws IOException if there is an IO issue.
     * @throws ClassNotFoundException if a class cannot be found.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        final Calendar definingCalendar = Calendar.getInstance(timeZone, locale);
        init(definingCalendar);
    }

    /* (non-Javadoc)
     * @see org.apache.commons.lang3.time.DateParser#parseObject(java.lang.String)
     */
    public Object parseObject(final String source) throws ParseException {
        return parse(source);
    }

    /* (non-Javadoc)
     * @see org.apache.commons.lang3.time.DateParser#parse(java.lang.String)
     */
    public Date parse(final String source) throws ParseException {
        String normalizedSource = source.length() == 19 ? (source + ".000") : source;
        final Date date = parse(normalizedSource, new ParsePosition(0));
        if (date == null) {
            // Add a note re supported date range
            if (locale.equals(JAPANESE_IMPERIAL)) {
                throw new ParseException(
                        "(The "
                                + locale
                                + " locale does not support dates before 1868 AD)\n"
                                + "Unparseable date: \""
                                + normalizedSource
                                + "\" does not match "
                                + parsePattern.pattern(),
                        0);
            }
            throw new ParseException(
                    "Unparseable date: \""
                            + normalizedSource
                            + "\" does not match "
                            + parsePattern.pattern(),
                    0);
        }
        return date;
    }

    /* (non-Javadoc)
     * @see org.apache.commons.lang3.time.DateParser#parseObject(java.lang.String, java.text.ParsePosition)
     */
    public Object parseObject(final String source, final ParsePosition pos) {
        return parse(source, pos);
    }

    /**
     * This implementation updates the ParsePosition if the parse succeeds. However, unlike the
     * method {@link java.text.SimpleDateFormat#parse(String, ParsePosition)} it is not able to set
     * the error Index - i.e. {@link ParsePosition#getErrorIndex()} - if the parse fails.
     *
     * <p>To determine if the parse has succeeded, the caller must check if the current parse
     * position given by {@link ParsePosition#getIndex()} has been updated. If the input buffer has
     * been fully parsed, then the index will point to just after the end of the input buffer.
     *
     * <p>See org.apache.commons.lang3.time.DateParser#parse(java.lang.String,
     * java.text.ParsePosition) {@inheritDoc}
     */
    public Date parse(final String source, final ParsePosition pos) {
        final int offset = pos.getIndex();
        final Matcher matcher = parsePattern.matcher(source.substring(offset));
        if (!matcher.lookingAt()) {
            return null;
        }
        // timing tests indicate getting new instance is 19% faster than cloning
        final Calendar cal = Calendar.getInstance(timeZone, locale);
        cal.clear();

        for (int i = 0; i < strategies.length; ) {
            final DateParsingStrategy dateParsingStrategy = strategies[i++];
            dateParsingStrategy.setCalendar(this, cal, matcher.group(i));
        }
        pos.setIndex(offset + matcher.end());
        return cal.getTime();
    }

    // Support for strategies
    // -----------------------------------------------------------------------

    /**
     * Escape constant fields into regular expression
     *
     * @param regex The destination regex
     * @param value The source field
     * @param unquote If true, replace two success quotes ('') with single quote (')
     * @return The <code>StringBuilder</code>
     */
    protected static StringBuilder escapeRegex(
            final StringBuilder regex, final String value, final boolean unquote) {
        regex.append("\\Q");
        for (int i = 0; i < value.length(); ++i) {
            char c = value.charAt(i);
            switch (c) {
                case '\'':
                    if (unquote) {
                        if (++i == value.length()) {
                            return regex;
                        }
                        c = value.charAt(i);
                    }
                    break;
                case '\\':
                    if (++i == value.length()) {
                        break;
                    }
                    /*
                     * If we have found \E, we replace it with \E\\E\Q, i.e. we stop the quoting,
                     * quote the \ in \E, then restart the quoting.
                     *
                     * Otherwise we just output the two characters.
                     * In each case the initial \ needs to be output and the final char is done at the end
                     */
                    regex.append(c); // we always want the original \
                    c = value.charAt(i); // Is it followed by E ?
                    if (c == 'E') { // \E detected
                        regex.append("E\\\\E\\"); // see comment above
                        c = 'Q'; // appended below
                    }
                    break;
                default:
                    break;
            }
            regex.append(c);
        }
        regex.append("\\E");
        return regex;
    }

    /**
     * Get the short and long values displayed for a field
     *
     * @param field The field of interest
     * @param definingCalendar The calendar to obtain the short and long values
     * @param locale The locale of display names
     * @return A Map of the field key / value pairs
     */
    protected static Map<String, Integer> getDisplayNames(
            final int field, final Calendar definingCalendar, final Locale locale) {
        return definingCalendar.getDisplayNames(field, Calendar.ALL_STYLES, locale);
    }

    /**
     * Adjust dates to be within appropriate century
     *
     * @param twoDigitYear The year to adjust
     * @return A value between centuryStart(inclusive) to centuryStart+100(exclusive)
     */
    private int adjustYear(final int twoDigitYear) {
        final int trial = century + twoDigitYear;
        return twoDigitYear >= startYear ? trial : trial + 100;
    }

    /**
     * Is the next field a number?
     *
     * @return true, if next field will be a number
     */
    boolean isNextNumber() {
        return nextDateParsingStrategy != null && nextDateParsingStrategy.isNumber();
    }

    /**
     * What is the width of the current field?
     *
     * @return The number of characters in the current format field
     */
    int getFieldWidth() {
        return currentFormatField.length();
    }


    /** A <code>Pattern</code> to parse the user supplied SimpleDateFormat pattern */
    private static final Pattern formatPattern =
            Pattern.compile(
                    "D+|E+|F+|G+|H+|K+|M+|S+|W+|X+|Z+|a+|d+|h+|k+|m+|s+|w+|y+|z+|''|'[^']++(''[^']*+)*+'|[^'A-Za-z]++");

    /**
     * Obtain a Strategy given a field from a SimpleDateFormat pattern
     *
     * @param formatField A sub-sequence of the SimpleDateFormat pattern
     * @param definingCalendar The calendar to obtain the short and long values
     * @return The Strategy that will handle parsing for the field
     */
    private DateParsingStrategy getStrategy(final String formatField, final Calendar definingCalendar) {
        switch (formatField.charAt(0)) {
            case '\'':
                if (formatField.length() > 2) {
                    return new CopyQuotedDateParsingStrategy(
                            formatField.substring(1, formatField.length() - 1));
                }
                // $FALL-THROUGH$
            default:
                return new CopyQuotedDateParsingStrategy(formatField);
            case 'D':
                return DAY_OF_YEAR_DATE_PARSING_STRATEGY;
            case 'E':
                return getLocaleSpecificStrategy(Calendar.DAY_OF_WEEK, definingCalendar);
            case 'F':
                return DAY_OF_WEEK_IN_MONTH_DATE_PARSING_STRATEGY;
            case 'G':
                return getLocaleSpecificStrategy(Calendar.ERA, definingCalendar);
            case 'H': // Hour in day (0-23)
                return HOUR_OF_DAY_DATE_PARSING_STRATEGY;
            case 'K': // Hour in am/pm (0-11)
                return HOUR_DATE_PARSING_STRATEGY;
            case 'M':
                return formatField.length() >= 3
                        ? getLocaleSpecificStrategy(Calendar.MONTH, definingCalendar)
                        : NUMBER_MONTH_DATE_PARSING_STRATEGY;
            case 'S':
                return MILLISECOND_DATE_PARSING_STRATEGY;
            case 'W':
                return WEEK_OF_MONTH_DATE_PARSING_STRATEGY;
            case 'a':
                return getLocaleSpecificStrategy(Calendar.AM_PM, definingCalendar);
            case 'd':
                return DAY_OF_MONTH_DATE_PARSING_STRATEGY;
            case 'h': // Hour in am/pm (1-12), i.e. midday/midnight is 12, not 0
                return HOUR_12_DATE_PARSING_STRATEGY;
            case 'k': // Hour in day (1-24), i.e. midnight is 24, not 0
                return HOUR_24_OF_DAY_DATE_PARSING_STRATEGY;
            case 'm':
                return MINUTE_DATE_PARSING_STRATEGY;
            case 's':
                return SECOND_DATE_PARSING_STRATEGY;
            case 'w':
                return WEEK_OF_YEAR_DATE_PARSING_STRATEGY;
            case 'y':
                return formatField.length() > 2 ? LITERAL_YEAR_DATE_PARSING_STRATEGY : ABBREVIATED_YEAR_DATE_PARSING_STRATEGY;
            case 'X':
                return ISO8601TimeZoneDateParsingStrategy.getStrategy(formatField.length());
            case 'Z':
                if (formatField.equals("ZZ")) {
                    return ISO_8601_DATE_PARSING_STRATEGY;
                }
                // $FALL-THROUGH$
            case 'z':
                return getLocaleSpecificStrategy(Calendar.ZONE_OFFSET, definingCalendar);
        }
    }

    @SuppressWarnings("unchecked") // OK because we are creating an array with no entries
    private static final ConcurrentMap<Locale, DateParsingStrategy>[] caches =
            new ConcurrentMap[Calendar.FIELD_COUNT];

    /**
     * Get a cache of Strategies for a particular field
     *
     * @param field The Calendar field
     * @return a cache of Locale to Strategy
     */
    private static ConcurrentMap<Locale, DateParsingStrategy> getCache(final int field) {
        synchronized (caches) {
            if (caches[field] == null) {
                caches[field] = new ConcurrentHashMap<Locale, DateParsingStrategy>(3);
            }
            return caches[field];
        }
    }

    /**
     * Construct a Strategy that parses a Text field
     *
     * @param field The Calendar field
     * @param definingCalendar The calendar to obtain the short and long values
     * @return a TextStrategy for the field and Locale
     */
    private DateParsingStrategy getLocaleSpecificStrategy(final int field, final Calendar definingCalendar) {
        final ConcurrentMap<Locale, DateParsingStrategy> cache = getCache(field);
        DateParsingStrategy dateParsingStrategy = cache.get(locale);
        if (dateParsingStrategy == null) {
            dateParsingStrategy =
                    field == Calendar.ZONE_OFFSET
                            ? new TimeZoneDateParsingStrategy(locale)
                            : new CaseInsensitiveTextStrategy(field, definingCalendar, locale);
            final DateParsingStrategy inCache = cache.putIfAbsent(locale, dateParsingStrategy);
            if (inCache != null) {
                return inCache;
            }
        }
        return dateParsingStrategy;
    }

    private static final DateParsingStrategy ABBREVIATED_YEAR_DATE_PARSING_STRATEGY =
            new NumberDateParsingStrategy(Calendar.YEAR) {
                /** {@inheritDoc} */
                @Override
                void setCalendar(
                        final FastDateParser parser, final Calendar cal, final String value) {
                    int iValue = Integer.parseInt(value);
                    if (iValue < 100) {
                        iValue = parser.adjustYear(iValue);
                    }
                    cal.set(Calendar.YEAR, iValue);
                }
            };

    private static final DateParsingStrategy NUMBER_MONTH_DATE_PARSING_STRATEGY =
            new NumberDateParsingStrategy(Calendar.MONTH) {
                @Override
                int modify(final int iValue) {
                    return iValue - 1;
                }
            };
    private static final DateParsingStrategy LITERAL_YEAR_DATE_PARSING_STRATEGY = new NumberDateParsingStrategy(Calendar.YEAR);
    private static final DateParsingStrategy WEEK_OF_YEAR_DATE_PARSING_STRATEGY = new NumberDateParsingStrategy(Calendar.WEEK_OF_YEAR);
    private static final DateParsingStrategy WEEK_OF_MONTH_DATE_PARSING_STRATEGY =
            new NumberDateParsingStrategy(Calendar.WEEK_OF_MONTH);
    private static final DateParsingStrategy DAY_OF_YEAR_DATE_PARSING_STRATEGY = new NumberDateParsingStrategy(Calendar.DAY_OF_YEAR);
    private static final DateParsingStrategy DAY_OF_MONTH_DATE_PARSING_STRATEGY = new NumberDateParsingStrategy(Calendar.DAY_OF_MONTH);
    private static final DateParsingStrategy DAY_OF_WEEK_IN_MONTH_DATE_PARSING_STRATEGY =
            new NumberDateParsingStrategy(Calendar.DAY_OF_WEEK_IN_MONTH);
    private static final DateParsingStrategy HOUR_OF_DAY_DATE_PARSING_STRATEGY = new NumberDateParsingStrategy(Calendar.HOUR_OF_DAY);
    private static final DateParsingStrategy HOUR_24_OF_DAY_DATE_PARSING_STRATEGY =
            new NumberDateParsingStrategy(Calendar.HOUR_OF_DAY) {
                @Override
                int modify(final int iValue) {
                    return iValue == 24 ? 0 : iValue;
                }
            };
    private static final DateParsingStrategy HOUR_12_DATE_PARSING_STRATEGY =
            new NumberDateParsingStrategy(Calendar.HOUR) {
                @Override
                int modify(final int iValue) {
                    return iValue == 12 ? 0 : iValue;
                }
            };
    private static final DateParsingStrategy HOUR_DATE_PARSING_STRATEGY = new NumberDateParsingStrategy(Calendar.HOUR);
    private static final DateParsingStrategy MINUTE_DATE_PARSING_STRATEGY = new NumberDateParsingStrategy(Calendar.MINUTE);
    private static final DateParsingStrategy SECOND_DATE_PARSING_STRATEGY = new NumberDateParsingStrategy(Calendar.SECOND);
    private static final DateParsingStrategy MILLISECOND_DATE_PARSING_STRATEGY = new NumberDateParsingStrategy(Calendar.MILLISECOND);
    private static final DateParsingStrategy ISO_8601_DATE_PARSING_STRATEGY =
            new ISO8601TimeZoneDateParsingStrategy("(Z|(?:[+-]\\d{2}(?::?\\d{2})?))");
}
