package org.sqlite;

import org.sqlite.date.FastDateFormat;

import java.sql.Connection;
import java.util.Properties;

/**
 * Connection local cofigurations
 */
public class SQLiteConnectionConfig implements Cloneable
{
    /* Date storage class*/
    public final static String DEFAULT_DATE_STRING_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    private SQLiteConfig.DateClass dateClass = SQLiteConfig.DateClass.INTEGER;
    private SQLiteConfig.DatePrecision datePrecision = SQLiteConfig.DatePrecision.MILLISECONDS; //Calendar.SECOND or Calendar.MILLISECOND
    private String dateStringFormat = DEFAULT_DATE_STRING_FORMAT;
    private FastDateFormat dateFormat = FastDateFormat.getInstance(dateStringFormat);

    private int transactionIsolation = Connection.TRANSACTION_SERIALIZABLE;
    private SQLiteConfig.TransactionMode transactionMode = SQLiteConfig.TransactionMode.DEFFERED;
    private boolean autoCommit = false;

    public static SQLiteConnectionConfig fromPragmaTable(Properties pragmaTable) {
        return new SQLiteConnectionConfig(
                SQLiteConfig.DateClass.getDateClass(pragmaTable.getProperty(SQLiteConfig.Pragma.DATE_CLASS.pragmaName, SQLiteConfig.DateClass.INTEGER.name())),
                SQLiteConfig.DatePrecision.getPrecision(pragmaTable.getProperty(SQLiteConfig.Pragma.DATE_PRECISION.pragmaName, SQLiteConfig.DatePrecision.MILLISECONDS.name())),
                pragmaTable.getProperty(SQLiteConfig.Pragma.DATE_STRING_FORMAT.pragmaName, DEFAULT_DATE_STRING_FORMAT),
                Connection.TRANSACTION_SERIALIZABLE,
                SQLiteConfig.TransactionMode.getMode(
                        pragmaTable.getProperty(SQLiteConfig.Pragma.TRANSACTION_MODE.pragmaName, SQLiteConfig.TransactionMode.DEFFERED.name())),
                false);
    }

    public SQLiteConnectionConfig(
            SQLiteConfig.DateClass dateClass,
            SQLiteConfig.DatePrecision datePrecision,
            String dateStringFormat,
            int transactionIsolation,
            SQLiteConfig.TransactionMode transactionMode,
            boolean autoCommit
    )
    {
        setDateClass(dateClass);
        setDatePrecision(datePrecision);
        setDateStringFormat(dateStringFormat);
        setTransactionIsolation(transactionIsolation);
        setTransactionMode(transactionMode);
        setAutoCommit(autoCommit);
    }

    public SQLiteConnectionConfig copyConfig() {
        return new SQLiteConnectionConfig(
                dateClass,
                datePrecision,
                dateStringFormat,
                transactionIsolation,
                transactionMode,
                autoCommit
        );
    }

    public long getDateMultiplier()
    {
        return (datePrecision == SQLiteConfig.DatePrecision.MILLISECONDS) ? 1L : 1000L;
    }

    public SQLiteConfig.DateClass getDateClass()
    {
        return dateClass;
    }

    public void setDateClass(SQLiteConfig.DateClass dateClass)
    {
        this.dateClass = dateClass;
    }

    public SQLiteConfig.DatePrecision getDatePrecision()
    {
        return datePrecision;
    }

    public void setDatePrecision(SQLiteConfig.DatePrecision datePrecision)
    {
        this.datePrecision = datePrecision;
    }

    public String getDateStringFormat()
    {
        return dateStringFormat;
    }

    public void setDateStringFormat(String dateStringFormat)
    {
        this.dateStringFormat = dateStringFormat;
        this.dateFormat = FastDateFormat.getInstance(dateStringFormat);
    }

    public FastDateFormat getDateFormat()
    {
        return dateFormat;
    }

    public boolean isAutoCommit()
    {
        return autoCommit;
    }

    public void setAutoCommit(boolean autoCommit)
    {
        this.autoCommit = autoCommit;
    }

    public int getTransactionIsolation()
    {
        return transactionIsolation;
    }

    public void setTransactionIsolation(int transactionIsolation)
    {
        this.transactionIsolation = transactionIsolation;
    }

    public SQLiteConfig.TransactionMode getTransactionMode()
    {
        return transactionMode;
    }

    public void setTransactionMode(SQLiteConfig.TransactionMode transactionMode)
    {
        this.transactionMode = transactionMode;
    }
}
