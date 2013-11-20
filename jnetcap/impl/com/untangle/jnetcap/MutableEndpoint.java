/**
 * $Id$
 */
package com.untangle.jnetcap;

import java.net.InetAddress;

public interface MutableEndpoint extends Endpoint
{
    public void host( InetAddress newHost );
    public void port( int newPort );
}
