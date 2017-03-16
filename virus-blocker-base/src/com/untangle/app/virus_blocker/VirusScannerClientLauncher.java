/**
 * $Id$
 */
package com.untangle.app.virus_blocker;

import java.io.File;

abstract public class VirusScannerClientLauncher
{
    protected File msgFile;

    /**
     * Create a ClientLauncher for the given file
     */
    public VirusScannerClientLauncher(File msgFile)
    {
        this.msgFile = msgFile;
    }

    abstract public VirusScannerResult doScan(long timeout);
}
