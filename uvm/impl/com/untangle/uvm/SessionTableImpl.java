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
import com.untangle.uvm.node.SessionTupleImpl;
import com.untangle.uvm.vnet.PipelineConnector;

/**
 * This table stores a global list of all currently active sessions being vectored
 */
public class SessionTableImpl
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final SessionTableImpl INSTANCE = new SessionTableImpl();

    private final Map<Long,SessionGlobalState> sessionTableById = new HashMap<Long,SessionGlobalState>();
    private final Map<SessionTupleImpl,SessionGlobalState> sessionTableByTuple = new HashMap<SessionTupleImpl,SessionGlobalState>();
    private final Map<NatPortAvailabilityKey,SessionGlobalState> tcpPortAvailabilityMap = new HashMap<NatPortAvailabilityKey,SessionGlobalState>();

    public static final short PROTO_TCP = 6;
    
    /* Singleton */
    private SessionTableImpl() {}

    public static SessionTableImpl getInstance()
    {
        return INSTANCE;
    }

    public SessionGlobalState lookupTuple( SessionTupleImpl tuple )
    {
        return sessionTableByTuple.get( tuple );
    }

    /**
     * Add a sessionId to the table(s)
     * @param  sessionId - The sessionId to add.
     * @return - True if the item did not already exist
     */
    protected synchronized boolean put( long sessionId, SessionGlobalState session )
    {
        boolean inserted = ( sessionTableById.put( sessionId, session ) == null );

        if ( inserted ) {
            if ( session.getProtocol() == PROTO_TCP ) {
                int port = session.netcapSession().serverSide().client().port();
                InetAddress addr = session.netcapSession().serverSide().client().host();
                NatPortAvailabilityKey key = new NatPortAvailabilityKey( addr, port );
                if ( tcpPortAvailabilityMap.get( key ) != null ) {
                    logger.warn("Collision value in port availability map: " + addr.getHostAddress() + ":" + port);
                    // just continue, not much can be done about it here.
                } else {
                    tcpPortAvailabilityMap.put( key, session );
                }
            }

            SessionTupleImpl tupleKey = new SessionTupleImpl( session.getProtocol(),
                                                              session.netcapSession().clientSide().interfaceId(),
                                                              session.netcapSession().serverSide().interfaceId(),
                                                              session.netcapSession().clientSide().client().host(),
                                                              session.netcapSession().serverSide().server().host(),
                                                              session.netcapSession().clientSide().client().port(),
                                                              session.netcapSession().serverSide().server().port());
            sessionTableByTuple.put( tupleKey, session );
        }
        
        return inserted;
    }

    /**
     * Remove a session ID from the table(s).
     * @return - returns the session if it was removed, null if not found
     */
    protected synchronized SessionGlobalState remove( long sessionId )
    {
        SessionGlobalState session = sessionTableById.get( sessionId );
        if ( session == null ) {
            return null;
        }

        boolean removed = ( sessionTableById.remove( sessionId ) != null );
        
        if ( removed ) {
            SessionTupleImpl tupleKey = new SessionTupleImpl( session.getProtocol(),
                                                              session.netcapSession().clientSide().interfaceId(),
                                                              session.netcapSession().serverSide().interfaceId(),
                                                              session.netcapSession().clientSide().client().host(),
                                                              session.netcapSession().serverSide().server().host(),
                                                              session.netcapSession().clientSide().client().port(),
                                                              session.netcapSession().serverSide().server().port());
            if ( sessionTableByTuple.remove( tupleKey ) == null ) {
                logger.warn("Missing value in tuple map: " + tupleKey );
            }

            if ( session.getProtocol() == PROTO_TCP ) {
                int port = session.netcapSession().serverSide().client().port();
                InetAddress addr = session.netcapSession().serverSide().client().host();
                NatPortAvailabilityKey key = new NatPortAvailabilityKey( addr, port );
                if ( tcpPortAvailabilityMap.remove( key ) == null ) {
                    logger.warn("Missing value in port availability map: " + addr.getHostAddress() + ":" + port );
                }
            }
        }

        return session;
    }

    /**
     * Remove a session from the table(s)
     * @return - returns the session if it was removed, null if not found
     */
    protected synchronized SessionGlobalState remove( short protocol, int clientIntf, int serverIntf, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort )
    {
        SessionTupleImpl tupleKey = new SessionTupleImpl( protocol, clientIntf, serverIntf, clientAddr, serverAddr, clientPort, serverPort );
        SessionGlobalState session = sessionTableByTuple.get( tupleKey );
        if ( session == null ) {
            return null;
        }

        boolean removed = ( sessionTableByTuple.remove( tupleKey ) != null );
        
        if ( removed  ) {
            if ( sessionTableById.remove( session.id() ) == null ) {
                logger.warn("Missing value in session ID map: " + session.id() );
            }

            if ( session.getProtocol() == PROTO_TCP ) {
                int port = session.netcapSession().serverSide().client().port();
                InetAddress addr = session.netcapSession().serverSide().client().host();
                NatPortAvailabilityKey key = new NatPortAvailabilityKey( addr, port );
                if ( tcpPortAvailabilityMap.remove( key ) == null ) {
                    logger.warn("Missing value in port availability map: " + addr.getHostAddress() + ":" + port );
                }
            }
        } else {
            logger.warn("Failed to remove session: " + tupleKey);
        }

        return session;
    }
    
    /**
     * Get the number of sessions remaining
     */
    protected synchronized int count()
    {
        return sessionTableById.size();
    }

    /**
     * Returns the count for a given protocol
     */
    protected synchronized int count( short protocol )
    {
        int count = 0;
        
        for ( SessionGlobalState state : sessionTableById.values() ) {
            if (state.getProtocol() == protocol)
                count++;
        }

        return count;
    }

    /**
     * Returns true if the address port is free according to the tcpPortAvailabilityMap
     * false otherwise
     */
    protected synchronized boolean isTcpPortUsed( InetAddress addr, int port )
    {
        NatPortAvailabilityKey key = new NatPortAvailabilityKey( addr, port );
        if ( tcpPortAvailabilityMap.get( key ) != null )
            return true;
        else
            return false;
    }
    
    /**
     * This kills all active vectors
     * Returns true if vectors were killed
     * false if no active vectors were found
     */
    public synchronized boolean shutdownActive()
    {
        boolean foundActive = false;

        for ( Iterator<SessionGlobalState> iter = sessionTableById.values().iterator(); iter.hasNext() ; ) {
            SessionGlobalState sess = iter.next();
            Vector vector = sess.netcapHook().getVector();
            if ( vector != null ) {
                foundActive = true;
                vector.shutdown();
            }
            /* Don't actually remove the item, it is removed when the session exits */
        }

        return foundActive;
    }

    /**
     * Returns a new list of all sessions
     */
    public synchronized List<SessionGlobalState> getSessions()
    {
        return new LinkedList<SessionGlobalState>(this.sessionTableById.values());
    }
    
    /**
     * Shutdown all sessions with active vectors
     * that match the passed matcher
     */
    protected void shutdownMatches( SessionMatcher matcher )
    {
        shutdownMatches( matcher, null );
    }

    /**
     * Shutdown all sessions with active vectors
     * in use by the passed connector
     * that match the passed matcher
     * If connector is null, all sessions are evaulated
     */
    @SuppressWarnings("unchecked")
    protected void shutdownMatches( SessionMatcher matcher, PipelineConnector connector )
    {
        if ( matcher == null ) {
            logger.warn("Invalid arguments");
            return;
        }

        int shutdownCount = 0;
        LinkedList<Vector> shutdownList = new LinkedList<Vector>();

        if ( sessionTableById.isEmpty()) 
            return;

        /**
         * Iterate through all sessions and reset matching sessions
         */
        Object[] array;
        synchronized (this) {
            array = sessionTableById.entrySet().toArray();
        }
        int i;
        for ( i = 0; i < array.length ; i++ ) {
            Map.Entry<Long,SessionGlobalState> e = (Map.Entry<Long,SessionGlobalState>)array[i];
            boolean isMatch;

            SessionGlobalState session = e.getValue();
            Long sessionId  = e.getKey();
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

                Vector vector = null;
                if ( session.netcapHook() != null )
                    vector = session.netcapHook().getVector();
                if ( vector != null )
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

