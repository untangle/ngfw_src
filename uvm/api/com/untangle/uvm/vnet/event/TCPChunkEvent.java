/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.vnet.TCPSession;

/**
 * TCP data event -- chunk of bytes received.
 */
@SuppressWarnings("serial")
public class TCPChunkEvent extends TCPSessionEvent implements IPDataEvent
{
    private ByteBuffer readBuffer;

    public TCPChunkEvent( ArgonConnector argonConnector, TCPSession session, ByteBuffer readBuffer )
    {
        super(argonConnector, session);
        this.readBuffer = readBuffer;
    }

    public ByteBuffer chunk()
    {
        return readBuffer;
    }

    public ByteBuffer data()
    {
        return readBuffer;
    }
}
