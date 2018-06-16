/**
 * $Id$
 */

package com.untangle.app.virus_blocker;

import java.io.File;

/**
 * Virus Scanner Client Launcher
 */
abstract public class VirusScannerClientLauncher
{
    protected File msgFile;

    /**
     * Create a ClientLauncher for the given file
     * 
     * @param msgFile
     *        The file
     */
    public VirusScannerClientLauncher(File msgFile)
    {
        this.msgFile = msgFile;
    }

    /**
     * Do the scan
     * 
     * @param timeout
     *        The timeout
     * @return Virus scanner result
     */
    abstract public VirusScannerResult doScan(long timeout);
}
