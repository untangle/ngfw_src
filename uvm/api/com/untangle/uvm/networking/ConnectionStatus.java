/*
 * $HeadURL: svn://chef.metaloft.com/work/src/uvm/impl/com/untangle/uvm/engine/ConnectivityTesterImpl.java $
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

package com.untangle.uvm.networking;

import java.io.Serializable;

import com.untangle.uvm.RemoteConnectivityTester;

@SuppressWarnings("serial")
public class ConnectionStatus implements RemoteConnectivityTester.Status, Serializable
{        
    private final boolean isDnsWorking;
    private final boolean isTcpWorking;

    public static final ConnectionStatus DNS_AND_TCP = new ConnectionStatus( true, true );
    public static final ConnectionStatus DNS         = new ConnectionStatus( true, false );
    public static final ConnectionStatus TCP         = new ConnectionStatus( false, true );
    public static final ConnectionStatus NOTHING     = new ConnectionStatus( false, false );
    
    private ConnectionStatus( boolean isDnsWorking, boolean isTcpWorking )
    {
        this.isDnsWorking = isDnsWorking;
        this.isTcpWorking = isTcpWorking;
    }
    
    public boolean isTcpWorking()
    {
        return this.isTcpWorking;
    }
    
    public boolean isDnsWorking()
    {
        return this.isDnsWorking;
    }

    public static ConnectionStatus makeConnectionStatus( boolean isDnsWorking, boolean isTcpWorking )
    {
        if ( isDnsWorking && isTcpWorking ) {
            return DNS_AND_TCP;
        } else if ( isDnsWorking ) {
            return DNS;
        } else if ( isTcpWorking ) {
            return TCP;
        }
        return NOTHING;
    }
}
