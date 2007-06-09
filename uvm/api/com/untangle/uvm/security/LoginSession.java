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

package com.untangle.mvvm.security;

import java.io.Serializable;
import java.net.InetAddress;

public class LoginSession implements Serializable
{
    private static final long serialVersionUID = -585571141103354067L;

    public enum LoginType { INTERACTIVE, SYSTEM };

    private final MvvmPrincipal mvvmPrincipal;
    private final int sessionId;
    private final InetAddress clientAddr;
    private final LoginType loginType;

    public LoginSession(MvvmPrincipal mp, int sessionId,
                        InetAddress clientAddr, LoginType loginType)
    {
        this.mvvmPrincipal =  mp;
        this.sessionId = sessionId;
        this.clientAddr = clientAddr;
        this.loginType = loginType;
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

    public LoginType getLoginType()
    {
        return loginType;
    }

    public boolean isInteractive()
    {
        return LoginType.INTERACTIVE == loginType;
    }

    public boolean isSystem()
    {
        return LoginType.SYSTEM == loginType;
    }

    // Object methods ---------------------------------------------------------

    @Override
    public String toString()
    {
        return (null == mvvmPrincipal ? "nobody" : mvvmPrincipal.getName())
            + " " + sessionId + " login type: " + loginType
            + " clientAddr: " + clientAddr;
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
            : clientAddr.equals(ls.clientAddr)
            && loginType == ls.loginType;
    }
}
