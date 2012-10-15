/**
 * $Id$
 */
package com.untangle.node.http;

import com.untangle.node.token.Casing;
import com.untangle.node.token.CasingFactory;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Creates an HTTP casing.
 */
class HttpCasingFactory implements CasingFactory
{
    private final HttpNodeImpl node;

    public HttpCasingFactory(HttpNodeImpl node)
    {
        this.node = node;
    }

    public Casing casing(NodeTCPSession session, boolean clientSide)
    {
        return new HttpCasing(session, clientSide, node);
    }
}
