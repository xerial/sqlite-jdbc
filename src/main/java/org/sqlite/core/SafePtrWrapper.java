package org.sqlite.core;

import java.sql.SQLException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A class for safely wrapping calls to a native pointer, ensuring no other thread has access to the pointer while it is run
 */
public class SafePtrWrapper {
    private final String ptrName;
    private final long ptr;
    private final SafePtrCloseFunction closeCallback;
    private final Lock lock = new ReentrantLock();
    private volatile boolean closed = false;
    // to return on subsequent calls to close after this ptr has been closed
    private int closedRC;
    // to throw on subsequent calls to close after this ptr has been close if the close function threw an exception
    private SQLException closeException;

    /**
     * Construct a new Safe Pointer Wrapper to ensure a pointer is properly handled
     *
     * @param ptrName       the name of the pointer, only used for better exception messages
     * @param ptr           the raw pointer
     * @param closeCallback the callback function to free this native pointer when it is closed.
     *                      This is guaranteed to be called at most once
     */
    public SafePtrWrapper(String ptrName, long ptr, SafePtrCloseFunction closeCallback) {
        this.ptrName = ptrName;
        this.ptr = ptr;
        this.closeCallback = closeCallback;
    }

    /**
     * Check whether this pointer has been closed
     *
     * @return whether this pointer has been closed
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Close this pointer
     *
     * @return the return code of the close callback function
     * @throws SQLException if the close callback throws an SQLException, or the pointer is locked elsewhere
     */
    public int close() throws SQLException {
        lock.lock();
        try {
            // if this is already closed, return or throw the previous result
            if (closed) {
                if (closeException != null)
                    throw closeException;
                return closedRC;
            }
            closedRC = closeCallback.run(this, ptr);
            return closedRC;
        } catch (SQLException ex) {
            this.closeException = ex;
            throw ex;
        } finally {
            this.closed = true;
            lock.unlock();
        }
    }

    /**
     * Run a callback with the wrapped pointer safely. Note: this call will fail if the pointer is being used elsewhere
     *
     * @param run the function to run
     * @return the return code of the function
     * @throws SQLException if the pointer is utilized elsewhere
     */
    public <E extends Throwable> int safeRunInt(SafePtrIntFunction<E> run) throws SQLException, E {
        this.safeTryLock();
        try {
            return run.run(ptr);
        } finally {
            this.unlock();
        }
    }

    /**
     * Run a callback with the wrapped pointer safely. Note: this call will fail if the pointer is being used elsewhere
     *
     * @param run the function to run
     * @return the return code of the function
     * @throws SQLException if the pointer is utilized elsewhere
     */
    public <T, E extends Throwable> T safeRun(SafePtrFunction<T, E> run) throws SQLException, E {
        this.safeTryLock();
        try {
            return run.run(ptr);
        } finally {
            this.unlock();
        }
    }

    /**
     * Run a callback with the wrapped pointer safely. Note: this call will fail if the pointer is being used elsewhere
     *
     * @param run the function to run
     * @throws SQLException if the pointer is utilized elsewhere
     */
    public <E extends Throwable> void safeRunConsume(SafePtrConsumer<E> run) throws SQLException, E {
        this.safeTryLock();
        try {
            run.run(ptr);
        } finally {
            this.unlock();
        }
    }


    /**
     * Lock this pointer so no other thread can lock it. Useful if several atomic calls must be made in a row.
     *
     * @throws SQLException if another thread is using this
     */
    private void safeTryLock() throws SQLException {
        if (!lock.tryLock()) {
            throw new SQLException(ptrName + " ptr is locked elsewhere");
        }
        if (this.closed) {
            throw new SQLException(ptrName + " is closed");
        }
    }

    private void unlock() {
        lock.unlock();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SafePtrWrapper that = (SafePtrWrapper) o;
        return ptr == that.ptr;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(ptr);
    }

    @FunctionalInterface
    public interface SafePtrIntFunction<E extends Throwable> {
        int run(long ptr) throws E;
    }

    @FunctionalInterface
    public interface SafePtrCloseFunction {
        int run(SafePtrWrapper safePtr, long ptr) throws SQLException;
    }

    @FunctionalInterface
    public interface SafePtrFunction<T, E extends Throwable> {
        T run(long ptr) throws E;
    }

    @FunctionalInterface
    public interface SafePtrConsumer<E extends Throwable> {
        void run(long ptr) throws E;
    }
}
