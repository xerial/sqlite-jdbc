package org.sqlite.date;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class FastDateParserTest {

    @Test
    public void testEquals_SamePatternTimeZoneAndLocale() {
        // Arrange
        FastDateParser parser1 = new FastDateParser("dd-MM-yyyy", TimeZone.getTimeZone("UTC"), Locale.US);
        FastDateParser parser2 = new FastDateParser("dd-MM-yyyy", TimeZone.getTimeZone("UTC"), Locale.US);

        // Act and Assert
        assertTrue(parser1.equals(parser2));
    }

    @Test
    public void testEquals_DifferentPattern() {
        // Arrange
        FastDateParser parser1 = new FastDateParser("dd-MM-yyyy", TimeZone.getTimeZone("UTC"), Locale.US);
        FastDateParser parser2 = new FastDateParser("MM-dd-yyyy", TimeZone.getTimeZone("UTC"), Locale.US);

        // Act and Assert
        assertFalse(parser1.equals(parser2));
    }

    @Test
    public void testEquals_DifferentTimeZone() {
        // Arrange
        FastDateParser parser1 = new FastDateParser("dd-MM-yyyy", TimeZone.getTimeZone("UTC"), Locale.US);
        FastDateParser parser2 = new FastDateParser("dd-MM-yyyy", TimeZone.getTimeZone("GMT"), Locale.US);

        // Act and Assert
        assertFalse(parser1.equals(parser2));
    }

    @Test
    public void testEquals_DifferentLocale() {
        // Arrange
        FastDateParser parser1 = new FastDateParser("dd-MM-yyyy", TimeZone.getTimeZone("UTC"), Locale.US);
        FastDateParser parser2 = new FastDateParser("dd-MM-yyyy", TimeZone.getTimeZone("UTC"), Locale.FRANCE);

        // Act and Assert
        assertFalse(parser1.equals(parser2));
    }

    @Test
    public void testEquals_NotFastDateParser() {
        // Arrange
        FastDateParser parser = new FastDateParser("dd-MM-yyyy", TimeZone.getTimeZone("UTC"), Locale.US);
        Object notParser = new Object();

        // Act and Assert
        assertFalse(parser.equals(notParser));
    }
}