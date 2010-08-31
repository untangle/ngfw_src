package com.untangle.uvm;

import java.util.Map;

public interface SettingsManager
{
    /**
     * Load the settings from the store for a singleton.
     * 
     * @param <T>
     *            Type of class to load
     * @param clz
     *            Type of class to load.
     * @param packageName
     *            Name of the debian package that is making the request.
     * @return The object that was loaded or null if an object was not loaded.
     * @throws SettingsException
     */
    public <T> T load(Class<T> clz, String packageName) throws SettingsException;

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
    public <T> T load(Class<T> clz, String packageName, String id) throws SettingsException;

    /**
     * Load the settings from the store using a unique identifier.
     * 
     * @param <T>
     *            Type of class to load
     * @param clz
     *            Type of class to load.
     * @param packageName
     *            Name of the debian package that is making the request.
     * @param k
     *            Name of the key for v.
     * @param v
     *            Value for the key k.
     * @return The object that was loaded or null if an object was not loaded.
     * @throws SettingsException
     */
    public <T> T load(Class<T> clz, String packageName, String k, String v) throws SettingsException;

    /**
     * Load the settings from the store using a unique identifier.
     * 
     * @param <T>
     *            Type of class to load
     * @param clz
     *            Type of class to load.
     * @param packageName
     *            Name of the debian package that is making the request.
     * @param criteria
     *            Map of key value pairs to select on.
     * @return The object that was loaded or null if an object was not loaded.
     * @throws SettingsException
     */
    public <T> T load(Class<T> clz, String packageName, Map<String, String> criteria) throws SettingsException;

    /**
     * Save the settings from the store for a singleton.
     * 
     * @param <T>
     *            Type of class to load
     * @param clz
     *            Type of class to load.
     * @param packageName
     *            Name of the debian package that is making the request.
     * @param value
     *            The value to be saved.
     * @return The object that was saved.
     * @throws SettingsException
     */
    public <T> T save(Class<T> clz, String packageName, T value) throws SettingsException;

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
    public <T> T save(Class<T> clz, String packageName, String id, T value) throws SettingsException;

    /**
     * Save the settings from the store using a unique identifier.
     * 
     * @param <T>
     *            Type of class to save
     * @param clz
     *            Type of class to save.
     * @param packageName
     *            Name of the debian package that is making the request.
     * @param k
     *            Name of the key for v.
     * @param v
     *            Value for the key k.
     * @param value
     *            The value to be saved.
     * @return The object that was saved.
     * @throws SettingsException
     */
    public <T> T save(Class<T> clz, String packageName, String k, String v, T value) throws SettingsException;

    /**
     * Save the settings from the store using a unique identifier.
     * 
     * @param <T>
     *            Type of class to save
     * @param clz
     *            Type of class to save.
     * @param packageName
     *            Name of the debian package that is making the request.
     * @param criteria
     *            Map of key value pairs to select on.
     * @param value
     *            The value to be saved.
     * @return The object that was saved.
     * @throws SettingsException
     */
    public <T> T save(Class<T> clz, String packageName, Map<String, String> criteria, T value) throws SettingsException; 

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
