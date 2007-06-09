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
import java.net.InetAddress;

public class LoginSession implements Serializable
{
    private static final long serialVersionUID = -585571141103354067L;

    public enum LoginType { INTERACTIVE, SYSTEM };

    private final UvmPrincipal uvmPrincipal;
    private final int sessionId;
    private final InetAddress clientAddr;
    private final LoginType loginType;

    public LoginSession(UvmPrincipal mp, int sessionId,
                        InetAddress clientAddr, LoginType loginType)
    {
        this.uvmPrincipal =  mp;
        this.sessionId = sessionId;
        this.clientAddr = clientAddr;
        this.loginType = loginType;
    }

    public UvmPrincipal getUvmPrincipal()
    {
        return uvmPrincipal;
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
        return (null == uvmPrincipal ? "nobody" : uvmPrincipal.getName())
            + " " + sessionId + " login type: " + loginType
            + " clientAddr: " + clientAddr;
    }

    @Override
    public int hashCode()
    {
        int result = 17;
        result = 37 * result
            + (null == uvmPrincipal ? 0 : uvmPrincipal.hashCode());
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

        return null == uvmPrincipal ? null == ls.uvmPrincipal
            : uvmPrincipal.equals(ls.uvmPrincipal)
            && sessionId == ls.sessionId
            && null == clientAddr ? null == ls.clientAddr
            : clientAddr.equals(ls.clientAddr)
            && loginType == ls.loginType;
    }
}
