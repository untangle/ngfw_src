/**
 * $Id$
 */
package com.untangle.uvm.engine;

import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.argon.ArgonSession;

import org.apache.log4j.Logger;

/**
 * Abstract base class for all live sessions
 */
abstract class NodeSessionImpl implements NodeSession
{
    private final Logger logger = Logger.getLogger(NodeSessionImpl.class);
    
    protected ArgonConnectorImpl argonConnector;

    /**
     * The argon session that corresponds to this (node) Session.
     */
    protected ArgonSession argonSession;

    protected volatile Object attachment = null;

    protected NodeSessionImpl(ArgonConnectorImpl argonConnector, ArgonSession argonSession)
    {
        this.argonConnector = argonConnector;
        this.argonSession = argonSession;
    }

    public ArgonConnector argonConnector()
    {
        return argonConnector;
    }

    public long id()
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
}
