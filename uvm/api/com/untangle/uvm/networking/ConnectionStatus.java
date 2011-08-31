/*
 * $Id: ConnectionStatus.java,v 1.00 2011/08/31 15:35:50 dmorris Exp $
 */
package com.untangle.uvm.networking;

import java.io.Serializable;

import com.untangle.uvm.ConnectivityTester;

@SuppressWarnings("serial")
public class ConnectionStatus implements ConnectivityTester.Status, Serializable
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
