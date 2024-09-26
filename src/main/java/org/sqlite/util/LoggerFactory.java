package org.sqlite.util;

import java.text.MessageFormat;

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
        public boolean isTraceEnabled() {
            return logger.isLoggable(java.util.logging.Level.FINEST);
        }

        @Override
        public void trace(String format, Object o1, Object o2) {
            if (logger.isLoggable(java.util.logging.Level.FINEST)) {
                logger.log(java.util.logging.Level.FINEST, MessageFormat.format(format, o1, o2));
            }
        }

        @Override
        public void info(String format, Object o1, Object o2) {
            if (logger.isLoggable(java.util.logging.Level.INFO)) {
                logger.log(java.util.logging.Level.INFO, MessageFormat.format(format, o1, o2));
            }
        }

        @Override
        public void warn(String msg) {
            logger.log(java.util.logging.Level.WARNING, msg);
        }

        @Override
        public void error(String message, Throwable t) {
            logger.log(java.util.logging.Level.SEVERE, message, t);
        }

        @Override
        public void error(String format, Object o1, Throwable t) {
            if (logger.isLoggable(java.util.logging.Level.SEVERE)) {
                logger.log(java.util.logging.Level.SEVERE, MessageFormat.format(format, o1), t);
            }
        }

        @Override
        public void error(String format, Object o1, Object o2, Throwable t) {
            if (logger.isLoggable(java.util.logging.Level.SEVERE)) {
                logger.log(java.util.logging.Level.SEVERE, MessageFormat.format(format, o1, o2), t);
            }
        }
    }

    private static class SLF4JLogger implements Logger {
        final org.slf4j.Logger logger;

        SLF4JLogger(Class<?> hostClass) {
            logger = org.slf4j.LoggerFactory.getLogger(hostClass);
        }

        @Override
        public boolean isTraceEnabled() {
            return logger.isTraceEnabled();
        }

        @Override
        public void trace(String format, Object o1, Object o2) {
            logger.trace(format, o1, o2);
        }

        @Override
        public void info(String format, Object o1, Object o2) {
            logger.info(format, o1, o2);
        }

        @Override
        public void warn(String msg) {
            logger.warn(msg);
        }

        @Override
        public void error(String message, Throwable t) {
            logger.error(message, t);
        }

        @Override
        public void error(String format, Object o1, Throwable t) {
            logger.error(format, o1, t);
        }

        @Override
        public void error(String format, Object o1, Object o2, Throwable t) {
            logger.error(format, o1, o2, t);
        }
    }
}
