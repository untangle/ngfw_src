/**
 * $Id$
 */
package com.untangle.app.virus_blocker;

import java.io.File;

/**
 * Represents a virus scanner client context
 */
public final class VirusClientContext
{
    private InputSettings iSettings;

    private volatile VirusScannerResult virusReport;

    /**
     * Constructor
     * 
     * @param msgFile
     *        The message file
     * @param host
     *        The host
     * @param port
     *        The port
     */
    public VirusClientContext(File msgFile, String host, int port)
    {
        iSettings = new InputSettings(msgFile, host, port);
        virusReport = null;
    }

    /**
     * Gets the message file
     * 
     * @return The message file
     */
    public File getMsgFile()
    {
        return iSettings.getMsgFile();
    }

    /**
     * Gets the host
     * 
     * @return The host
     */
    public String getHost()
    {
        return iSettings.getHost();
    }

    /**
     * Gets the port
     * 
     * @return The port
     */
    public int getPort()
    {
        return iSettings.getPort();
    }

    /**
     * Sets ERROR in the result
     */
    public void setResultError()
    {
        this.virusReport = VirusScannerResult.ERROR;
        return;
    }

    /**
     * Sets the argumented status in the result
     * 
     * @param clean
     *        Clean flag
     * @param virusName
     *        The name of the virus
     */
    public void setResult(boolean clean, String virusName)
    {
        if (true == clean) {
            this.virusReport = VirusScannerResult.CLEAN;
        } else {
            this.virusReport = new VirusScannerResult(clean, virusName);
        }
        return;
    }

    /**
     * Gets the virus scanner result
     * 
     * @return
     */
    public VirusScannerResult getResult()
    {
        return virusReport;
    }

    /**
     * Class to store input settings
     */
    class InputSettings
    {
        private final File msgFile;
        private final String host;
        private final int port;

        /**
         * Constructor
         * 
         * @param msgFile
         *        The message file
         * @param host
         *        The host
         * @param port
         *        The port
         */
        public InputSettings(File msgFile, String host, int port)
        {
            this.msgFile = msgFile;
            this.host = host;
            this.port = port;
        }

        /**
         * Gets the message file
         * 
         * @return The message file
         */
        public File getMsgFile()
        {
            return msgFile;
        }

        /**
         * Gets the host
         * 
         * @return The host
         */
        public String getHost()
        {
            return host;
        }

        /**
         * Gets the port
         * 
         * @return The port
         */
        public int getPort()
        {
            return port;
        }
    }
}
