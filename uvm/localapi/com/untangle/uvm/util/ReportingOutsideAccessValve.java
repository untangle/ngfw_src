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
package com.untangle.uvm.util;

public class ReportingOutsideAccessValve extends OutsideValve
{
    public ReportingOutsideAccessValve()
    {
    }

    protected boolean isOutsideAccessAllowed()
    {
        return getAccessSettings().getIsOutsideReportingEnabled();
    }

    protected String outsideErrorMessage()
    {
        return "off-site access to reporting";
    }

    protected String httpErrorMessage()
    {
        return "standard access to reporting";
    }
}
