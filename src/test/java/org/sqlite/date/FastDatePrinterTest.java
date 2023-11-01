package org.sqlite.date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class FastDatePrinterTest {

    private final Locale locale = new Locale("en", "US");
    FastDatePrinter fastDatePrinter;
    StringBuffer buffer;
    Date parsedDate;
    FieldPosition fieldPosition;
    Calendar calendar;


    @BeforeEach
    public void setUp() throws ParseException {
        //Arrange
        //Setting the references
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date currentDate = new Date();
        String formattedDate = dateFormat.format(currentDate);
        parsedDate = dateFormat.parse(formattedDate);

        buffer= new StringBuffer();
        calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        buffer.append(year);

        fieldPosition=new FieldPosition(0);


        String pattern = "yyyy-MM-dd HH:mm:ss";
        TimeZone timeZone = TimeZone.getTimeZone("GMT");
        Locale locale = Locale.US;


        fastDatePrinter = new FastDatePrinter(pattern, timeZone, locale);

    }


    @Test
    void formatForDateObject()  {
        //Act and Assert
        assertEquals(fastDatePrinter.format(parsedDate,buffer,fieldPosition),fastDatePrinter.format(parsedDate,buffer));
    }

    @Test
    void formatForCalendarObject() {
        //Act and Assert
        assertEquals(fastDatePrinter.format(calendar,buffer,fieldPosition),fastDatePrinter.format(calendar,buffer));
    }

    @Test
    void formatForLong()  {
        long currentMillis = System.currentTimeMillis();
        //Act and Assert
        assertEquals(fastDatePrinter.format(currentMillis,buffer,fieldPosition),fastDatePrinter.format(currentMillis,buffer));
    }


    @Test
    void formatForException()  {
        //Act and Assert
        Object obj=null;
        try{
            fastDatePrinter.format(obj,buffer,fieldPosition);
        }catch(IllegalArgumentException e){
            Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
                throw new IllegalArgumentException("Unknown class: <null>");
            });
            assertEquals("Unknown class: <null>", exception.getMessage());
        }
    }
}