/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.List;
import java.util.Collection;
import java.util.HashMap;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.NetcapSession;
import com.untangle.jnetcap.NetcapTCPSession;
import com.untangle.jnetcap.NetcapUDPSession;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.app.SessionTuple;

/**
 * This stores the global system-wide state for a given session
 */
public class SessionGlobalState
{
    private final Logger logger = Logger.getLogger(getClass());

    protected final NetcapSession netcapSession;

    protected final long id;
    protected final short protocol;
    protected final long creationTime;
    
    protected final SideListener clientSideListener;
    protected final SideListener serverSideListener;

    protected final NetcapHook netcapHook;

    protected String user; 
    protected SessionEvent sessionEvent = null;
    protected SessionTuple clientSideTuple = null;
    protected SessionTuple serverSideTuple = null;
    protected int clientIntf = 0;
    protected int serverIntf = 0;
    protected long endTime = 0;
    protected long lastUpdateBytes = 0;

    protected HostTableEntry hostEntry = null;
    protected DeviceTableEntry deviceEntry = null;
    protected UserTableEntry userEntry = null;
    
    private HashMap<String,Tag> tags = new HashMap<String,Tag>();
    
    /**
     * This is the global list of attachments for this session
     * It is used by various parts of the platform and apps to store metadata about the session
     */
    protected HashMap<String,Object> stringAttachments;

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
        creationTime = System.currentTimeMillis();
        protocol = netcapSession.getProtocol();
        user = null;

        this.clientSideListener = clientSideListener;
        this.serverSideListener = serverSideListener;

        this.stringAttachments = new HashMap<String,Object>();
    }

    public long id()
    {
        return id;
    }
    
    public short getProtocol()
    {
        return protocol;
    }

    public long getCreationTime()
    {
        return this.creationTime;
    }

    public long getEndTime() { return this.endTime; }
    public void setEndTime( long newValue ) { this.endTime = newValue; }

    public long getLastUpdateBytes() { return this.lastUpdateBytes; }
    public void setLastUpdateBytes( long newValue ) { this.lastUpdateBytes = newValue; }
    
    public String user() { return this.user; }
    public void setUser( String newValue ) { this.user = newValue; }

    public SessionEvent getSessionEvent() { return this.sessionEvent; }
    public void setSessionEvent( SessionEvent newValue ) { this.sessionEvent = newValue; }

    public SessionTuple getClientSideTuple() { return this.clientSideTuple; }
    public void setClientSideTuple( SessionTuple newValue ) { this.clientSideTuple = newValue; }

    public SessionTuple getServerSideTuple() { return this.serverSideTuple; }
    public void setServerSideTuple( SessionTuple newValue ) { this.serverSideTuple = newValue; }

    public int getClientIntf() { return this.clientIntf; }
    public void setClientIntf( int newValue ) { this.clientIntf = newValue; }

    public int getServerIntf() { return this.serverIntf; }
    public void setServerIntf( int newValue ) { this.serverIntf = newValue; }

    public HostTableEntry getHostEntry() { return this.hostEntry; }
    public void setHostEntry( HostTableEntry newValue ) { this.hostEntry = newValue; }

    public DeviceTableEntry getDeviceEntry() { return this.deviceEntry; }
    public void setDeviceEntry( DeviceTableEntry newValue ) { this.deviceEntry = newValue; }

    public UserTableEntry getUserEntry() { return this.userEntry; }
    public void setUserEntry( UserTableEntry newValue ) { this.userEntry = newValue; }
    
    public List<Tag> getTags()
    {
        removeExpiredTags();
        return new LinkedList<Tag>(this.tags.values());
    }

    public String getTagsString()
    {
        return Tag.tagsToString( getTags() );
    }

    public void addTag( Tag tag )
    {
        if ( tag == null || tag.getName() == null )
            return;
        this.tags.put( tag.getName(), tag );
    }

    public void addTags( Collection<Tag> tags )
    {
        if ( tags == null )
            return;
        for ( Tag tag : tags ) {
            addTag( tag );
        }
    }

    public boolean hasTag( String name )
    {
        Tag t = this.tags.get( name );
        if ( t == null )
            return false;
        if ( t.isExpired() ) {
            this.tags.remove( t.getName() );
            return false;
        }
        return true;
    }

    public void removeExpiredTags()
    {
        for ( Iterator<Tag> i = this.tags.values().iterator() ; i.hasNext() ; ) {
            Tag t = i.next();
            if ( t.isExpired() )
                i.remove();
        }
    }
    
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

    public String getPipelineDescription()
    {
        if ( originalAgents == null )
            return "null";

        String pipelineDescription = "";
        boolean first = true;
        for ( PipelineConnectorImpl connector: originalAgents ) {
            pipelineDescription += (first ? "" : "," ) + connector.getName();
            first = false;
        }

        return pipelineDescription;
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

    public String toString()
    {
        return (sessionEvent == null ? "null" : sessionEvent.toString());
    }
}
