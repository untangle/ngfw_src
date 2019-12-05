/**
 * $Id$
 */
package com.untangle.app.http;

import java.io.Serializable;

/**
 * Holds information about redirects.
 */
@SuppressWarnings("serial")
public class BlockDetails extends RedirectDetails implements Serializable
{

    /**
     * Create a RedirectDetails instance for the following host and URI
     * @param host - the host (being blocked)
     * @param uri - the URI (being blocked)
     */
    public BlockDetails(String host, String uri)
    {
        super(host, uri);
    }

}
