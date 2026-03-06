package org.sqlite.util;

import java.util.function.Supplier;

/** A simple internal Logger interface. */
public interface Logger {
    void trace(Supplier<String> message);

    void info(Supplier<String> message);

    void warn(Supplier<String> message);

    void error(Supplier<String> message, Throwable t);
}
