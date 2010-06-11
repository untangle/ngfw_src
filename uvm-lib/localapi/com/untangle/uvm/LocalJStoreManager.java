package com.untangle.uvm;

import java.util.Map;

public interface LocalJStoreManager
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
     * @throws TransactionException
     */
    public <T> T load(Class<T> clz, String packageName)
            throws TransactionException;

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
     * @throws TransactionException
     */
    public <T> T load(Class<T> clz, String packageName, String id)
            throws TransactionException;

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
     * @throws TransactionException
     */
    public <T> T load(Class<T> clz, String packageName, String k, String v)
            throws TransactionException;

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
     * @throws TransactionException
     */
    public <T> T load(Class<T> clz, String packageName,
            Map<String, String> criteria) throws TransactionException;

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
     * @throws TransactionException
     */
    public <T> T save(Class<T> clz, String packageName, T value)
            throws TransactionException;

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
     * @throws TransactionException
     */
    public <T> T save(Class<T> clz, String packageName, String id, T value)
            throws TransactionException;

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
     * @throws TransactionException
     */
    public <T> T save(Class<T> clz, String packageName, String k, String v,
            T value) throws TransactionException;

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
     * @throws TransactionException
     */
    public <T> T save(Class<T> clz, String packageName,
            Map<String, String> criteria, T value) throws TransactionException; 

@SuppressWarnings("serial")
    public static class TransactionException extends Exception {

        public TransactionException(String message) {
            super(message);
        }

        public TransactionException(String message, Throwable cause) {
            super(message, cause);
        }

        public TransactionException(Throwable cause) {
            super(cause);
        }
    }
}
