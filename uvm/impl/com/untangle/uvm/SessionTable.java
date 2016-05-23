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

    private final Map<Long,SessionGlobalState> activeSessions = new HashMap<Long,SessionGlobalState>();
    private final Map<SessionTupleKey,SessionGlobalState> activeSessionsByTuple = new HashMap<SessionTupleKey,SessionGlobalState>();
    private final Map<NatPortAvailabilityKey,SessionGlobalState> tcpPortsUsed = new HashMap<NatPortAvailabilityKey,SessionGlobalState>();

    public static final short PROTO_TCP = 6;
    
    /* Singleton */
    private SessionTable() {}

    /**
     * Add a sessionId to the hash set.
     * @param  sessionId - The sessionId to add.
     * @return - True if the item did not already exist
     */
    protected synchronized boolean put( long sessionId, SessionGlobalState session )
    {
        boolean inserted = ( activeSessions.put( sessionId, session ) == null );

        if ( inserted ) {
            if ( session.getProtocol() == PROTO_TCP ) {
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

            SessionTupleKey tupleKey = new SessionTupleKey( session );
            activeSessionsByTuple.put( tupleKey, session );
        }
        
        return inserted;
    }

    /**
     * Remove a session ID from the table(s).
     * @return - returns the session if it was removed, null if not found
     */
    protected synchronized SessionGlobalState remove( long sessionId )
    {
        SessionGlobalState session = activeSessions.get( sessionId );
        if ( session == null ) {
            return null;
        }

        boolean removed = ( activeSessions.remove( sessionId ) != null );
        
        if ( removed ) {
            if ( session.getProtocol() == PROTO_TCP ) {
                int port = session.netcapSession().serverSide().client().port();
                InetAddress addr = session.netcapSession().serverSide().client().host();
                NatPortAvailabilityKey key = new NatPortAvailabilityKey( addr, port );
                if ( tcpPortsUsed.remove( key ) == null ) {
                    logger.warn("Missing value in port availability map: " + addr.getHostAddress() + ":" + port );
                }
            }

            SessionTupleKey tupleKey = new SessionTupleKey( session );
            if ( activeSessionsByTuple.remove( tupleKey ) == null ) {
                logger.warn("Missing value in tuple map: " + tupleKey );
            }
        }

        return session;
    }

    /**
     * Remove a session from the table(s)
     * @return - returns the session if it was removed, null if not found
     */
    protected synchronized SessionGlobalState remove( short protocol, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort )
    {
        SessionTupleKey tupleKey = new SessionTupleKey( protocol, clientAddr, serverAddr, clientPort, serverPort );
        SessionGlobalState session = activeSessionsByTuple.get( tupleKey );
        if ( session == null ) {
            return null;
        }

        boolean removed = ( activeSessionsByTuple.remove( tupleKey ) != null );
        
        if ( removed && session.getProtocol() == PROTO_TCP ) {
            int port = session.netcapSession().serverSide().client().port();
            InetAddress addr = session.netcapSession().serverSide().client().host();
            NatPortAvailabilityKey key = new NatPortAvailabilityKey( addr, port );
            if ( tcpPortsUsed.remove( key ) == null ) {
                logger.warn("Missing value in port availability map: " + addr.getHostAddress() + ":" + port );
            }

            if ( activeSessionsByTuple.remove( session.id() ) == null ) {
                logger.warn("Missing value in session ID map: " + session.id() );
            }
        }

        return session;
    }
    
    /**
     * Get the number of sessions remaining
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
        boolean foundActive = false;

        for ( Iterator<SessionGlobalState> iter = activeSessions.values().iterator(); iter.hasNext() ; ) {
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

    private class SessionTupleKey
    {
        public static final short PROTO_TCP = 6;
        public static final short PROTO_UDP = 17;

        private short protocol;
        private InetAddress clientAddr;
        private int clientPort;
        private InetAddress serverAddr;
        private int serverPort;
    
        public SessionTupleKey( short protocol, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort )
        {
            this.protocol = protocol;
            this.clientAddr = clientAddr;
            this.clientPort = clientPort;
            this.serverAddr = serverAddr;
            this.serverPort = serverPort;
        }

        public SessionTupleKey( SessionGlobalState session )
        {
            this.protocol = session.getProtocol();
            this.clientAddr = session.netcapSession().clientSide().client().host();
            this.clientPort = session.netcapSession().clientSide().client().port();
            this.serverAddr = session.netcapSession().serverSide().server().host();
            this.serverPort = session.netcapSession().serverSide().server().port();
        }
    
        public short getProtocol() { return this.protocol; }
        public void setProtocol( short protocol ) { this.protocol = protocol; }

        public InetAddress getClientAddr() { return this.clientAddr; }
        public void setClientAddr( InetAddress clientAddr ) { this.clientAddr = clientAddr; }

        public int getClientPort() { return this.clientPort; }
        public void setClientPort( int clientPort ) { this.clientPort = clientPort; }

        public InetAddress getServerAddr() { return this.serverAddr; }
        public void setServerAddr( InetAddress serverAddr ) { this.serverAddr = serverAddr; }

        public int getServerPort() { return this.serverPort; }
        public void setServerPort( int serverPort ) { this.serverPort = serverPort; }

        @Override
        public int hashCode()
        {
            return protocol + clientAddr.hashCode() + serverPort + serverAddr.hashCode() + clientPort;
        }

        @Override
        public boolean equals(Object o)
        {
            if ( ! (o instanceof SessionTupleKey) )
                return false;
            SessionTupleKey s = (SessionTupleKey)o;
            return s.protocol == protocol
                && s.clientPort == clientPort
                && s.serverPort == serverPort
                && s.clientAddr.equals(clientAddr)
                && s.serverAddr.equals(serverAddr);
        }

        @Override
        public String toString()
        {
            return "[Tuple " + protocol + " " +
                (clientAddr == null ? "null" : clientAddr.getHostAddress()) + ":" +
                clientPort + " -> " +
                (serverAddr == null ? "null" : serverAddr.getHostAddress()) + ":" +
                serverPort + "]";
        }
    }
}

