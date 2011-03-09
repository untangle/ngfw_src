/* $HeadURL$ */
package com.untangle.jnetcap;

public interface NetcapHook
{
    /* This the callback that will be called for the UDP/TCP hooks */
    public void event( int sessionId );
}
