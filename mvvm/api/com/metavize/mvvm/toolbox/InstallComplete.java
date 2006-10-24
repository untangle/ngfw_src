/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.toolbox;

import java.io.Serializable;

public class InstallComplete implements InstallProgress, Serializable
{
    // XXX serial UID

    private final boolean success;

    public InstallComplete(boolean success)
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
        visitor.visitInstallComplete(this);
    }
}
