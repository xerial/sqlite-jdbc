package org.sqlite.util;

import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * A factory for {@link Logger} instances that uses SLF4J if present, falling back on a
 * java.util.logging implementation otherwise.
 */
public class LoggerFactory {
    static final boolean USE_SLF4J;

    static {
        boolean useSLF4J;
        try {
            Class.forName("org.slf4j.Logger");
            useSLF4J = true;
        } catch (Exception e) {
            useSLF4J = false;
        }
        USE_SLF4J = useSLF4J;
    }

    /**
     * Get a {@link Logger} instance for the given host class.
     *
     * @param hostClass the host class from which log messages will be issued
     * @return a Logger
     */
    public static Logger getLogger(Class<?> hostClass) {
        if (USE_SLF4J) {
            return new SLF4JLogger(hostClass);
        }

        return new JDKLogger(hostClass);
    }

    private static class JDKLogger implements Logger {
        final java.util.logging.Logger logger;

        public JDKLogger(Class<?> hostClass) {
            logger = java.util.logging.Logger.getLogger(hostClass.getCanonicalName());
        }

        @Override
        public void trace(Supplier<String> message) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, message.get());
            }
        }

        @Override
        public void info(Supplier<String> message) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, message.get());
            }
        }

        @Override
        public void warn(Supplier<String> message) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, message.get());
            }
        }

        @Override
        public void error(Supplier<String> message, Throwable t) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, message.get(), t);
            }
        }
    }

    private static class SLF4JLogger implements Logger {
        final org.slf4j.Logger logger;

        SLF4JLogger(Class<?> hostClass) {
            logger = org.slf4j.LoggerFactory.getLogger(hostClass);
        }

        @Override
        public void trace(Supplier<String> message) {
            if (logger.isTraceEnabled()) {
                logger.trace(message.get());
            }
        }

        @Override
        public void info(Supplier<String> message) {
            if (logger.isInfoEnabled()) {
                logger.info(message.get());
            }
        }

        @Override
        public void warn(Supplier<String> message) {
            if (logger.isWarnEnabled()) {
                logger.warn(message.get());
            }
        }

        @Override
        public void error(Supplier<String> message, Throwable t) {
            if (logger.isErrorEnabled()) {
                logger.error(message.get(), t);
            }
        }
    }
}
