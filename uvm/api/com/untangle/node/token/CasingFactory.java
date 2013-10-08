/**
 * $Id: CasingFactory.java 31921 2012-05-12 02:44:47Z dmorris $
 */
package com.untangle.node.token;

import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Creates casing instance to handle a session.
 */
public interface CasingFactory
{
    /**
     * Creates a casing.
     *
     * @param session the casing's session.
     * @param clientSide true if casing will be on the side nearest to
     * the client.
     * @return a new casing for this session.
     */
    Casing casing(NodeTCPSession session, boolean clientSide);
}
