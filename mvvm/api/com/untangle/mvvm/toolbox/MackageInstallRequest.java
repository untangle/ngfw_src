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

public class MackageInstallRequest extends ToolboxMessage
{
    private final String mackageName;

    public MackageInstallRequest(String mackageName)
    {
        this.mackageName = mackageName;
    }

    public String getMackageName()
    {
        return mackageName;
    }

    // ToolboxMessage methods -------------------------------------------------

    public void accept(ToolboxMessageVisitor v)
    {
        v.visitMackageInstallRequest(this);
    }
}
