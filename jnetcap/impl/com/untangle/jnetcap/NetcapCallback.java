/**
 * $Id: NetcapCallback.java 34444 2013-04-01 23:17:13Z dmorris $
 */
package com.untangle.jnetcap;

public interface NetcapCallback
{
    /* This the callback that will be called for the UDP/TCP hooks */
    public void event( long sessionId );
}
