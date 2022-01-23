package org.sqlite.core;

/** This is a helper class for exposing package local functions of NativeDB to unit tests */
public class NativeDBHelper {
    /**
     * Get the native pointer of the progress handler
     *
     * @param nativeDB the native db object
     * @return the pointer of the progress handler
     */
    public static long getProgressHandler(DB nativeDB) {
        return ((NativeDB) nativeDB).getProgressHandler();
    }

    /**
     * Get the native pointer of the busy handler
     *
     * @param nativeDB the native db object
     * @return the pointer of the busy handler
     */
    public static long getBusyHandler(DB nativeDB) {
        return ((NativeDB) nativeDB).getBusyHandler();
    }

    /**
     * Get the native pointer of the commit listener
     *
     * @param nativeDB the native db object
     * @return the pointer of the commit listener
     */
    public static long getCommitListener(DB nativeDB) {
        return ((NativeDB) nativeDB).getCommitListener();
    }

    /**
     * Get the native pointer of the update listener
     *
     * @param nativeDB the native db object
     * @return the pointer of the update listener
     */
    public static long getUpdateListener(DB nativeDB) {
        return ((NativeDB) nativeDB).getUpdateListener();
    }
}
