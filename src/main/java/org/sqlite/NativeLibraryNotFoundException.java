package org.sqlite;

public class NativeLibraryNotFoundException extends Exception {
    public NativeLibraryNotFoundException(String message) {
        super(message);
    }
}
