package org.sqlite;

@SuppressWarnings("serial")
public class NativeLibraryNotFoundException extends Exception {
    public NativeLibraryNotFoundException(String message) {
        super(message);
    }
}
