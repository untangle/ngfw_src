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
     * @param fileName
     *            The fileName of the file
     * @return The object that was loaded or null if an object was not loaded.
     * @throws SettingsException
     */
    public <T> T load(Class<T> clz, String fileName) throws SettingsException;

    /**
     * Save the settings from the store using a unique identifier.
     * 
     * @param <T>
     *            Type of class to save
     * @param clz
     *            Type of class to save.
     * @param fileName
     *            The filename to save the class to
     * @param value
     *            The value to be saved.
     * @return The object that was saved.
     * @throws SettingsException
     */
    public <T> T save(Class<T> clz, String fileName, T value) throws SettingsException;

    /**
     * Save the settings from the store using a unique identifier.
     * 
     * @param <T>
     *            Type of class to save
     * @param clz
     *            Type of class to save.
     * @param fileName
     *            The filename to save the class to
     * @param value
     *            The value to be saved.
     * @param saveVersion
     *            True if older versions should be saved.
     * @return The object that was saved.
     * @throws SettingsException
     */
    public <T> T save(Class<T> clz, String fileName, T value, boolean saveVersion) throws SettingsException;

    
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
    public static class SettingsException extends Exception
    {
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
