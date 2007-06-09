/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.toolbox;

import java.io.Serializable;

public class DownloadProgress implements InstallProgress, Serializable
{
    // XXX serial UID

    private final String name;
    private final int bytesDownloaded;
    private final int size;
    private final String speed;

    public DownloadProgress(String name, int bytesDownloaded, int size,
                            String speed)
    {
        this.name = name;
        this.bytesDownloaded = bytesDownloaded;
        this.size = size;
        this.speed = speed;
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

    // InstallProgress methods ------------------------------------------------

    public void accept(ProgressVisitor visitor)
    {
        visitor.visitDownloadProgress(this);
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "DownloadProgress name: " + name
            + " bytesDownloaded: " + bytesDownloaded + " size: " + size
            + " speed: " + speed;
    }
}
