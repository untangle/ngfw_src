/**
 * $Id: DownloadProgress.java 34784 2013-05-28 02:18:40Z dmorris $
 */
package com.untangle.uvm.apt;

import com.untangle.uvm.message.Message;

/**
 * Update on the status of a download.
 */
@SuppressWarnings("serial")
public class DownloadProgress extends Message
{

    private final String name;
    private final int bytesDownloaded;
    private final int size;
    private final String speed;
    private final PackageDesc requestingPackage;

    public DownloadProgress(String name, int bytesDownloaded, int size, String speed, PackageDesc requestingPackage)
    {
        this.name = name;
        this.bytesDownloaded = bytesDownloaded;
        this.size = size;
        this.speed = speed;
        this.requestingPackage = requestingPackage;
    }

    // accessors --------------------------------------------------------------

    public String getName()
    {
        return name;
    }

    public int getBytesDownloaded()
    {
        return bytesDownloaded;
    }

    public int getSize()
    {
        return size;
    }

    public String getSpeed()
    {
        return speed;
    }

    public PackageDesc getRequestingPackage()
    {
        return requestingPackage;
    }

    public boolean isUpgrade()
    {
        return null == requestingPackage;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "DownloadProgress name: " + name
            + " bytesDownloaded: " + bytesDownloaded + " size: " + size
            + " speed: " + speed;
    }
}
