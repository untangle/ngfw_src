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

package com.untangle.mvvm.engine;

import com.untangle.mvvm.util.OutsideValve;

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
        return "Off-site access to the WMI Installer is prohibited.";
    }
    
    protected String httpErrorMessage()
    {
        return "Standard access to the WMI Installer is disabled.";
    }
}
