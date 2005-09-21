/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.security;

import java.io.Serializable;
import java.security.Principal;

public final class MvvmPrincipal implements Principal, Serializable
{
    private static final long serialVersionUID = -3343175380349082197L;

    private String loginName;

    public MvvmPrincipal(String loginName)
    {
        this.loginName = loginName;
    }

    public String getName()
    {
        return loginName;
    }

    public int hashCode()
    {
        return loginName.hashCode();
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof MvvmPrincipal)) {
            return false;
        }
        MvvmPrincipal mp = (MvvmPrincipal)o;
        return loginName.equals(mp.loginName);
    }
}
