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

package com.metavize.mvvm.portal;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.tran.IPaddr;

public abstract class PortalEvent extends LogEvent
{
    // constructors -----------------------------------------------------------

    protected PortalEvent() { }

    // abstract methods -------------------------------------------------------

    public abstract IPaddr getClientAddr();
    public abstract String getUid();

}
