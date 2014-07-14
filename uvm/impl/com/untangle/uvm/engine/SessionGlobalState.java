/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.NetcapSession;
import com.untangle.jnetcap.NetcapTCPSession;
import com.untangle.jnetcap.NetcapUDPSession;
import com.untangle.uvm.node.SessionEvent;

/**
 * This stores the global system-wide state for a given session
 */
public class SessionGlobalState
{
    private final Logger logger = Logger.getLogger(getClass());

    protected final NetcapSession netcapSession;

    protected final long id;
    protected final short protocol;

    protected final SideListener clientSideListener;
    protected final SideListener serverSideListener;

    protected final NetcapHook netcapHook;

    protected String user; 
    protected SessionEvent sessionEvent = null;

    /**
     * This is the global list of attachments for this session
     * It is used by various parts of the platform and apps to store metadata about the session
     */
    protected HashMap<String,Object> stringAttachments;
    protected HashMap<Long,Object> longAttachments;

    /**
     * Stores a list of the original agents/pipelinespecs processing this session
     * Note: Even if a node/agent releases a session it will still be in this list
     * This is used for resetting sessions with killMatchingSessions so we can only reset
     * sessions that were originally processed by the node calling killMatchingSessions
     */
    private List<PipelineConnectorImpl> originalAgents;
    
    SessionGlobalState( NetcapSession netcapSession, SideListener clientSideListener, SideListener serverSideListener, NetcapHook netcapHook )
    {
        this.netcapHook = netcapHook;

        this.netcapSession = netcapSession;

        id = netcapSession.id();
        protocol = netcapSession.getProtocol();
        user = null;

        this.clientSideListener = clientSideListener;
        this.serverSideListener = serverSideListener;

        this.stringAttachments = new HashMap<String,Object>();
        this.longAttachments = new HashMap<Long,Object>();
    }

    public long id()
    {
        return id;
    }
    
    public short getProtocol()
    {
        return protocol;
    }

    public String user() { return this.user; }
    public void setUser( String newValue ) { this.user = newValue; }

    public SessionEvent getSessionEvent() { return this.sessionEvent; }
    public void setSessionEvent( SessionEvent newValue ) { this.sessionEvent = newValue; }
    
    public NetcapSession netcapSession()
    {
        return netcapSession;
    }

    /**
     * Retrieve the netcap TCP Session.  If this is not a TCP session, this will throw an exception.
     */
    public NetcapTCPSession netcapTCPSession()
    {
        return (NetcapTCPSession)netcapSession;
    }

    /**
     * Retrieve the netcap UDP Session.  If this is not a UDP session, this will throw an exception.
     */
    public NetcapUDPSession netcapUDPSession()
    {
        return (NetcapUDPSession)netcapSession;
    }

    public SideListener clientSideListener()
    {
        return clientSideListener;
    }

    public SideListener serverSideListener()
    {
        return serverSideListener;
    }

    public List<PipelineConnectorImpl> getPipelineConnectors()
    {
        return originalAgents;
    }

    public void setPipelineConnectorImpls( List<PipelineConnectorImpl> agents )
    {
        this.originalAgents = agents;
    }

    public NetcapHook netcapHook()
    {
        return netcapHook;
    }

    public Object attach(String key, Object attachment)
    {
        logger.debug("globalAttach( " + key + " , " + attachment + " )");
        return this.stringAttachments.put(key,attachment);
    }

    public Object attachment(String key)
    {
        return this.stringAttachments.get(key);
    }

    public Map<String,Object> getAttachments()
    {
        return this.stringAttachments;
    }

    public Object attach(Long key, Object attachment)
    {
        logger.debug("globalAttach( " + key + " , " + attachment + " )");
        return this.longAttachments.put(key,attachment);
    }

    public Object attachment(Long key)
    {
        return this.longAttachments.get(key);
    }
}
