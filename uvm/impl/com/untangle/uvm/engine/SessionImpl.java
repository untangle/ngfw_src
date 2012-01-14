/**
 * $Id$
 */
package com.untangle.uvm.engine;

import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.vnet.Session;
import com.untangle.uvm.argon.ArgonSession;

import org.apache.log4j.Logger;

/**
 * Abstract base class for all live sessions
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
abstract class SessionImpl implements Session
{
    private final Logger logger = Logger.getLogger(SessionImpl.class);
    
    // For when we use a two-element array to store state for both sides.
    protected static final int CLIENT = 0;
    protected static final int SERVER = 1;

    protected ArgonConnectorImpl argonConnector;

    /**
     * The argon session that corresponds to this (node) Session.
     */
    protected ArgonSession argonSession;

    protected volatile Object attachment = null;

    protected SessionImpl(ArgonConnectorImpl argonConnector, ArgonSession argonSession)
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
    
    /**
     * Number of bytes received from the client.
     */
    public long c2tBytes()
    {
        return this.argonSession.c2tBytes();
    }

    /**
     * Number of bytes transmitted to the server.
     */
    public long t2sBytes()
    {
        return this.argonSession.t2sBytes();
    }

    /**
     * Number of bytes received from the server.
     */
    public long s2tBytes()
    {
        return this.argonSession.s2tBytes();
    }

    /**
     * Number of bytes transmitted to the client.
     */
    public long t2cBytes()
    {
        return this.argonSession.t2cBytes();
    }

    /**
     * Number of chunks received from the client.
     */
    public long c2tChunks()
    {
        return this.argonSession.c2tChunks();
    }

    /**
     * Number of chunks transmitted to the server.
     */
    public long t2sChunks()
    {
        return this.argonSession.t2sChunks();
    }

    /**
     * Number of chunks received from the server.
     */
    public long s2tChunks()
    {
        return this.argonSession.s2tChunks();
    }

    /**
     * Number of chunks transmitted to the client.
     */
    public long t2cChunks()
    {
        return this.argonSession.t2cChunks();
    }

}
