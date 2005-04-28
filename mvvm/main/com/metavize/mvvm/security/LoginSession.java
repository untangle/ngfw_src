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

    // We provide the login session as a thread local for now, until we can integrate this with
    // Subject/Principal stuff that's builtin.
    private static InheritableThreadLocal<LoginSession> activeSession =
        new InheritableThreadLocal<LoginSession>();

    private MvvmPrincipal mvvmPrincipal;
    private int sessionId;
    private InetAddress clientAddr;

    public LoginSession(MvvmPrincipal mp, int sessionId, InetAddress clientAddr)
    {
        this.mvvmPrincipal =  mp;
        this.sessionId = sessionId;
        this.clientAddr = clientAddr;
    }

    public MvvmPrincipal mvvmPrincipal()
    {
        return mvvmPrincipal;
    }

    public int sessionId()
    {
        return sessionId;
    }

    public InetAddress clientAddr()
    {
        return clientAddr;
    }

    public void setActive()
    {
        activeSession.set(this);
    }

    public static LoginSession getActive()
    {   
        return activeSession.get();
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
