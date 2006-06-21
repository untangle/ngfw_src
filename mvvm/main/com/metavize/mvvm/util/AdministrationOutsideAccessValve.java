/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.mvvm.util;

public class AdministrationOutsideAccessValve extends OutsideValve
{
    public void AdministrationOutsideAccessValve()
    {
    }

    protected boolean isOutsideAccessAllowed()
    {
        return getRemoteSettings().getIsOutsideAdministrationEnabled();
    }

    protected String errorMessage()
    {
        return "Off-site Administration is disabled.";
    }
}