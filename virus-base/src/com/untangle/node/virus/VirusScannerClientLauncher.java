/**
 * $Id$
 */
package com.untangle.node.virus;

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
