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
        return getRemoteSettings().getIsOutsideAdministrationEnabled();
    }

    protected String outsideErrorMessage()
    {
        return "Off-site Administration is disabled.";
    }

    protected String httpErrorMessage()
    {
        return "Standard Administration is disabled.";
    }
}