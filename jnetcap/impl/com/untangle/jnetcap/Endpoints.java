/**
 * $Id$
 */
package com.untangle.jnetcap;

public interface Endpoints
{
    public Endpoint client();

    public Endpoint server();

    /**
     * Retrieve a unique interface identifier, or 0 if the interface is unknown
     */
    public int interfaceId();
}
