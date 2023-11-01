package org.sqlite.date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class FormatCacheTest {

    Integer dateStyle,timeStyle;
    FormatCache<FastDateFormat> cache;
    Locale locale;
    String pattern;

    @BeforeEach
    public void setUp() throws ParseException {
        //Arrange
        locale = Locale.US;
        pattern="MMM d, y, h:mm:ss a";
        dateStyle = DateFormat.MEDIUM;
        timeStyle = DateFormat.MEDIUM;
        cache =
                new FormatCache<FastDateFormat>() {
                    @Override
                    protected FastDateFormat createInstance(
                            final String pattern, final TimeZone timeZone, final Locale locale) {
                        return new FastDateFormat(pattern, TimeZone.getDefault(), locale);
                    }
                };
    }

    @Test
    public void testGetDateTimeInstance() {
        //Act and Assert
        assertEquals(cache.getDateTimeInstance(dateStyle, timeStyle, TimeZone.getDefault(), null),
                cache.getInstance(pattern,TimeZone.getDefault(),null));
    }


    @Test
    public void testGetDateTimeInstanceForNullLocale() {
        //Act and Assert
        assertEquals(cache.getDateTimeInstance(dateStyle, timeStyle, TimeZone.getDefault(), null),
                cache.getInstance(pattern,TimeZone.getDefault(),null));
    }
}