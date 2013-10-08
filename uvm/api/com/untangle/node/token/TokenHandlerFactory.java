/**
 * $Id: TokenHandlerFactory.java 31921 2012-05-12 02:44:47Z dmorris $
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
