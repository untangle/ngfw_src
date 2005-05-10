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
import java.net.InetAddress;

public class LoginSession implements Serializable
{
    private static final long serialVersionUID = 3121773301570017319L;

    private final MvvmPrincipal mvvmPrincipal;
    private final int sessionId;
    private final InetAddress clientAddr;

    public LoginSession(MvvmPrincipal mp, int sessionId,
                        InetAddress clientAddr)
    {
        this.mvvmPrincipal =  mp;
        this.sessionId = sessionId;
        this.clientAddr = clientAddr;
    }

    public MvvmPrincipal getMvvmPrincipal()
    {
        return mvvmPrincipal;
    }

    public int getSessionId()
    {
        return sessionId;
    }

    public InetAddress getClientAddr()
    {
        return clientAddr;
    }

    // Object methods ---------------------------------------------------------

    @Override
    public String toString()
    {
        return (null == mvvmPrincipal ? "nobody" : mvvmPrincipal.getName())
            + " " + sessionId;
    }

    @Override
    public int hashCode()
    {
        int result = 17;
        result = 37 * result
            + (null == mvvmPrincipal ? 0 : mvvmPrincipal.hashCode());
        result = 37 * result + sessionId;
        result = 37 * result
            + (null == clientAddr ? 0 : clientAddr.hashCode());

        return result;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof LoginSession)) {
            return false;
        }

        LoginSession ls = (LoginSession)o;

        return null == mvvmPrincipal ? null == ls.mvvmPrincipal
            : mvvmPrincipal.equals(ls.mvvmPrincipal)
            && sessionId == ls.sessionId
            && null == clientAddr ? null == ls.clientAddr
            : clientAddr.equals(ls.clientAddr);
    }
}
