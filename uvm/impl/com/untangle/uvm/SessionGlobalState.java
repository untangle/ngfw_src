/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.List;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.NetcapSession;
import com.untangle.jnetcap.NetcapTCPSession;
import com.untangle.jnetcap.NetcapUDPSession;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.app.SessionTuple;
import com.untangle.uvm.vnet.SessionAttachments;

/**
 * This stores the global system-wide state for a given session
 */
public class SessionGlobalState implements SessionAttachments
{
    private final Logger logger = Logger.getLogger(getClass());
    private static final String NO_KEY_VALUE = "NOKEY";

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
    
    private HashMap<String,Tag> tags = new HashMap<>();
    
    /**
     * This is the global list of attachments for this session
     * It is used by various parts of the platform and apps to store metadata about the session
     */
    protected HashMap<String,Object> stringAttachments;

    /**
     * Stores a list of the original agents/pipelinespecs processing this session
     * Note: Even if a app/agent releases a session it will still be in this list
     * This is used for resetting sessions with killMatchingSessions so we can only reset
     * sessions that were originally processed by the app calling killMatchingSessions
     */
    private List<PipelineConnectorImpl> originalAgents;
    
    /**
     * SessionGlobalState constructor
     * @param netcapSession
     * @param clientSideListener
     * @param serverSideListener
     * @param netcapHook
     */
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

        this.stringAttachments = new HashMap<>();
    }

    /**
     * id
     * @return id
     */
    public long id()
    {
        return id;
    }

    /**
     * getProtocol
     * @return protocol
     */
    public short getProtocol()
    {
        return protocol;
    }

    /**
     * getCreationTime
     * @return creationTime
     */
    public long getCreationTime()
    {
        return this.creationTime;
    }

    /**
     * getEndTime
     * @return endTime
     */
    public long getEndTime()
    {
        return this.endTime;
    }

    /**
     * setEndTime
     * @param newValue
     */
    public void setEndTime( long newValue )
    {
        this.endTime = newValue;
    }

    /**
     * getLastUpdateBytes
     * @return lastUpdateBytes
     */
    public long getLastUpdateBytes()
    {
        return this.lastUpdateBytes;
    }

    /**
     * setLastUpdateBytes
     * @param newValue
     */
    public void setLastUpdateBytes( long newValue )
    {
        this.lastUpdateBytes = newValue;
    }

    /**
     * user
     * @return user
     */
    public String user()
    {
        return this.user;
    }

    /**
     * setUser
     * @param newValue
     */
    public void setUser( String newValue )
    {
        this.user = newValue;
    }

    /**
     * getSessionEvent
     * @return sessionEvent
     */
    public SessionEvent getSessionEvent()
    {
        return this.sessionEvent;
    }

    /**
     * setSessionEvent
     * @param newValue
     */
    public void setSessionEvent( SessionEvent newValue )
    {
        this.sessionEvent = newValue;
    }

    /**
     * getClientSideTuple
     * @return clientSideTuple
     */
    public SessionTuple getClientSideTuple()
    {
        return this.clientSideTuple;
    }

    /**
     * setClientSideTuple
     * @param newValue
     */
    public void setClientSideTuple( SessionTuple newValue )
    {
        this.clientSideTuple = newValue;
    }

    /**
     * getServerSideTuple
     * @return serverSideTuple
     */
    public SessionTuple getServerSideTuple()
    {
        return this.serverSideTuple;
    }

    /**
     * setServerSideTuple
     * @param newValue
     */
    public void setServerSideTuple( SessionTuple newValue )
    {
        this.serverSideTuple = newValue;
    }

    /**
     * getClientIntf
     * @return clientIntf
     */
    public int getClientIntf()
    {
        return this.clientIntf;
    }

    /**
     * setClientIntf
     * @param newValue
     */
    public void setClientIntf( int newValue )
    {
        this.clientIntf = newValue;
    }

    /**
     * getServerIntf
     * @return serverIntf
     */
    public int getServerIntf()
    {
        return this.serverIntf;
    }

    /**
     * setServerIntf
     * @param newValue
     */
    public void setServerIntf( int newValue )
    {
        this.serverIntf = newValue;
    }

    /**
     * getHostEntry
     * @return hostEntry
     */
    public HostTableEntry getHostEntry()
    {
        return this.hostEntry;
    }

    /**
     * setHostEntry
     * @param newValue
     */
    public void setHostEntry( HostTableEntry newValue )
    {
        this.hostEntry = newValue;
    }


    /**
     * getDeviceEntry
     * @return deviceEntry
     */
    public DeviceTableEntry getDeviceEntry()
    {
        return this.deviceEntry;
    }

    /**
     * setDeviceEntry
     * @param newValue
     */
    public void setDeviceEntry( DeviceTableEntry newValue )
    {
        this.deviceEntry = newValue;
    }

    /**
     * getUserEntry
     * @return userEntry
     */
    public UserTableEntry getUserEntry()
    {
        return this.userEntry;
    }

    /**
     * setUserEntry
     * @param newValue
     */
    public void setUserEntry( UserTableEntry newValue )
    {
        this.userEntry = newValue;
    }
    
    /**
     * getTags returns a list of tags in this SessionGlobalState
     * @return list
     */
    public List<Tag> getTags()
    {
        removeExpiredTags();
        return new LinkedList<>(this.tags.values());
    }

    /**
     * getTagsString returns the list of tags as a string
     * Each tag name is concatenated with a comma
     * It does not include the expirations times
     * Example: "foo,bar"
     * @return String
     */
    public String getTagsString()
    {
        return Tag.tagsToString( getTags() );
    }

    /**
     * addTag adds a tag to the tags on this SessionGlobalState
     * @param tag
     */
    public void addTag( Tag tag )
    {
        if ( tag == null || tag.getName() == null )
            return;
        this.tags.put( tag.getName(), tag );
    }

    /**
     * addTags adds a list of tags to the tags on this SessionGlobalState
     * @param tags
     */
    public void addTags( Collection<Tag> tags )
    {
        if ( tags == null )
            return;
        for ( Tag tag : tags ) {
            addTag( tag );
        }
    }

    /**
     * hasTag checks if the tag is in the list of tags (and not expired)
     * @param name of tag
     * @return true if found, false otherwise
     */
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

    /**
     * removeExpiredTags removes any expired tags from the list of tags
     */
    public void removeExpiredTags()
    {
        for ( Iterator<Tag> i = this.tags.values().iterator() ; i.hasNext() ; ) {
            Tag t = i.next();
            if ( t.isExpired() )
                i.remove();
        }
    }
    
    /**
     * Get the NetcapSession for this Session
     * @return NetcapSession
     */
    public NetcapSession netcapSession()
    {
        return netcapSession;
    }

    /**
     * Retrieve the netcap TCP Session.  If this is not a TCP session,
     * this will throw an exception.
     * @return NetcapTCPSession
     */
    public NetcapTCPSession netcapTCPSession()
    {
        return (NetcapTCPSession)netcapSession;
    }

    /**
     * Retrieve the netcap UDP Session.  If this is not a UDP session,
     * this will throw an exception.
     * @return NetcapUDPSession
     */
    public NetcapUDPSession netcapUDPSession()
    {
        return (NetcapUDPSession)netcapSession;
    }

    /**
     * clientSideListener returns the client side listener
     * @return SideListener
     */
    public SideListener clientSideListener()
    {
        return clientSideListener;
    }

    /**
     * serverSideListener returns the server side listener
     * @return SideListener
     */
    public SideListener serverSideListener()
    {
        return serverSideListener;
    }

    /**
     * getPipelineConnectors returns the list of original PipelineConnectors
     * processing this session
     * @return list
     */
    public List<PipelineConnectorImpl> getPipelineConnectors()
    {
        return originalAgents;
    }

    /**
     * getPipelineDescription builds a string description of the original pipeline
     * @return string
     */
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
    
    /**
     * set the list of original PipelineConnectors processing this session
     * @param agents
     */
    public void setPipelineConnectorImpls( List<PipelineConnectorImpl> agents )
    {
        this.originalAgents = agents;
    }

    /**
     * netcapHook returns the netcapHook for this session
     * @return netcapHook
     */
    public NetcapHook netcapHook()
    {
        return netcapHook;
    }

    /**
     * Attach an object with the specified key
     * @param key
     * @param attachment
     * @return the previous attachment for this key
     */
    public Object attach(String key, Object attachment)
    {
        logger.debug("globalAttach( " + key + " , " + attachment + " )");
        return this.stringAttachments.put(key,attachment);
    }

    /**
     * Get an attachment for the specified key
     * @param key
     * @return the obj (or null)
     */
    public Object attachment(String key)
    {
        return this.stringAttachments.get(key);
    }

    /**
     * getAttachments returns all attachments
     * @return map of the string to objects
     */
    public Map<String,Object> getAttachments()
    {
        return this.stringAttachments;
    }


    // The following methods satisfy the SessionAttachments interface.
    /**
     * Get an attachment for the specified key
     * @param key
     * @return the obj (or null)
     */
    public Object globalAttachment(String key)
    {
        return attachment(key);
    }

    /**
     * Attach an object with the specified key
     * @param key
     * @param attachment
     * @return the previous attachment for this key
     */
    public Object globalAttach(String key, Object attachment)
    {
        return this.attach(key,attachment);
    }

    /**
     * Attach an unnamed object to the session
     *
     * @param ob
     *        The object
     * @return The object
     */
    public Object attach(Object ob)
    {
        return attach(NO_KEY_VALUE, ob);
    }

    /**
     * Get the unnamed object attached to the session
     *
     * @return The object
     */
    public Object attachment()
    {
        return attachment(NO_KEY_VALUE);
    }

    /**
     * toString
     * @return <doc>
     */
    public String toString()
    {
        return (sessionEvent == null ? "null" : sessionEvent.toString());
    }
}
