/**
 * $Id$
 */
package com.untangle.jnetcap;

public interface NetcapCallback
{
    /* This is the callback that will be called for the UDP/TCP hooks */
    public void event( long sessionId );

    /* This is the callback for conntrack events */
    public void event( long conntrackPtr, int type );
}
