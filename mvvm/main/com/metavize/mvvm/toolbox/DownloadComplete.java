/*
 * Copyright (c) 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.toolbox;

import java.io.Serializable;

public class DownloadComplete implements InstallProgress, Serializable
{
    // XXX serial UID

    private final boolean success;

    public DownloadComplete(boolean success)
    {
        this.success = success;
    }

    // accessors --------------------------------------------------------------

    public boolean getSuccess()
    {
        return success;
    }

    // InstallProgress methods ------------------------------------------------

    public void accept(ProgressVisitor visitor)
    {
        visitor.visitDownloadComplete(this);
    }
}
