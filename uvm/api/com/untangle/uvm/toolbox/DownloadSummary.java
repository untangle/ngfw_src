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

package com.untangle.uvm.toolbox;

import java.io.Serializable;

public class DownloadSummary implements InstallProgress, Serializable
{
    // XXX serial UID

    private final int count;
    private final int size;

    public DownloadSummary(int count, int size)
    {
        this.count = count;
        this.size = size;
    }

    public int getCount()
    {
        return count;
    }

    public int getSize()
    {
        return size;
    }

    // InstallProgress methods ------------------------------------------------

    public void accept(ProgressVisitor visitor)
    {
        visitor.visitDownloadSummary(this);
    }
}
