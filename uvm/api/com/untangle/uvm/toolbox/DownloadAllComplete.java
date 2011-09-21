/*
 * $Id$
 */
package com.untangle.uvm.toolbox;

import com.untangle.uvm.message.Message;

/**
 * Signals that all downloads are complete.
 */
@SuppressWarnings("serial")
public class DownloadAllComplete extends Message
{
    private final boolean success;
    private final PackageDesc requestingPackage;

    public DownloadAllComplete(boolean success, PackageDesc requestingPackage)
    {
        this.success = success;
        this.requestingPackage = requestingPackage;
    }

    public boolean getSuccess()
    {
        return success;
    }

    public PackageDesc getRequestingPackage()
    {
        return requestingPackage;
    }

    public boolean isUpgrade()
    {
        return null == requestingPackage;
    }
}
