/**
 * $Id$
 */

package com.untangle.uvm;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.net.InetAddress;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.untangle.jvector.Vector;
import com.untangle.jnetcap.Netcap;
import com.untangle.jnetcap.NetcapSession;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.app.SessionTuple;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.SessionAttachments;

/**
 * This table stores a global list of all currently active sessions being
 * vectored
 */
public class SessionTableImpl
{
    public static final short PROTO_TCP = 6;

    private final Logger logger = Logger.getLogger(getClass());

    private static final SessionTableImpl INSTANCE = new SessionTableImpl();

    private final Map<Long, SessionGlobalState> sessionTableById = new HashMap<>();
    private final Map<SessionTuple, SessionGlobalState> sessionTableByTuple = new HashMap<>();
    private final Map<NatPortAvailabilityKey, SessionGlobalState> tcpPortAvailabilityMap = new HashMap<>();

    /**
     * Singleton
     */
    private SessionTableImpl()
    {
    }

    /**
     * Get the singleton instance
     * 
     * @return The instance
     */
    public synchronized static SessionTableImpl getInstance()
    {
        return INSTANCE;
    }

// THIS IS FOR ECLIPSE - @formatter:off

    /**
     * Lookup a session by the TUPLE
     * IMPORTANT:
     * The tuple MUST be in the following format:
     * protocol
     * CLIENT SIDE client interface
     * SERVER SIDE server interface
     * CLIENT SIDE client address
     * CLIENT SIDE client port
     * CLIENT SIDE server address
     * CLIENT SIDE server port
     */

// THIS IS FOR ECLIPSE - @formatter:on

    /**
     * Get the session for a tuple
     * 
     * @param tuple
     *        The tuple
     * @return The session
     */
    public SessionGlobalState lookupTuple(SessionTuple tuple)
    {
        return sessionTableByTuple.get(tuple);
    }

    /**
     * Add a sessionId to the table(s)
     * 
     * @param sessionId
     *        - The sessionId to add.
     * @param session
     *        The session
     * @return - True if the item did not already exist
     */
    protected synchronized boolean put(long sessionId, SessionGlobalState session)
    {
        boolean inserted = (sessionTableById.put(sessionId, session) == null);

        if (inserted) {
            if (session.getProtocol() == PROTO_TCP) {
                int port = session.netcapSession().serverSide().client().port();
                InetAddress addr = session.netcapSession().serverSide().client().host();
                NatPortAvailabilityKey key = new NatPortAvailabilityKey(addr, port);
                if (tcpPortAvailabilityMap.get(key) != null) {
                    logger.warn("Collision value in port availability map: " + addr.getHostAddress() + ":" + port);
                    // just continue, not much can be done about it here.
                } else {
                    tcpPortAvailabilityMap.put(key, session);
                }
            }

// THIS IS FOR ECLIPSE - @formatter:off
            SessionTuple tupleKey = new SessionTuple( session.getProtocol(),
                                                              session.netcapSession().clientSide().client().host(),
                                                              session.netcapSession().clientSide().server().host(),
                                                              session.netcapSession().clientSide().client().port(),
                                                              session.netcapSession().clientSide().server().port());
// THIS IS FOR ECLIPSE - @formatter:on

            sessionTableByTuple.put(tupleKey, session);
        }

        return inserted;
    }

    /**
     * Remove a session ID from the table(s).
     * 
     * @param sessionId
     *        The session ID to remove
     * @return - returns the session if it was removed, null if not found
     */
    protected synchronized SessionGlobalState remove(long sessionId)
    {
        SessionGlobalState session = sessionTableById.get(sessionId);
        if (session == null) {
            return null;
        }

        boolean removed = (sessionTableById.remove(sessionId) != null);

        if (removed) {

// THIS IS FOR ECLIPSE - @formatter:off            
            SessionTuple tupleKey = new SessionTuple( session.getProtocol(),
                                                      session.netcapSession().clientSide().client().host(),
                                                      session.netcapSession().clientSide().server().host(),
                                                      session.netcapSession().clientSide().client().port(),
                                                      session.netcapSession().clientSide().server().port());
// THIS IS FOR ECLIPSE - @formatter:on

            if (sessionTableByTuple.remove(tupleKey) == null) {
                logger.warn("Missing value in tuple map: " + tupleKey);
            }

            if (session.getProtocol() == PROTO_TCP) {
                int port = session.netcapSession().serverSide().client().port();
                InetAddress addr = session.netcapSession().serverSide().client().host();
                NatPortAvailabilityKey key = new NatPortAvailabilityKey(addr, port);
                if (tcpPortAvailabilityMap.remove(key) == null) {
                    logger.warn("Missing value in port availability map: " + addr.getHostAddress() + ":" + port);
                }
            }
        }

        return session;
    }

    /**
     * Remove a session from the table(s)
     * 
     * @param protocol
     *        The protocol
     * @param clientIntf
     *        The client interface
     * @param serverIntf
     *        The server interface
     * @param clientAddr
     *        The client address
     * @param serverAddr
     *        The server address
     * @param clientPort
     *        The client port
     * @param serverPort
     *        The server port
     * @return the session if it was removed, null if not found
     */
    protected synchronized SessionGlobalState remove(short protocol, int clientIntf, int serverIntf, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort)
    {
        SessionTuple tupleKey = new SessionTuple(protocol, clientAddr, serverAddr, clientPort, serverPort);
        SessionGlobalState session = sessionTableByTuple.get(tupleKey);
        if (session == null) {
            return null;
        }

        boolean removed = (sessionTableByTuple.remove(tupleKey) != null);

        if (removed) {
            if (sessionTableById.remove(session.id()) == null) {
                logger.warn("Missing value in session ID map: " + session.id());
            }

            if (session.getProtocol() == PROTO_TCP) {
                int port = session.netcapSession().serverSide().client().port();
                InetAddress addr = session.netcapSession().serverSide().client().host();
                NatPortAvailabilityKey key = new NatPortAvailabilityKey(addr, port);
                if (tcpPortAvailabilityMap.remove(key) == null) {
                    logger.warn("Missing value in port availability map: " + addr.getHostAddress() + ":" + port);
                }
            }
        } else {
            logger.warn("Failed to remove session: " + tupleKey);
        }

        return session;
    }

    /**
     * Get the number of sessions remaining
     * 
     * @return The number of sessions remaining
     */
    protected synchronized int count()
    {
        return sessionTableById.size();
    }

    /**
     * Returns the count for a given protocol
     * 
     * @param protocol
     *        The protocol
     * @return The count
     */
    protected synchronized int count(short protocol)
    {
        int count = 0;

        for (SessionGlobalState state : sessionTableById.values()) {
            if (state.getProtocol() == protocol) count++;
        }

        return count;
    }

    /**
     * Returns true if the address port is free according to the
     * tcpPortAvailabilityMap false otherwise
     * 
     * @param addr
     *        The address
     * @param port
     *        The port
     * @return True if free, false if used
     */
    protected synchronized boolean isTcpPortUsed(InetAddress addr, int port)
    {
        NatPortAvailabilityKey key = new NatPortAvailabilityKey(addr, port);
        if (tcpPortAvailabilityMap.get(key) != null) return true;
        else return false;
    }

    /**
     * This kills all active vectors
     * 
     * @return True if vectors were killed, false if no active vectors were
     *         found
     */
    public synchronized boolean shutdownActive()
    {
        boolean foundActive = false;

        for (Iterator<SessionGlobalState> iter = sessionTableById.values().iterator(); iter.hasNext();) {
            SessionGlobalState sess = iter.next();
            Vector vector = sess.netcapHook().getVector();
            if (vector != null) {
                foundActive = true;
                vector.shutdown();
            }
            /*
             * Don't actually remove the item, it is removed when the session
             * exits
             */
        }

        return foundActive;
    }

    /**
     * Returns a new list of all sessions
     * 
     * @return A list of all sessions
     */
    public synchronized List<SessionGlobalState> getSessions()
    {
        return new LinkedList<>(this.sessionTableById.values());
    }

    /**
     * Shutdown all sessions with active vectors that match the passed matcher
     * 
     * @param matcher
     *        The matcher
     */
    protected void shutdownMatches(SessionMatcher matcher)
    {
        shutdownMatches(matcher, null);
    }

    /**
     * Shutdown all sessions with active vectors in use by the passed connector
     * that match the passed matcher If connector is null, all sessions are
     * evaulated
     * 
     * @param matcher
     *        The matcher
     * @param connector
     *        The connector
     */
    @SuppressWarnings("unchecked")
    protected void shutdownMatches(SessionMatcher matcher, PipelineConnector connector)
    {
        if (matcher == null) {
            logger.warn("Invalid arguments");
            return;
        }

        int shutdownCount = 0;
        LinkedList<Vector> shutdownList = new LinkedList<>();
        LinkedList<SessionEvent> conntrackDestroyList = new LinkedList<>();

        if (sessionTableById.isEmpty()) return;

        /**
         * Iterate through all sessions and reset matching sessions
         */
        Object[] array;
        synchronized (this) {
            array = sessionTableById.entrySet().toArray();
        }
        int i;
        for (i = 0; i < array.length; i++) {
            Map.Entry<Long, SessionGlobalState> e = (Map.Entry<Long, SessionGlobalState>) array[i];
            boolean isMatch;

            SessionGlobalState session = e.getValue();
            Long sessionId = e.getKey();
            NetcapHook netcapHook = session.netcapHook();

            /**
             * Only process sessions involving the specified pipespec and
             * associated connectors
             */
            if (connector != null) {
                if (!session.getPipelineConnectors().contains(connector)) continue;
            }

            com.untangle.uvm.app.SessionEvent sessionEvent = session.getSessionEvent();
            if (sessionEvent == null) continue;

// THIS IS FOR ECLIPSE - @formatter:off

            isMatch = matcher.isMatch( sessionEvent.getPolicyId(), sessionEvent.getProtocol(),
                                       sessionEvent.getClientIntf(), sessionEvent.getServerIntf(),
                                       sessionEvent.getCClientAddr(), sessionEvent.getSServerAddr(),
                                       sessionEvent.getCClientPort(), sessionEvent.getSServerPort(),
                                       session );

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

// THIS IS FOR ECLIPSE - @formatter:on

                Vector vector = null;
                if (session.netcapHook() != null) vector = session.netcapHook().getVector();
                if (vector != null) {
                    shutdownList.add(vector);
                }
                // if the vector doesn't exist then the session may still be alive
                // but dynamically bypassed and still passing at layer-3
                // if this is the case try to delete the conntrack entry
                else {
                    NetcapSession netcapSession = session.netcapSession();
                    if (netcapSession != null) conntrackDestroyList.add(sessionEvent);
                }
            }
        }

        for (Vector vector : shutdownList) {
            try {
                shutdownCount++;
                vector.shutdown();
            } catch (Exception e) {
                logger.warn("Exception killing session", e);
            }
        }

        for (SessionEvent sessionEvent : conntrackDestroyList) {
            String cli = sessionEvent.getCClientAddr().getHostAddress();
            String srv = sessionEvent.getCServerAddr().getHostAddress();
            Netcap.conntrackDestroy(sessionEvent.getProtocol(), cli, sessionEvent.getCClientPort(), srv, sessionEvent.getCServerPort());
        }

        if (shutdownCount > 0) logger.info("shutdownMatches(" + matcher.getClass().getSimpleName() + ") shutdown " + shutdownCount + " sessions.");
    }

    /**
     * Class for managing NAT port availability
     */
    private class NatPortAvailabilityKey
    {
        public InetAddress addr;
        public int port;

        /**
         * Constructor
         * 
         * @param addr
         *        The address
         * @param port
         *        The port
         */
        public NatPortAvailabilityKey(InetAddress addr, int port)
        {
            this.addr = addr;
            this.port = port;
        }

        /**
         * Compare one object to another
         * 
         * @param o
         *        The object for comparison
         * @return True if equal, otherwise false
         */
        @Override
        public boolean equals(Object o)
        {
            if (o == null) return false;
            if (!(o instanceof NatPortAvailabilityKey)) return false;

            NatPortAvailabilityKey other = (NatPortAvailabilityKey) o;

            if (this.port != other.port) return false;

            if (this.addr != null && other.addr != null) return this.addr.equals(other.addr);
            else return (this.addr == other.addr);
        }

        /**
         * Get the hash code for the address+port
         * 
         * @return The hash code
         */
        @Override
        public int hashCode()
        {
            return addr.hashCode() + port;
        }
    }
}
