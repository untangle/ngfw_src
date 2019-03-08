/**
 * $Id
 */
package com.untangle.uvm;

/**
 * Settings manager.
 * Documentation is in SettingsManagerImpl
 */
public interface SettingsManager
{
    /**
     * See SettingsManagerImpl
     */
    public <T> T load( Class<T> clz, String fileName ) throws SettingsException;

    /**
     * See SettingsManagerImpl
     */
    public void save( String fileName, Object value ) throws SettingsException;

    /**
     * See SettingsManagerImpl
     */
    public void save( String fileName, Object value, boolean saveVersion ) throws SettingsException;

    /**
     * See SettingsManagerImpl
     */
    public void save( String fileName, Object value, boolean saveVersion, boolean prettyFormat ) throws SettingsException;
    
    /**
     * See SettingsManagerImpl
     */
    public void save( String fileName, String inputFileName, boolean saveVersion) throws SettingsException;
        
    /**
     * See SettingsManagerImpl
     */
    public <T> T loadUrl(Class<T> clz, String urlStr) throws SettingsException;
    
    /**
     * See SettingsManagerImpl
     */
    public String getDiff(String fileName) throws SettingsException;

    /**
     * Settings exception
     */
    @SuppressWarnings("serial")
    public static class SettingsException extends Exception
    {
        /**
         * Initialize instance of SettingSException.
         * @param  message String of message.
         * @return         Instance of SettingsException.
         */
        public SettingsException(String message) {
            super(message);
        }

        /**
         * Initialize instance of SettingSException.
         * @param  message String of message.
         * @param  cause Trowable of cause.
         * @return         Instance of SettingsException.
         */
        public SettingsException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Initialize instance of SettingSException.
         * @param cause Trowable of cause.
         * @return         Instance of SettingsException.
         */
        public SettingsException(Throwable cause) {
            super(cause);
        }
    }

}
