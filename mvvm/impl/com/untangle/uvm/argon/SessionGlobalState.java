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

package com.untangle.mvvm.argon;

import com.untangle.jnetcap.NetcapSession;
import com.untangle.jnetcap.NetcapTCPSession;
import com.untangle.jnetcap.NetcapUDPSession;

public class SessionGlobalState
{

    protected final NetcapSession netcapSession;

    protected final int id;
    protected final short protocol;
    protected String user;      // Not final since we can set it later

    protected final SideListener clientSideListener;
    protected final SideListener serverSideListener;

    protected final ArgonHook argonHook;


    SessionGlobalState( NetcapSession netcapSession, SideListener clientSideListener, 
                        SideListener serverSideListener, ArgonHook argonHook )
    {
        this.argonHook = argonHook;

        this.netcapSession = netcapSession;

        id = netcapSession.id();
        protocol = netcapSession.protocol();
        user = null;

        this.clientSideListener = clientSideListener;
        this.serverSideListener = serverSideListener;
    }

    public int id()
    {
        return id;
    }
    
    public short protocol()
    {
        return protocol;
    }

    public String user()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public NetcapSession netcapSession()
    {
        return netcapSession;
    }

    /**
     * Retrieve the netcap TCP Session.  If this is not a TCP session, this will throw an exception.
     */
    public NetcapTCPSession netcapTCPSession()
    {
        return (NetcapTCPSession)netcapSession;
    }

    /**
     * Retrieve the netcap UDP Session.  If this is not a UDP session, this will throw an exception.
     * XXX Probably better executed with two subclasses.
     */
    public NetcapUDPSession netcapUDPSession()
    {
        return (NetcapUDPSession)netcapSession;
    }

    public SideListener clientSideListener()
    {
        return clientSideListener;
    }

    public SideListener serverSideListener()
    {
        return serverSideListener;
    }

    public ArgonHook argonHook()
    {
        return argonHook;
    }
}
