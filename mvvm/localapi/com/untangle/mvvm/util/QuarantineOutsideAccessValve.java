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
package com.untangle.mvvm.util;

public class QuarantineOutsideAccessValve extends OutsideValve
{
    public void QuarantineOutsideAccessValve()
    {
    }

    protected boolean isOutsideAccessAllowed()
    {
        return getRemoteSettings().getIsOutsideQuarantineEnabled();
    }

    protected String errorMessage()
    {
        return "Off-site access to quarantine is disabled.";
    }
}