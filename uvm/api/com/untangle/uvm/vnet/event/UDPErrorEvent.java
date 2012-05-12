/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import java.net.InetAddress;
import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.IPPacketHeader;
import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.vnet.NodeUDPSession;

/**
 * The class <code>UDPErrorEvent</code> is for events from incoming ICMP messages that are
 * associated with a given UDP session.
 */
@SuppressWarnings("serial")
public class UDPErrorEvent extends UDPPacketEvent
{
    private byte icmpType;
    private byte icmpCode;
    private InetAddress icmpSource;
    
    public UDPErrorEvent(ArgonConnector argonConnector, NodeUDPSession src, ByteBuffer icmpData, IPPacketHeader header, byte icmpType, byte icmpCode, InetAddress icmpSource)
    {
        super(argonConnector, src, icmpData, header);
        this.icmpType   = icmpType;
        this.icmpCode   = icmpCode;
        this.icmpSource = icmpSource;
    }

    public byte getErrorType()
    {
        return icmpType;
    }

    public byte getErrorCode()
    {
        return icmpCode;
    }
    
    public InetAddress getErrorSource()
    {
        return icmpSource;
    }
}
