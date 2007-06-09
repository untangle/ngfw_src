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
package com.untangle.mvvm.util;

public class AdministrationOutsideAccessValve extends OutsideValve
{
    public void AdministrationOutsideAccessValve()
    {
    }

    protected boolean isOutsideAccessAllowed()
    {
        return getAccessSettings().getIsOutsideAdministrationEnabled();
    }

    protected String outsideErrorMessage()
    {
        return "off-site administration";
    }

    protected String httpErrorMessage()
    {
        return "standard administration";
    }
}
