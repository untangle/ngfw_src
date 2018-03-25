package com.untangle.uvm;

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
    public <T> T load( Class<T> clz, String fileName ) throws SettingsException;

    /**
     * Save the settings from the store using a unique identifier.
     * 
     * @param <T>
     *            Type of class to save
     * @param fileName
     *            The filename to save the class to
     * @param value
     *            The value to be saved.
     * @return The object that was saved.
     * @throws SettingsException
     */
    public void save( String fileName, Object value ) throws SettingsException;

    /**
     * Save the settings from the store using a unique identifier.
     * 
     * @param <T>
     *            Type of class to save
     * @param fileName
     *            The filename to save the class to
     * @param value
     *            The value to be saved.
     * @param saveVersion
     *            True if older versions should be saved.
     * @return The object that was saved.
     * @throws SettingsException
     */
    public void save( String fileName, Object value, boolean saveVersion ) throws SettingsException;

    /**
     * Save the settings from the store using a unique identifier.
     * 
     * @param <T>
     *            Type of class to save
     * @param fileName
     *            The filename to save the class to
     * @param value
     *            The value to be saved.
     * @param saveVersion
     *            True if older versions should be saved.
     * @param prettyFormat
     *            True if the file should be pretty printed with indentation and whitespace
     * @return The object that was saved.
     * @throws SettingsException
     */
    public void save( String fileName, Object value, boolean saveVersion, boolean prettyFormat ) throws SettingsException;
    
    /**
     * Move the settings file from its input location and store using a unique identifier.
     * 
     * @param fileName
     *            The filename to save the class to
     * @param inputFilename
     *            The source filename to move
     * @param saveVersion
     *            True if older versions should be saved.
     * @return Nothing
     * @throws SettingsException
     */
    public void save( String fileName, String inputFileName, boolean saveVersion) throws SettingsException;
        
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
    
    /**
     * From the specified settings file, get the previous version and return a string diff
     *
     * @param fileName
     *      Filename to compare.  Expected to be in event log format settings_dir/name.js-version-js
     */
    public String getDiff(String fileName) throws SettingsException;

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
