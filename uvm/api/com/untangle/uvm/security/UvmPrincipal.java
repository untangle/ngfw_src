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

package com.untangle.uvm.security;

import java.io.Serializable;
import java.security.Principal;

public final class UvmPrincipal implements Principal, Serializable
{
    private static final long serialVersionUID = -3343175380349082197L;

    private String loginName;
    private boolean readOnly;

    public UvmPrincipal(String loginName, boolean readOnly)
    {
        this.loginName = loginName;
        this.readOnly = readOnly;
    }

    public String getName()
    {
        return loginName;
    }

    public boolean isReadOnly()
    {
        return readOnly;
    }

    public int hashCode()
    {
        return loginName.hashCode();
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof UvmPrincipal)) {
            return false;
        }
        UvmPrincipal mp = (UvmPrincipal)o;
        return loginName.equals(mp.loginName);
    }
}
