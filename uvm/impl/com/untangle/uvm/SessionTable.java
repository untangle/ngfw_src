/*
 * $Id$
 */
package com.untangle.uvm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.jvector.Vector;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.vnet.PipelineConnector;

/**
 * This table stores a global list of all currently active sessions being vectored
 */
public class SessionTable
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final SessionTable INSTANCE = new SessionTable();

    private final Map<Vector,SessionGlobalState> activeSessions = new HashMap<Vector,SessionGlobalState>();
    private final Map<NatPortAvailabilityKey,SessionGlobalState> tcpPortsUsed = new HashMap<NatPortAvailabilityKey,SessionGlobalState>();

    public static final short PROTO_TCP = 6;
    
    /* Singleton */
    private SessionTable() {}

    /**
     * Add a vector to the hash set.
     * @param  vector - The vector to add.
     * @return - True if the item did not already exist
     */
    protected synchronized boolean put( Vector vector, SessionGlobalState session )
    {
        boolean inserted = ( activeSessions.put( vector, session ) == null );

        if ( inserted && session.getProtocol() == PROTO_TCP ) {
            int port = session.netcapSession().serverSide().client().port();
            InetAddress addr = session.netcapSession().serverSide().client().host();
            NatPortAvailabilityKey key = new NatPortAvailabilityKey( addr, port );
            if ( tcpPortsUsed.get( key ) != null ) {
                logger.warn("Collision value in port availability map: " + addr.getHostAddress() + ":" + port);
                // just continue, not much can be done about it here.
            } else {
                tcpPortsUsed.put( key, session );
            }
        }
        
        return inserted;
    }

    /**
     * Remove a vector from the hash set.
     * @param  vector - The vector to remove.
     * @return - True if the item was removed, false if it wasn't in the set.
     */
    protected synchronized boolean remove( Vector vector )
    {
        SessionGlobalState session = activeSessions.get( vector );
        if ( session == null ) {
            return false;
        }

        boolean removed = ( activeSessions.remove( vector ) != null );
        
        if ( removed && session.getProtocol() == PROTO_TCP ) {
            int port = session.netcapSession().serverSide().client().port();
            InetAddress addr = session.netcapSession().serverSide().client().host();
            NatPortAvailabilityKey key = new NatPortAvailabilityKey( addr, port );
            if ( tcpPortsUsed.remove( key ) == null ) {
                logger.warn("Missing value in port availability map: " + addr.getHostAddress() + ":" + port );
            }
        }

        return removed;
    }

    /**
     * Get the number of vectors remaining
     */
    protected synchronized int count()
    {
        return activeSessions.size();
    }

    protected synchronized int count( short protocol )
    {
        int count = 0;
        
        for ( SessionGlobalState state : activeSessions.values() ) {
            if (state.getProtocol() == protocol)
                count++;
        }

        return count;
    }

    protected synchronized boolean isTcpPortUsed( InetAddress addr, int port )
    {
        NatPortAvailabilityKey key = new NatPortAvailabilityKey( addr, port );
        if ( tcpPortsUsed.get( key ) != null )
            return true;
        else
            return false;
    }
    
    /**
     * This kills all active vectors, since this is synchronized, it pauses the creation
     * of new vectoring machines, but it doesn't prevent the creating of new vectoring
     * machines
     * @return - Returns false if there are no active sessions. */
    public synchronized boolean shutdownActive()
    {
        if ( activeSessions.isEmpty()) return false;

        for ( Iterator<Vector> iter = activeSessions.keySet().iterator(); iter.hasNext() ; ) {
            Vector vector = iter.next();
            vector.shutdown();
            /* Don't actually remove the item, it is removed when the session exits */
        }

        return true;
    }

    public synchronized List<SessionGlobalState> getSessions()
    {
        return new LinkedList<SessionGlobalState>(this.activeSessions.values());
    }
    
    protected void shutdownMatches( SessionMatcher matcher )
    {
        shutdownMatches( matcher, null );
    }

    @SuppressWarnings("unchecked")
    protected void shutdownMatches( SessionMatcher matcher, PipelineConnector connector )
    {
        if ( matcher == null ) {
            logger.warn("Invalid arguments");
            return;
        }

        int shutdownCount = 0;
        LinkedList<Vector> shutdownList = new LinkedList<Vector>();

        if ( activeSessions.isEmpty()) 
            return;

        /**
         * Iterate through all sessions and reset matching sessions
         */
        Object[] array;
        synchronized (this) {
            array = activeSessions.entrySet().toArray();
        }
        int i;
        for ( i = 0; i < array.length ; i++ ) {
            Map.Entry<Vector,SessionGlobalState> e = (Map.Entry<Vector,SessionGlobalState>)array[i];
            boolean isMatch;

            SessionGlobalState session = e.getValue();
            Vector vector  = e.getKey();
            NetcapHook netcapHook = session.netcapHook();

            /**
             * Only process sessions involving the specified pipespec and associated connectors
             */
            if ( connector != null ) {
                if ( ! session.getPipelineConnectors().contains( connector ) )
                    continue;
            }
                
            com.untangle.uvm.node.SessionEvent sessionEvent = session.getSessionEvent();
            if ( sessionEvent == null )
                continue;
                
            isMatch = matcher.isMatch( sessionEvent.getPolicyId(), sessionEvent.getProtocol(),
                                       sessionEvent.getClientIntf(), sessionEvent.getServerIntf(),
                                       sessionEvent.getCClientAddr(), sessionEvent.getSServerAddr(),
                                       sessionEvent.getCClientPort(), sessionEvent.getSServerPort(),
                                       session.getAttachments() );

            if ( logger.isDebugEnabled() ) {
                logger.debug( "shutdownMatches(" + matcher.getClass().getSimpleName() + ") Tested    session[" + session.id() + "]: " +
                              sessionEvent.getProtocolName() + "| "  +
                              sessionEvent.getCClientAddr().getHostAddress() + ":" + 
                              sessionEvent.getCClientPort() + " -> " +
                              sessionEvent.getSServerAddr().getHostAddress() + ":" +
                              sessionEvent.getSServerPort() + 
                              " matched: " + isMatch );
            }
            if ( isMatch ) {
                logger.info( "shutdownMatches(" + matcher.getClass().getSimpleName() + ") Shutdown  session[" + session.id() + "]: " +
                             sessionEvent.getProtocolName() + "| "  +
                             sessionEvent.getCClientAddr().getHostAddress() + ":" + 
                             sessionEvent.getCClientPort() + " -> " +
                             sessionEvent.getSServerAddr().getHostAddress() + ":" +
                             sessionEvent.getSServerPort());
                shutdownList.add(vector);
            }
        }

        for ( Vector vector : shutdownList ) {
            try {
                shutdownCount++;
                vector.shutdown();
            }
            catch (Exception e) {
                logger.warn( "Exception killing session", e );
            }
        }

        if ( shutdownCount > 0 )
            logger.info( "shutdownMatches(" + matcher.getClass().getSimpleName() + ") shutdown " + shutdownCount + " sessions.");
    }
    
    public static SessionTable getInstance()
    {
        return INSTANCE;
    }

    private class NatPortAvailabilityKey
    {
        public InetAddress addr;
        public int port;

        public NatPortAvailabilityKey( InetAddress addr, int port )
        {
            this.addr = addr;
            this.port = port;
        }

        @Override
        public boolean equals( Object o )
        {
            if ( o == null )
                return false;
            if ( ! ( o instanceof NatPortAvailabilityKey ) ) 
                return false;

            NatPortAvailabilityKey other = (NatPortAvailabilityKey) o;
                
            if ( this.port != other.port )
                return false;

            if ( this.addr != null && other.addr != null )
                return this.addr.equals( other.addr );
            else 
                return ( this.addr == other.addr );
        }

        @Override
        public int hashCode()
        {
            return addr.hashCode() + port;
        }
    }
}

