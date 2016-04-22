/*
 * $Id$
 */
package com.untangle.uvm;

import java.util.List;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.HashMap;
import java.net.InetAddress;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;
import org.apache.log4j.Logger;

import com.untangle.uvm.SessionMonitor;
import com.untangle.uvm.UvmState;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SessionMonitorEntry;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.SessionTuple;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.network.InterfaceSettings;

/**
 * SessionMonitor is a utility class that provides some convenient functions
 * to monitor and view current sessions and state existing in the untangle-vm
 *
 * This is used by the UI to display state
 */
public class SessionMonitorImpl implements SessionMonitor
{
    private final Logger logger = Logger.getLogger(getClass());

    private static ExecManager execManager = null;
    
    UvmContext uvmContext;
    
    public SessionMonitorImpl ()
    {
        SessionMonitorImpl.execManager = UvmContextFactory.context().createExecManager();
        uvmContext = UvmContextFactory.context();
    }

    public List<SessionMonitorEntry> getMergedBandwidthSessions(String interfaceIdStr, int nodeId)
    {
        /**
         * Find the the system interface name that matches this ID
         */
        try {
            int interfaceId = Integer.parseInt(interfaceIdStr);
            InterfaceSettings intf = uvmContext.networkManager().findInterfaceId( interfaceId );
            
            if (intf == null) {
                logger.warn( "Unabled to find interface " + interfaceId );
                return null;
            }
            
            String systemName = intf.getSystemDev();
            return _getMergedBandwidthSessions(systemName);

        } catch (Exception e) {
            logger.warn("Unable to retrieve sessions",e);
            return null;
        }
    }
    
    /**
     * documented in SessionMonitor.java
     */
    public List<SessionMonitorEntry> getMergedBandwidthSessions(String interfaceIdStr)
    {
        return getMergedBandwidthSessions(interfaceIdStr, 0);
    }

    /**
     * documented in SessionMonitor.java
     */
    public List<SessionMonitorEntry> getMergedBandwidthSessions()
    {
        return getMergedBandwidthSessions("0");
    }

    /**
     * documented in SessionMonitor.java
     */
    public List<SessionMonitorEntry> getMergedSessions()
    {
        return getMergedSessions(0);
    }
    
    /**
     * documented in SessionMonitor.java
     */
    public List<SessionMonitorEntry> getMergedSessions(long nodeId)
    {
        List<SessionMonitorEntry> sessions = this._getConntrackSessionMonitorEntrys();
        List<SessionGlobalState> netcapSessions = SessionTable.getInstance().getSessions();
        List<SessionTuple> nodeSessions = null;;

        Node node = null;
        if (nodeId > 0)
            node = UvmContextFactory.context().nodeManager().node(nodeId);
        if (node != null)
            nodeSessions = node.liveSessions();

        HashMap<Tuple,SessionGlobalState> map = new HashMap<Tuple,SessionGlobalState>();
        for (SessionGlobalState netcapSession : netcapSessions) {
            com.untangle.uvm.node.SessionTuple clientSide = netcapSession.netcapHook().getClientSide();
            Tuple clientTuple = new Tuple( clientSide.getProtocol(),
                                           clientSide.getClientAddr(),
                                           clientSide.getServerAddr(),
                                           clientSide.getClientPort(),
                                           clientSide.getServerPort());
            com.untangle.uvm.node.SessionTuple serverSide = netcapSession.netcapHook().getServerSide();
            Tuple serverTuple = new Tuple( serverSide.getProtocol(),
                                           serverSide.getClientAddr(),
                                           serverSide.getServerAddr(),
                                           serverSide.getClientPort(),
                                           serverSide.getServerPort());
            
            map.put( clientTuple, netcapSession );
            map.put( serverTuple, netcapSession );
        }
        
        for (Iterator<SessionMonitorEntry> i = sessions.iterator(); i.hasNext(); ) {  
            SessionMonitorEntry session = i.next();

            session.setPolicy("");             
            if (session.getClientIntf() == null || session.getClientIntf() == 0 )
                session.setClientIntf(Integer.valueOf(-1));
            if (session.getServerIntf() == null || session.getServerIntf() == 0 )
                session.setServerIntf(Integer.valueOf(-1));
            session.setPriority(session.getQosPriority()); 

            Tuple tuple = _makeTuple( session );
            SessionGlobalState netcapSession = map.get(tuple); // find corresponding netcap

            if ( netcapSession != null ) {
                try {
                    int priority = netcapSession.netcapSession().clientQosMark();
                    com.untangle.uvm.node.SessionTuple clientSide = netcapSession.netcapHook().getClientSide();
                    com.untangle.uvm.node.SessionTuple serverSide = netcapSession.netcapHook().getServerSide();

                    NetcapHook hook = netcapSession.netcapHook();
                    if (hook == null)
                        continue;
                        
                    Integer policyId = hook.getPolicyId();
                    if (policyId == null)
                        session.setPolicy("");
                    else
                        session.setPolicy(policyId.toString()); 

                    session.setSessionId(netcapSession.id());
                    session.setCreationTime(netcapSession.getCreationTime());
                    session.setPipeline(netcapSession.getPipelineDescription());
                    session.setBypassed(Boolean.FALSE);
                    session.setClientIntf(new Integer(clientSide.getClientIntf()));
                    session.setServerIntf(new Integer(serverSide.getServerIntf()));

                    /**
                     * The conntrack entry shows that this session has been redirect to the local host
                     * We need to overwrite that with the correct info
                     */
                    session.setPostNatClient(serverSide.getClientAddr());
                    session.setPostNatServer(serverSide.getServerAddr());
                    session.setPostNatClientPort(serverSide.getClientPort());
                    session.setPostNatServerPort(serverSide.getServerPort());

                    /**
                     * Only have one priority per session
                     * Assume both client and server are the same
                     */
                    if (priority != 0)
                        session.setPriority(priority);

                    session.setAttachments(netcapSession.getAttachments());
                } catch (Exception e) {
                    logger.warn("Exception while searching for session",e);
                }
            }
            // else netcapSession == null (no UVM session found)
            else {
                if ( session.getMark() != null ) {
                    Integer mark = session.getMark();
                    // if session was not explicitly bypassed hide it, but yet its not at layer 7, hide it.
                    // This is so we don't show sessions that have been blocked or died at layer 7, but still exist in conntrack
                    // Doing so is confusing because it would show up as "bypassed" when its actually already been blocked.
                    if ((mark & 0x01000000) == 0) {
                        logger.debug("Removing session from view (not scanned but no bypass mark): " + session);
                        i.remove();
                        continue;
                    }
                }

                // if its not being scanned by the UVM it must be bypassed
                // this is set from the mark, setting this manually should not be required
                // session.setBypassed(Boolean.TRUE);
            }

            /**
             * Ignore sessions to 192.0.2.42
             */
            if ( "192.0.2.42".equals( session.getPostNatServer().getHostAddress() ) ) {
                logger.debug("Removing session from view (internal session to 192.0.2.42): " + session);
                i.remove();
                continue;
            }
        }

        /**
         * Update some additional fields
         */
        for (SessionMonitorEntry entry : sessions) {
            entry.setNatted(Boolean.FALSE);
            entry.setPortForwarded(Boolean.FALSE);

            if (! entry.getPreNatClient().equals(entry.getPostNatClient())) {
                entry.setNatted(Boolean.TRUE);
            }
            if (! entry.getPreNatServer().equals(entry.getPostNatServer())) {
                entry.setPortForwarded(Boolean.TRUE);
            }
        }

        /**
         * If a nodeId was specified remove all sessions not being touched by that nodeId
         */
        if ( nodeSessions != null ) {
            for (Iterator<SessionMonitorEntry> i = sessions.iterator(); i.hasNext(); ) {  
                SessionMonitorEntry entry = i.next();
                Long sessionId = entry.getSessionId();
                boolean found = false;
                for (SessionTuple tuple : nodeSessions) {
                    if ( sessionId != null && sessionId == tuple.getSessionId() ) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    i.remove();
            }
        }

        return sessions;
    }

    /**
     * Retrieve the session stats (but not the sessions themselves)
     */
    public org.json.JSONObject getSessionStats()
    {
        List<SessionMonitorEntry> sessions = getMergedSessions();
        org.json.JSONObject json = new org.json.JSONObject();
        int totalSessions = 0;
        int bypassedSessions = 0;
        int scannedSessions = 0;
        int scannedTCPSessions = 0;
        int scannedUDPSessions = 0;

        try {
            if ( sessions != null ) {
                for ( SessionMonitorEntry entry : sessions ) {
                    totalSessions++;

                    if ( ! entry.getBypassed() ) {
                        scannedSessions++;
                        if ( "TCP".equals(entry.getProtocol()) )
                            scannedTCPSessions++;
                        else if ( "UDP".equals(entry.getProtocol()) )
                            scannedUDPSessions++;
                    } else {
                        bypassedSessions++;
                    }
                }
            }

            json.put("totalSessions", totalSessions);
            json.put("bypassedSessions", bypassedSessions);
            json.put("scannedSessions", scannedSessions);
            json.put("scannedTCPSessions", scannedTCPSessions);
            json.put("scannedUDPSessions", scannedUDPSessions);
        } catch (Exception e) {
            logger.error("Error generating session stats", e);
        }

        return json;
    }

    /**
     * Returns a fully merged list for the given interface
     * systemIntfName is the system interface (example: "eth0")
     * This takes 5 seconds to gather data before it returns
     */
    private List<SessionMonitorEntry> _getMergedBandwidthSessions(String systemIntfName)
    {
        List<SessionMonitorEntry> jnettopSessions = _getJnettopSessionMonitorEntrys(systemIntfName);
        List<SessionMonitorEntry> sessions = this.getMergedSessions();

        HashMap<Tuple,SessionMonitorEntry> map = new HashMap<Tuple,SessionMonitorEntry>();
        for (SessionMonitorEntry entry : jnettopSessions) {
            Tuple tuple = _makeTuple( entry.getProtocol(),
                                      entry.getPreNatClient(),
                                      entry.getPreNatServer(),
                                      entry.getPreNatClientPort(),
                                      entry.getPreNatServerPort());
            
            map.put( tuple, entry );
        }

        for (SessionMonitorEntry session : sessions) {
            
            session.setClientKBps(Float.valueOf(0.0f));
            session.setServerKBps(Float.valueOf(0.0f));
            session.setTotalKBps(Float.valueOf(0.0f));

            Tuple a = _makeTuple(session.getProtocol(),
                                 session.getPreNatClient(),session.getPreNatServer(),
                                 session.getPreNatClientPort(),session.getPreNatServerPort());
            Tuple b = _makeTuple(session.getProtocol(),
                                 session.getPreNatServer(),session.getPreNatClient(),
                                 session.getPreNatServerPort(),session.getPreNatClientPort());
            Tuple c = _makeTuple(session.getProtocol(),
                                 session.getPostNatClient(),session.getPostNatServer(),
                                 session.getPostNatClientPort(),session.getPostNatServerPort());
            Tuple d = _makeTuple(session.getProtocol(),
                                 session.getPostNatServer(),session.getPostNatClient(),
                                 session.getPostNatServerPort(),session.getPostNatClientPort());

            SessionMonitorEntry matchingEntry = null;
            if ( matchingEntry == null ) {
                matchingEntry = map.get(a);
                if ( matchingEntry != null ) map.remove(a);
            }
            if ( matchingEntry == null ) {
                matchingEntry = map.get(b);
                if ( matchingEntry != null ) map.remove(b);
            }
            if ( matchingEntry == null ) {
                matchingEntry = map.get(c);
                if ( matchingEntry != null ) map.remove(c);
            }
            if ( matchingEntry == null ) {
                matchingEntry = map.get(d);
                if ( matchingEntry != null ) map.remove(d);
            }

            if ( matchingEntry == null ) {
                logger.debug("Session not found in jnettop: " +
                            session.getPreNatClient() + ":" + session.getPreNatClientPort() + " -> " + session.getPreNatServer() + ":" + session.getPreNatServerPort() + "  |  " +
                            session.getPostNatClient() + ":" + session.getPostNatClientPort() + " -> " + session.getPostNatServer() + ":" + session.getPostNatServerPort());
            } else {
                session.setClientKBps(matchingEntry.getClientKBps());
                session.setServerKBps(matchingEntry.getServerKBps());
                session.setTotalKBps(matchingEntry.getTotalKBps());
            }
        }

        // check for sessions that jnettop found that but we were unable to locate the corresponding conntrack/uvm session
        for (SessionMonitorEntry session : map.values()) {
            logger.warn("Unused jnettop session : " +
                        session.getPreNatClient() + ":" + session.getPreNatClientPort() + " -> " + session.getPreNatServer() + ":" + session.getPreNatServerPort() + "  | " +
                        session.getTotalKBps() + "KB/s");
                
        }
        
        return sessions;
    }

    /**
     * This returns a list of sessions and bandwidth usages reported by jnettop over 5 seconds
     * This takes 5 seconds to gather data before it returns
     */
    @SuppressWarnings("unchecked") //JSON
    private List<SessionMonitorEntry> _getJnettopSessionMonitorEntrys(String systemIntfName)
    {
        String execStr = new String(System.getProperty("uvm.bin.dir") + "/" + "ut-jnettop" + " " + systemIntfName);

        try {
            String output = SessionMonitorImpl.execManager.execOutput(execStr);
            List<SessionMonitorEntry> entryList = (List<SessionMonitorEntry>) ((UvmContextImpl)UvmContextFactory.context()).getSerializer().fromJSON(output);
            return entryList;
            
        } catch (org.jabsorb.serializer.UnmarshallException exc) {
            logger.error("Unable to read jnettop - invalid JSON",exc);
            return null;
        }
    }
    
    /**
     * This returns a list of descriptors for all sessions in the conntrack table
     */
    @SuppressWarnings("unchecked") //JSON
    private List<SessionMonitorEntry> _getConntrackSessionMonitorEntrys()
    {
        return parseProcNetIpConntrack();
    }

    private Tuple _makeTuple( String protocolStr, InetAddress preNatClient, InetAddress preNatServer, int preNatClientPort, int preNatServerPort )
    {
        short protocol;
        if ( "TCP".equals(protocolStr) )
            protocol = Tuple.PROTO_TCP;
        else if ( "UDP".equals(protocolStr) )
            protocol = Tuple.PROTO_UDP;
        else {
            logger.warn("Unknown protocol: " + protocolStr);
            protocol = 0;
        }
        return new Tuple( protocol, preNatClient, preNatServer, preNatClientPort, preNatServerPort );
    }

    private Tuple _makeTuple( SessionMonitorEntry session )
    {
        short protocol;
        if ( "TCP".equals(session.getProtocol()) )
            protocol = Tuple.PROTO_TCP;
        else if ( "UDP".equals(session.getProtocol()) )
            protocol = Tuple.PROTO_UDP;
        else {
            logger.warn("Unknown protocol: " + session.getProtocol());
            protocol = 0;
        }
        return new Tuple( protocol,
                          session.getPreNatClient(),
                          session.getPreNatServer(),
                          session.getPreNatClientPort(),
                          session.getPreNatServerPort() );
    }

    private class Tuple
    {
        public static final short PROTO_TCP = 6;
        public static final short PROTO_UDP = 17;

        public short protocol;
        public InetAddress clientAddr;
        public InetAddress serverAddr;
        public int clientPort;
        public int serverPort;

        public Tuple( short protocol, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort )
        {
            this.protocol = protocol;
            this.clientAddr = clientAddr;
            this.serverAddr = serverAddr;
            this.clientPort = clientPort;
            this.serverPort = serverPort;
        }

        public int hashCode()
        {
            return protocol +
                (clientAddr == null ? 0 : clientAddr.hashCode()) +
                (serverAddr == null ? 0 : serverAddr.hashCode()) * clientPort * serverPort;
        }
        
        public boolean equals( Object o2 )
        {
            if ( ! ( o2 instanceof Tuple ) ) {
                return false;
            }
            Tuple o = (Tuple) o2;
            if ( o.protocol != this.protocol ||
                 o.clientPort != this.clientPort ||
                 o.serverPort != this.serverPort ) {
                return false;
            }
            if ( ! ( o.clientAddr == null ? this.clientAddr == null : o.clientAddr.equals(this.clientAddr) ) ) {
                return false;
            }
            if ( ! ( o.serverAddr == null ? this.serverAddr == null : o.serverAddr.equals(this.serverAddr) ) ) {
                return false;
            }
            return true;
        }

        public String toString()
        {
            return this.protocol + "| " +
                ( this.clientAddr == null ? "null" : this.clientAddr.getHostAddress() ) + ":" +
                this.clientPort + " -> " +
                ( this.serverAddr == null ? "null" : this.serverAddr.getHostAddress() ) + ":" +
                this.serverPort;
        }
    }

    private List<SessionMonitorEntry> parseProcNetIpConntrack()
    {
        BufferedReader br = null;
        String line;
        LinkedList<SessionMonitorEntry> list = new LinkedList<SessionMonitorEntry>();
        
        try {
            br = new BufferedReader(new FileReader("/proc/net/ip_conntrack"));
            while ((line = br.readLine()) != null) {
                try {
                    if ( logger.isDebugEnabled() )
                        logger.debug("parseProcNetIpConntrack line: " + line);
                    String[] parts = line.split("\\s+");
                    SessionMonitorEntry newEntry = new SessionMonitorEntry();

                    if ( parts.length < 10 ) {
                        logger.warn("Too few parts: " + line);
                        continue;
                    }

                    // part[0] is either "udp" or "tcp"
                    if ( !"udp".equals(parts[0]) && !"tcp".equals(parts[0]) ) {
                        if ( logger.isDebugEnabled() )
                            logger.debug("parseProcNetIpConntrack skip line: " + line);
                        continue;
                    }
                    if ( line.contains("127.0.0.1") ) {
                        if ( logger.isDebugEnabled() )
                            logger.debug("parseProcNetIpConntrack ignore line: " + line);
                        continue;
                    }
                        
                    newEntry.setProtocol(parts[0].toUpperCase());

                    int src_count = 0;
                    int dst_count = 0;
                    int sport_count = 0;
                    int dport_count = 0;
                    for ( int i = 0 ; i < parts.length ; i++ ) {
                        String part = parts[i];
                        String[] subparts = part.split("=");
                        if ( subparts.length != 2 )
                            continue;

                        if ( logger.isDebugEnabled() )
                            logger.debug("parseProcNetIpConntrack part: " + part);
                        String varname = subparts[0];
                        String varval = subparts[1];

                        switch ( varname ) {
                        case "src":
                            if ( src_count == 0 )
                                newEntry.setPreNatClient( InetAddress.getByName( varval ) ); // request src is pre nat client 
                            else 
                                newEntry.setPostNatServer( InetAddress.getByName( varval ) ); // reply src is post nat server
                            src_count++;
                            break;
                        case "dst":
                            if ( dst_count == 0 )
                                newEntry.setPreNatServer( InetAddress.getByName( varval ) ); // request dst is pre nat server
                            else 
                                newEntry.setPostNatClient( InetAddress.getByName( varval ) ); // reply dst is pre nat client
                            dst_count++;
                            break;
                        case "sport":
                            if ( sport_count == 0 )
                                newEntry.setPreNatClientPort( Integer.parseInt( varval ) ); // request sport is pre nat client port
                            else 
                                newEntry.setPostNatServerPort( Integer.parseInt( varval ) ); // reply sport is post nat server port
                            sport_count++;
                            break;
                        case "dport":
                            if ( dport_count == 0 )
                                newEntry.setPreNatServerPort( Integer.parseInt( varval ) ); // request dport is pre nat server port
                            else 
                                newEntry.setPostNatClientPort( Integer.parseInt( varval ) ); // reply dport is post nat client port
                            dport_count++;
                            break;
                        case "mark":
                            int mark = Integer.parseInt( varval );
                            newEntry.setMark( mark ); 
                            newEntry.setBypassed( ((mark & 0x01000000) != 0) );
                            newEntry.setPriority( (mark & 0x000F0000) >> 16 );
                            newEntry.setClientIntf( (mark & 0x000000FF) >> 0 );
                            newEntry.setServerIntf( (mark & 0x0000FF00) >> 8 );
                            break;
                        default:
                            if ( logger.isDebugEnabled() )
                                logger.debug("parseProcNetIpConntrack skip part: " + part);
                        break;
                        }
                    }

                    list.add(newEntry);
                    
                } catch ( Exception lineException ) {
                    logger.warn("Failed to parse /proc/net/ip_conntrack line: " + line, lineException);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse /proc/net/ip_conntrack",e);
        } finally {
            if ( br != null ) {
                try {br.close();} catch(Exception ex) {}
            }
        }

        return list;
    }
                            
}
