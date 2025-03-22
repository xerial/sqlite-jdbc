package org.sqlite.util;

import java.util.regex.Pattern;

public final class PatternConstants {
    /**
     * Pattern to match integer or boolean types.
     */
    public static final Pattern TYPE_INTEGER = Pattern.compile(".*(INT|BOOL).*");

    /**
     * Pattern to match varchar, text, or blob types.
     */
    public static final Pattern TYPE_VARCHAR = Pattern.compile(".*(CHAR|CLOB|TEXT|BLOB).*");

    /**
     * Pattern to match float, double, decimal, or numeric types.
     */
    public static final Pattern TYPE_FLOAT = Pattern.compile(".*(REAL|FLOA|DOUB|DEC|NUM).*");

    /** 
     * Pattern used to extract column order for an unnamed primary key. 
     */
    public static final Pattern PK_UNNAMED_PATTERN = Pattern.compile(".*PRIMARY\\s+KEY\\s*\\((.*?)\\).*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     *  Pattern used to extract a named primary key. 
     **/
    public static final Pattern PK_NAMED_PATTERN = Pattern.compile(".*CONSTRAINT\\s*(.*?)\\s*PRIMARY\\s+KEY\\s*\\((.*?)\\).*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     *  Pattern used to extract a named primary key. 
     */
    public static final Pattern FK_NAMED_PATTERN = Pattern.compile("CONSTRAINT\\s*\"?([A-Za-z_][A-Za-z\\d_]*)?\"?\\s*FOREIGN\\s+KEY\\s*\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     *  A <code>Pattern</code> to parse the user supplied SimpleDateFormat pattern 
     */
    public static final Pattern FORMAT_PATTERN = Pattern.compile("D+|E+|F+|G+|H+|K+|M+|S+|W+|X+|Z+|a+|d+|h+|k+|m+|s+|w+|y+|z+|''|'[^']++(''[^']*+)*+'|[^'A-Za-z]++");

    /**
     *  Pattern used to extract the column type name from table column definition. 
     */
    public static final Pattern COLUMN_TYPENAME = Pattern.compile("([^\\(]*)");

    /**
     *  Pattern used to extract the column type name from a cast(col as type) 
     */
    public static final Pattern COLUMN_TYPECAST = Pattern.compile("cast\\(.*?\\s+as\\s+(.*?)\\s*\\)");

    /**
     * Pattern used to extract the precision and scale from column meta returned by the JDBC driver.
     */
    public static final Pattern COLUMN_PRECISION = Pattern.compile(".*?\\((.*?)\\)");

    /**
     * Pattern used for matching insert statements of the general format starting with INSERT or REPLACE.
     * CTEs used prior to the insert or replace keyword are also be permitted.
    */
    public static final Pattern INSERT_PATTERN = Pattern.compile( "^\\s*(?:with\\s+.+\\(.+?\\))*\\s*(?:insert|replace)\\s*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    public static final Pattern BACKUP_CMD = Pattern.compile("backup(\\s+(\"[^\"]*\"|'[^\']*\'|\\S+))?\\s+to\\s+(\"[^\"]*\"|'[^\']*\'|\\S+)",Pattern.CASE_INSENSITIVE);

    public static final Pattern RESTORE_CMD = Pattern.compile("restore(\\s+(\"[^\"]*\"|'[^\']*\'|\\S+))?\\s+from\\s+(\"[^\"]*\"|'[^\']*\'|\\S+)", Pattern.CASE_INSENSITIVE);

}
