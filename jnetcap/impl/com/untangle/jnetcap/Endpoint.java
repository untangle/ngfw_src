/**
 * $Id: Endpoint.java 35102 2013-06-20 18:52:32Z dmorris $
 */
package com.untangle.jnetcap;

import java.net.InetAddress;

public interface Endpoint
{
    /**
     * Retrieve the host for this endpoint
     */
    public InetAddress host();

    /**
     * Retrieve the port for the endpoint.
     */
    public int port();
}
