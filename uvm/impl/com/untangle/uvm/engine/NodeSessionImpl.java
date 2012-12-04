/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.util.Map;

import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.argon.ArgonSession;
import com.untangle.uvm.node.SessionEvent;

import org.apache.log4j.Logger;

/**
 * Abstract base class for all live sessions
 */
abstract class NodeSessionImpl implements NodeSession
{
    private final Logger logger = Logger.getLogger(NodeSessionImpl.class);
    
    protected ArgonConnectorImpl argonConnector;

    protected final SessionEvent sessionEvent;

    /**
     * The argon session that corresponds to this (node) Session.
     */
    protected ArgonSession argonSession;

    protected volatile Object attachment = null;

    protected NodeSessionImpl(ArgonConnectorImpl argonConnector, ArgonSession argonSession, SessionEvent sessionEvent)
    {
        this.argonConnector = argonConnector;
        this.argonSession = argonSession;
        this.sessionEvent = sessionEvent;
    }

    public ArgonConnector argonConnector()
    {
        return argonConnector;
    }

    public long id()
    {
        return argonSession.id();
    }

    public long getSessionId()
    {
        return argonSession.id();
    }
    
    public String user()
    {
        return argonSession.user();
    }

    public Object attach(Object ob)
    {
        Object oldOb = attachment;
        attachment = ob;
        return oldOb;
    }

    public Object attachment()
    {
        return attachment;
    }

    public Object globalAttach(String key, Object ob)
    {
        return this.argonSession.sessionGlobalState().attach(key,ob);
    }

    public Object globalAttachment(String key)
    {
        return this.argonSession.sessionGlobalState().attachment(key);
    }

    public Map<String,Object> getAttachments()
    {
        return this.argonSession.sessionGlobalState().getAttachments();
    }
    
    public void killSession()
    {
        if (this.argonSession == null)
            return;

        this.argonSession.killSession();
    }

}
