/**
 * $Id$
 */
package com.untangle.node.virus;

import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Factory for FTP <code>TokenHandler</code>s.
 *
 */
public class VirusFtpFactory implements TokenHandlerFactory
{
    private final VirusNodeImpl node;

    VirusFtpFactory(VirusNodeImpl node)
    {
        this.node = node;
    }

    public TokenHandler tokenHandler(NodeTCPSession session)
    {
        return new VirusFtpHandler(session, node);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
