package com.untangle.uvm;

import java.util.Map;

public interface SettingsManager
{
    /**
     * Load the settings from the store using a unique identifier.
     * 
     * @param <T>
     *            Type of class to load
     * @param clz
     *            Type of class to load.
     * @param packageName
     *            Name of the debian package that is making the request.
     * @param id
     *            Unique identifier to select the object.
     * @return The object that was loaded or null if an object was not loaded.
     * @throws SettingsException
     */
    public <T> T load(Class<T> clz, String packageName, String key) throws SettingsException;

    /**
     * Save the settings from the store using a unique identifier.
     * 
     * @param <T>
     *            Type of class to save
     * @param clz
     *            Type of class to save.
     * @param packageName
     *            Name of the debian package that is making the request.
     * @param id
     *            Unique identifier to select the object.
     * @param value
     *            The value to be saved.
     * @return The object that was saved.
     * @throws SettingsException
     */
    public <T> T save(Class<T> clz, String packageName, String key, T value) throws SettingsException;

@SuppressWarnings("serial")
    public static class SettingsException extends Exception {

        public SettingsException(String message) {
            super(message);
        }

        public SettingsException(String message, Throwable cause) {
            super(message, cause);
        }

        public SettingsException(Throwable cause) {
            super(cause);
        }
    }
}
