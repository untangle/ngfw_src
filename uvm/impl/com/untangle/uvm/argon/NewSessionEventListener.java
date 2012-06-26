/**
 * $Id$
 */
package com.untangle.uvm.argon;

public interface NewSessionEventListener
{
    /**
     * A new UDP session event event.  This function converts a request into a session.</p>
     *
     * @param request - A UDP NodeSession request.
     */
    public ArgonUDPSession newSession( ArgonUDPNewSessionRequest request );

    /**
     * A new TCP session event event.  This function converts a request into a session.</p>
     *
     * @param request - A TCP NodeSession request.
     */
    public ArgonTCPSession newSession( ArgonTCPNewSessionRequest request );
}
