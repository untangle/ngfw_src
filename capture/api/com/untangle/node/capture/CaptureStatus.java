/**
 * $Id: CaptureStatus.java,v 1.00 2011/12/14 01:02:03 mahotz Exp $
 */

package com.untangle.node.capture; // IMPL

import java.nio.ByteBuffer;
import com.untangle.uvm.vnet.NodeSession;

public class CaptureStatus
{
    ByteBuffer clientBuffer = null;
    String clientString = null;
    String serverString = null;
    String method = null;
    String hostname = null;
    String pagename = null;

    public CaptureStatus(NodeSession session)
    {
        clientString = (session.getClientAddr().getHostAddress() + ":" + session.getClientPort());
        serverString = (session.getServerAddr().getHostAddress() + ":" + session.getServerPort());
        clientBuffer = ByteBuffer.allocate(0x4000);
    }

    public String toString()
    {
        String string = new String();
        string = "CLIENT=" + clientString + " SERVER=" + serverString;
        return(string);
    }
}
