/**
 * $Id$
 */
package com.untangle.node.ips;

import java.util.Set;

import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.NodeUDPSession;
import com.untangle.uvm.vnet.event.IPDataEvent;

public class IpsSessionInfo
{
    private final NodeSession session;
    private final IpsNodeImpl ips;

    private Set<IpsRuleSignature> c2sSignatures;
    private Set<IpsRuleSignature> s2cSignatures;
    private IPDataEvent event;
    private String uriPath;
    private boolean isServer;

    // ContentOption variables
    public int start;
    public int end;
    public int indexOfLastMatch;

    public IpsSessionInfo(IpsNodeImpl ips, NodeSession session,
                          Set<IpsRuleSignature> c2sSignatures,
                          Set<IpsRuleSignature> s2cSignatures)
    {
        this.session = session;
        this.ips = ips;
        this.c2sSignatures = c2sSignatures;
        this.s2cSignatures = s2cSignatures;
    }

    public void setUriPath(String path)
    {
        uriPath = path;
    }

    public String  getUriPath()
    {
        return uriPath;
    }

    // Do i need to set sessionion data? I dont think so.. Check
    // later.
    public NodeSession getSession()
    {
        return session;
    }

    public void setEvent(IPDataEvent event)
    {
        this.event = event;
    }

    public IPDataEvent getEvent()
    {
        return event;
    }

    public void setFlow(boolean isServer)
    {
        this.isServer = isServer;
    }

    public boolean isServer()
    {
        return isServer;
    }

    public boolean processC2SSignatures()
    {
        for(IpsRuleSignature sig : c2sSignatures) {
            if (sig.execute(ips, this)) {
                return true;
            }
        }
        return false;
    }

    public boolean processS2CSignatures()
    {
        for(IpsRuleSignature sig : s2cSignatures) {
            if (sig.execute(ips, this)) {
                return true;
            }
        }
        return false;
    }

    // For debugging/login
    public int numC2SSignatures() { return c2sSignatures.size(); }
    public int numS2CSignatures() { return s2cSignatures.size(); }

    public void blockSession()
    {
        if(session instanceof NodeTCPSession) {
            ((NodeTCPSession)session).resetClient();
            ((NodeTCPSession)session).resetServer();
        }
        else if(session instanceof NodeUDPSession) {
            ((NodeUDPSession)session).expireClient();
            ((NodeUDPSession)session).expireServer();
        }
        session.release();
    }
}
