/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm;

import java.io.Serializable;

public class InstallComplete implements InstallProgress, Serializable
{
    // XXX serial UID

    public static final InstallComplete COMPLETE = new InstallComplete();

    private InstallComplete() { }

    public void accept(ProgressVisitor visitor)
    {
        visitor.visitInstallComplete(this);
    }
}
