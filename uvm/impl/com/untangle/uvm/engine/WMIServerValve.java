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

package com.untangle.uvm.engine;

import com.untangle.uvm.util.OutsideValve;

class WMIServerValve extends OutsideValve
{
    WMIServerValve()
    {
    }

    protected boolean isOutsideAccessAllowed()
    {
        return false;
    }

    protected String outsideErrorMessage()
    {
        return "off-site access to the WMI Installer";
    }

    protected String httpErrorMessage()
    {
        return "standard access to the WMI Installer";
    }
}
