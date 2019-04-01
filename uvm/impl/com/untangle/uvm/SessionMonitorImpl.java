/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.HashMap;
import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.log4j.Logger;

import com.untangle.uvm.SessionMonitor;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SessionMonitorEntry;
import com.untangle.uvm.app.App;
import com.untangle.uvm.app.SessionTuple;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.app.SessionEvent;

/**
 * SessionMonitor is a utility class that provides some convenient
 * functions to monitor and view current sessions and state existing
 * in the untangle-vm
 * This is used by the UI to display state
 */
public class SessionMonitorImpl implements SessionMonitor
{
    private final Logger logger = Logger.getLogger(getClass());

    public static final short PROTO_TCP = 6;
    public static final short PROTO_UDP = 17;

    private static ExecManager execManager = null;

    UvmContext uvmContext;

    /**
     * SessionMonitorImpl constructor
     */
    public SessionMonitorImpl ()
    {
        SessionMonitorImpl.execManager = UvmContextFactory.context().createExecManager();
        uvmContext = UvmContextFactory.context();
    }

    /**
     * This returns a list of descriptors for all sessions in the conntrack table
     * It also pulls the list of current "pipelines" from the foundry and adds the UVM informations
     * such as policy
     * @return list
     */
    public List<SessionMonitorEntry> getMergedSessions()
    {
        return getMergedSessions(0);
    }

    /**
     * This returns a list of descriptors for all sessions in the conntrack table
     * It also pulls the list of current "pipelines" from the foundry and adds the UVM informations
     * such as policy. This only lists sessions being processed by the given appId
     * If appId == 0, then getMergedSessions() is returned
     * @param appId
     * @return list
     */
    public List<SessionMonitorEntry> getMergedSessions(long appId)
    {
        List<SessionMonitorEntry> sessions = this._getConntrackSessionMonitorEntrys();
        List<AppSession> appSessions = null;;

        App app = null;
        if (appId > 0)
            app = UvmContextFactory.context().appManager().app(appId);
        if (app != null)
            appSessions = app.liveAppSessions();

        for (Iterator<SessionMonitorEntry> i = sessions.iterator(); i.hasNext(); ) {
            SessionMonitorEntry session = i.next();

            session.setPolicy("");
            if (session.getClientIntf() == null || session.getClientIntf() == 0 ) {
                session.setClientIntf(Integer.valueOf(-1));
            }
            if ( session.getServerIntf() == null || session.getServerIntf() == 0 ) {
                session.setServerIntf(Integer.valueOf(-1));
            }
            if ( session.getHostname() == null || session.getHostname().length() == 0 ) {
                session.setHostname( SessionEvent.determineBestHostname( session.getPreNatClient(), session.getClientIntf(), session.getPostNatServer(), session.getServerIntf() ) );
            }
            session.setPriority(session.getQosPriority());

            SessionTuple tuple = _makeTuple( session );
            // find corresponding session in UVM
            SessionGlobalState sessionState = SessionTableImpl.getInstance().lookupTuple(tuple);
            ConntrackMonitorImpl.ConntrackEntryState conntrackState = ConntrackMonitorImpl.getInstance().lookupTuple(tuple);

            if ( logger.isDebugEnabled() )
                logger.debug("Lookup session table (" + tuple + ") -> " + sessionState);
            if ( logger.isDebugEnabled() )
                logger.debug("Lookup conntrack table (" + tuple + ") -> " + conntrackState);

            if ( conntrackState != null ) {
                session.setClientKBps( conntrackState.c2sRateBps/1000.0f );
                session.setServerKBps( conntrackState.s2cRateBps/1000.0f );
                session.setTotalKBps( conntrackState.totalRateBps/1000.0f );
            }

            if ( sessionState != null ) {
                try {
                    int priority = sessionState.netcapSession().clientQosMark();
                    com.untangle.uvm.app.SessionTuple clientSide = sessionState.netcapHook().getClientSide();
                    com.untangle.uvm.app.SessionTuple serverSide = sessionState.netcapHook().getServerSide();

                    NetcapHook hook = sessionState.netcapHook();
                    if (hook == null)
                        continue;

                    Integer policyId = hook.getPolicyId();
                    if (policyId == null)
                        session.setPolicy("");
                    else
                        session.setPolicy(policyId.toString());

                    session.setSessionId(sessionState.id());
                    session.setCreationTime(sessionState.getCreationTime());
                    session.setPipeline(sessionState.getPipelineDescription());
                    session.setBypassed(Boolean.FALSE);
                    session.setClientIntf(new Integer(sessionState.getClientIntf()));
                    session.setServerIntf(new Integer(sessionState.getServerIntf()));
                    session.setHostname(sessionState.getSessionEvent().getHostname());
                    session.setUsername(sessionState.getSessionEvent().getUsername());

                    session.setClientCountry(sessionState.getSessionEvent().getClientCountry());
                    session.setClientLatitude(sessionState.getSessionEvent().getClientLatitude());
                    session.setClientLongitude(sessionState.getSessionEvent().getClientLongitude());

                    session.setServerCountry(sessionState.getSessionEvent().getServerCountry());
                    session.setServerLatitude(sessionState.getSessionEvent().getServerLatitude());
                    session.setServerLongitude(sessionState.getSessionEvent().getServerLongitude());

                    session.setTags(sessionState.getTags());
                    session.setTagsString(sessionState.getTagsString());

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

                    session.setAttachments(sessionState.getAttachments());
                } catch (Exception e) {
                    logger.warn("Exception while searching for session",e);
                }
            }
            // else sessionState == null (no UVM session found)
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

            if ( UvmContextFactory.context().networkManager().isWanInterface( session.getClientIntf() ) ) {
                session.setLocalAddr( session.getPostNatServer() );
                session.setRemoteAddr( session.getPreNatClient() );
            } else {
                session.setLocalAddr( session.getPreNatClient() );
                session.setRemoteAddr( session.getPostNatServer() );
            }

            /**
             * Ignore sessions to 192.0.2.200
             */
            if ( "192.0.2.200".equals( session.getPostNatServer().getHostAddress() ) ) {
                logger.debug("Removing session from view (internal session to 192.0.2.200): " + session);
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
         * If a appId was specified remove all sessions not being touched by that appId
         */
        if ( appSessions != null ) {
            for (Iterator<SessionMonitorEntry> i = sessions.iterator(); i.hasNext(); ) {
                SessionMonitorEntry entry = i.next();
                Long sessionId = entry.getSessionId();
                boolean found = false;
                for (AppSession ns : appSessions) {
                    if ( sessionId != null && sessionId == ns.getSessionId() ) {
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
     * This is a JSON object with some keys to store values such as totalSessions, scannedSession, etc.
     * @return JSONObject
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
     * Retrieve the session stats by policy id
     * This is a JSON object which has policy ids as keys and sessions count, total kbps for each policy
     * @return JSONObject
     */
    public org.json.JSONObject getPoliciesSessionsStats()
    {
        List<SessionMonitorEntry> sessions = getMergedSessions();
        org.json.JSONObject json = new org.json.JSONObject();

        Map<String, Float> totals = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();

        try {
            if ( sessions != null ) {
                for (SessionMonitorEntry entry: sessions) {
                    if ( entry == null ) continue;
                    String policy = entry.getPolicy();
                    if ( policy != null ) {
                        if ( totals.containsKey(policy)) {
                            Float existing = totals.get(policy);
                            totals.put(policy, entry.getTotalKBps() != null ? existing + entry.getTotalKBps() : existing);
                        } else
                            totals.put(policy, entry.getTotalKBps() != null ? entry.getTotalKBps() : 0.0f);
                        if ( counts.containsKey(policy)) {
                            Integer existing = counts.get(policy);
                            counts.put(policy, existing + 1);
                        } else
                            counts.put(policy, 1);
                    }
                }

                for (String key: totals.keySet()) {
                    org.json.JSONObject entry = new org.json.JSONObject();
                    entry.put("totalKbps", totals.get(key));
                    entry.put("sessionCount", counts.get(key));
                    json.put(key, entry);
                }
            }
        } catch (Exception e) {
            logger.error("Error generating session stats by", e);
        }

        return json;
    }

    /**
     * This returns a list of sessions and bandwidth usages reported by jnettop over 5 seconds
     * This takes 5 seconds to gather data before it returns
     * @param systemIntfName
     * @return list
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
     * @return list
     */
    private List<SessionMonitorEntry> _getConntrackSessionMonitorEntrys()
    {
        return parseProcNetIpConntrack();
    }

    /**
     * Make a SessionTuple
     * @param protocolStr
     * @param clientIntf
     * @param serverIntf
     * @param preNatClient
     * @param preNatServer
     * @param preNatClientPort
     * @param preNatServerPort
     * @return SessionTuple
     */
    private SessionTuple _makeTuple( String protocolStr, int clientIntf, int serverIntf, InetAddress preNatClient, InetAddress preNatServer, int preNatClientPort, int preNatServerPort )
    {
        short protocol;
        if ( "TCP".equals(protocolStr) )
            protocol = PROTO_TCP;
        else if ( "UDP".equals(protocolStr) )
            protocol = PROTO_UDP;
        else {
            logger.warn("Unknown protocol: " + protocolStr);
            protocol = 0;
        }
        return new SessionTuple( protocol, preNatClient, preNatServer, preNatClientPort, preNatServerPort );
    }

    /**
     * Make a tuple from a SessionMonitorEntry
     * @param session
     * @return SessionTuple
     */
    private SessionTuple _makeTuple( SessionMonitorEntry session )
    {
        short protocol;
        if ( "TCP".equals(session.getProtocol()) )
            protocol = PROTO_TCP;
        else if ( "UDP".equals(session.getProtocol()) )
            protocol = PROTO_UDP;
        else {
            logger.warn("Unknown protocol: " + session.getProtocol());
            protocol = 0;
        }
        return new SessionTuple( protocol,
                                     session.getPreNatClient(),
                                     session.getPreNatServer(),
                                     session.getPreNatClientPort(),
                                     session.getPreNatServerPort() );
    }

    /**
     * makeTuple copies a tuple
     * @param tuple
     * @return SessionTuple
     */
    private SessionTuple _makeTuple( SessionTuple tuple )
    {
        return new SessionTuple( tuple );
    }

    /**
     * Parse proc/net/nf_conntrack
     * and return a list of SessionMonitorEntry
     * @return the list of sessions with the conntrack information
     */
    private List<SessionMonitorEntry> parseProcNetIpConntrack()
    {
        BufferedReader br = null;
        String line;
        LinkedList<SessionMonitorEntry> list = new LinkedList<>();
        String conntrackFilename;
        if ( Files.exists(Paths.get("/proc/net/ip_conntrack")) )
            conntrackFilename = "/proc/net/ip_conntrack";
        else
            conntrackFilename = "/proc/net/nf_conntrack";

        try {
            br = new BufferedReader(new FileReader(conntrackFilename));
            while ((line = br.readLine()) != null) {
                try {
                    if ( logger.isDebugEnabled() )
                        logger.debug("parseProcNetIpConntrack line: " + line);
                    String[] parts = line.split("\\s+");
                    SessionMonitorEntry newEntry = new SessionMonitorEntry();

                    // if using the new nf_conntrack, remove first two fields
                    if ( conntrackFilename == "/proc/net/nf_conntrack" ) {
                        String[] newArray=new String[parts.length];
                        System.arraycopy(parts,2,newArray,0,parts.length-2);
                        parts = newArray;
                    }

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
                        if ( part == null )
                            continue;
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
                            newEntry.setQosPriority( (mark & 0x000F0000) >> 16 );
                            newEntry.setClientIntf( (mark & 0x000000FF) );
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
