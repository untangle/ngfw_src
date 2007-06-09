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

package com.untangle.mvvm.portal;

import com.untangle.mvvm.logging.LogEvent;
import com.untangle.mvvm.tran.IPaddr;

public abstract class PortalEvent extends LogEvent
{
    // constructors -----------------------------------------------------------

    protected PortalEvent() { }

    // abstract methods -------------------------------------------------------

    public abstract IPaddr getClientAddr();
    public abstract String getUid();

}
