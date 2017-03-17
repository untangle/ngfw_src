/**
 * $Id$
 */
package com.untangle.uvm.vnet;

/**
 * Base interface for IP Streamers
 */
public interface IPStreamer
{
    /**
     * <code>closeWhenDone</code> is called after EOF is reached.  If it returns true,
     * then a FIN is sent at the end, beginning the closing process.  If it returns false,
     * no FIN is sent and the app resumes normal event handling.
     *
     * XXX -- we need an event to let them know they've returned from streaming mode.
     *
     * @return a <code>boolean</code> true if a FIN should be sent at the end of the stream.
     */
    boolean closeWhenDone();
}
