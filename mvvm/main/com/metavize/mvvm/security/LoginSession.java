/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: LoginSession.java,v 1.2 2005/02/25 02:45:29 amread Exp $
 */

package com.metavize.mvvm.security;

import java.io.Serializable;

public class LoginSession implements Serializable
{
    private static final long serialVersionUID = 3121773301570017319L;

    private MvvmPrincipal mvvmPrincipal;
    private int sessionId;

    public LoginSession(MvvmPrincipal mp, int sessionId)
    {
        this.mvvmPrincipal =  mp;
        this.sessionId = sessionId;
    }

    public MvvmPrincipal mvvmPrincipal()
    {
        return mvvmPrincipal;
    }

    public int sessionId()
    {
        return sessionId;
    }

    public String toString()
    {
        return (null == mvvmPrincipal ? "nobody" : mvvmPrincipal.getName())
            + " " + sessionId();
    }

    public int hashCode()
    {
        int result = 17;
        result = 37 * result
            + (null == mvvmPrincipal ? 0 : mvvmPrincipal.hashCode());
        result = 37 * result + sessionId;

        return result;
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof LoginSession)) {
            return false;
        }

        LoginSession ls = (LoginSession)o;

        return null == mvvmPrincipal ? null == ls.mvvmPrincipal
            : mvvmPrincipal.equals(ls.mvvmPrincipal)
            && sessionId == ls.sessionId;

    }
}
