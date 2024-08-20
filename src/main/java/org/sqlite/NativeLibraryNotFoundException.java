package org.sqlite;

public class NativeLibraryNotFoundException extends Exception {
    public NativeLibraryNotFoundException(String message) {
        super(message);
    }

    public NativeLibraryNotFoundException(Throwable cause) {
        super(cause);
    }

    public NativeLibraryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
