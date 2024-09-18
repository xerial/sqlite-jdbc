package org.sqlite.util;

/** A simple internal Logger interface. */
public interface Logger {
    boolean isTraceEnabled();

    void trace(String format, Object o1, Object o2);

    void info(String format, Object o1, Object o2);

    void warn(String msg);

    void error(String message, Throwable t);

    void error(String format, Object o1, Throwable t);

    void error(String format, Object o1, Object o2, Throwable t);
}
