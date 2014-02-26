/**
 * $Id$
 */
package com.untangle.node.token;

import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Creates a <code>TokenHandler</code> for a session.
 */
public interface TokenHandlerFactory
{
    void handleNewSessionRequest(TCPNewSessionRequest tsr);
    TokenHandler tokenHandler(NodeTCPSession s);
}
