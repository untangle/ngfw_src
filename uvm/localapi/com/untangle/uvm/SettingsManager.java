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
     * @param dirName
     *            Name of the debian package that is making the request.
     * @param id
     *            Unique identifier to select the object.
     * @return The object that was loaded or null if an object was not loaded.
     * @throws SettingsException
     */
    public <T> T load(Class<T> clz, String dirName, String key) throws SettingsException;

    /**
     * Save the settings from the store using a unique identifier.
     * 
     * @param <T>
     *            Type of class to save
     * @param clz
     *            Type of class to save.
     * @param dirName
     *            Name of the debian package that is making the request.
     * @param id
     *            Unique identifier to select the object.
     * @param value
     *            The value to be saved.
     * @return The object that was saved.
     * @throws SettingsException
     */
    public <T> T save(Class<T> clz, String dirName, String key, T value) throws SettingsException;

    /**
     * Load the settings from the store using a unique identifier.
     * 
     * @param <T>
     *            Type of class to load
     * @param clz
     *            Type of class to load.
     * @param basePath
     *            The path of the settings file (/usr/share/untangle/settings/)
     * @param dirName
     *            Name of the debian package that is making the request.
     * @param id
     *            Unique identifier to select the object.
     * @return The object that was loaded or null if an object was not loaded.
     * @throws SettingsException
     */
    public <T> T loadBasePath(Class<T> clz, String basePath, String dirName, String id) throws SettingsException;

    /**
     * Save the settings from the store using a unique identifier.
     * 
     * @param <T>
     *            Type of class to save
     * @param clz
     *            Type of class to save.
     * @param basePath
     *            The path of the settings file (example: "/usr/share/untangle/settings/")
     * @param dirName
     *            Name of the debian package that is making the request. (example: "untangle-node-foo")
     * @param id
     *            Unique identifier to select the object.
     * @param value
     *            The value to be saved.
     * @return The object that was saved.
     * @throws SettingsException
     */
    public <T> T saveBasePath(Class<T> clz, String basePath, String dirName, String id, T value) throws SettingsException;

    /**
     * Load the settings from the store using a unique identifier.
     * 
     * @param <T>
     *            Type of class to load
     * @param clz
     *            Type of class to load.
     * @param URL
     *            The URL to load the settings for
     * @return The object that was loaded or null if an object was not loaded.
     * @throws SettingsException
     */
    public <T> T loadUrl(Class<T> clz, String urlStr) throws SettingsException;
    
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
