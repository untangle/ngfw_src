/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.argon;

import com.untangle.jnetcap.NetcapSession;
import com.untangle.jnetcap.NetcapTCPSession;
import com.untangle.jnetcap.NetcapUDPSession;

import org.apache.log4j.Logger;

import java.util.HashMap;

/**
 * This stores the global system-wide state for a given session
 */
public class SessionGlobalState
{
    private final Logger logger = Logger.getLogger(getClass());

    protected final NetcapSession netcapSession;

    protected final int id;
    protected final short protocol;
    protected String user; // Not final since we can set it later

    protected final SideListener clientSideListener;
    protected final SideListener serverSideListener;

    protected final ArgonHook argonHook;

    protected HashMap<String,Object> attachments;
        
    SessionGlobalState( NetcapSession netcapSession, SideListener clientSideListener, SideListener serverSideListener, ArgonHook argonHook )
    {
        this.argonHook = argonHook;

        this.netcapSession = netcapSession;

        id = netcapSession.id();
        protocol = netcapSession.protocol();
        user = null;

        this.clientSideListener = clientSideListener;
        this.serverSideListener = serverSideListener;

        this.attachments = new HashMap<String,Object>();
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

    public Object attach(String key, Object attachment)
    {
        logger.warn("globalAttach( " + key + " , " + attachment + " )");
        return this.attachments.put(key,attachment);
    }

    public Object attachment(String key)
    {
        return this.attachments.get(key);
    }
}
