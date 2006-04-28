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

package com.metavize.mvvm.portal;

import java.io.Serializable;

/**
 * Portal login key.  Used so the servlet looks up the PortalLogin through
 * the portal manager every time, since it can get logged out (removed).
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 */
public class PortalLoginKey implements Serializable
{
    private static final long serialVersionUID = -2816114706496389437L;

    private String desc;

    // constructors -----------------------------------------------------------
    // Not for user consumption, only used by PortalManagerImpl

    public PortalLoginKey(String desc)
    {
        this.desc = desc;
    }

    public String toString()
    {
        return "PortalLoginKey " + desc;
    }
}
