/*
 * $HeadURL: svn://chef/work/src/uvm-lib/impl/com/untangle/uvm/engine/AddressBookFactory.java $
 * Copyright (c) 2003-2010 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.util.List;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Iterator;
import java.net.InetAddress;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import org.apache.log4j.Logger;
import org.jabsorb.JSONSerializer;

import com.untangle.uvm.SessionMonitor;
import com.untangle.uvm.UvmState;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.SessionEndpoints;
import com.untangle.uvm.argon.SessionGlobalState;
import com.untangle.uvm.argon.ArgonHook;
import com.untangle.uvm.argon.ArgonSessionTable;
import com.untangle.uvm.SessionMonitorEntry;
import com.untangle.uvm.security.NodeId;
import com.untangle.uvm.networking.InterfaceSettings;

/**
 * SessionMonitor is a utility class that provides some convenient functions
 * to monitor and view current sessions and state existing in the untangle-vm
 *
 * This is used by the UI to display state
 */
class SessionMonitorImpl implements SessionMonitor
{
    private final Logger logger = Logger.getLogger(getClass());

    private static JSONSerializer serializer = null;

    LocalUvmContext uvmContext;
    
    public SessionMonitorImpl ()
    {
        uvmContext = LocalUvmContextFactory.context();
    }

    /**
     * This returns a list of descriptors for a certain node
     */
    public List<com.untangle.uvm.vnet.VnetSessionDesc> getNodeSessions(NodeId id)
    {
        NodeManager nodeManager = uvmContext.nodeManager();

        NodeContext nodeContext = nodeManager.nodeContext(id);

        return nodeContext.liveSessionDescs();
    }

    /**
     * documented in SessionMonitor.java
     */
    public List<SessionMonitorEntry> getMergedBandwidthSessions(String interfaceIdStr)
    {
        /**
         * Find the the system interface name that matches this ID
         * XXX this should be in a utility somewhere
         */
        List<InterfaceSettings> intfs = uvmContext.networkManager().getNetworkSettings().getInterfaceList();
        try {
            int interfaceId = Integer.parseInt(interfaceIdStr);
        
            for (InterfaceSettings intf : intfs) {

                if (((int)intf.getInterfaceId()) == interfaceId) {
                    String systemName = intf.getSystemName();

                    return _getMergedBandwidthSessions(systemName);
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to retrieve sessions",e);
            return null;
        }

        logger.warn("Unable to find match for interface " + interfaceIdStr);
        return null;
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
        List<SessionMonitorEntry> sessions = this._getConntrackSessionMonitorEntrys();
        List<SessionGlobalState> argonSessions = ArgonSessionTable.getInstance().getSessions();

        for (Iterator i = sessions.iterator(); i.hasNext(); ) {  
            SessionMonitorEntry session = (SessionMonitorEntry) i.next();
            session.setPolicy("");             
            session.setClientIntf(Integer.valueOf(-1));
            session.setServerIntf(Integer.valueOf(-1));
            session.setPriority(session.getQosPriority()); 
            boolean foundUvmSession = false;
            
            for (SessionGlobalState argonSession : argonSessions) {
                com.untangle.uvm.node.IPSessionDesc clientSide = argonSession.argonHook().getClientSide();
                com.untangle.uvm.node.IPSessionDesc serverSide = argonSession.argonHook().getServerSide();
                int priority = argonSession.netcapSession().clientQosMark();

                try {
                    if ( _matches(clientSide,session) || _matches(serverSide,session) ) {
                        session.setPolicy(argonSession.argonHook().getPolicy().getName());
                        session.setBypassed(Boolean.FALSE);
                        session.setLocalTraffic(Boolean.FALSE);
                        session.setClientIntf(new Integer(clientSide.clientIntf()));
                        session.setServerIntf(new Integer(serverSide.serverIntf()));

                        /**
                         * The conntrack entry shows that this session has been redirect to the local host
                         * We need to overwrite that with the correct info
                         */
                        session.setPostNatClient(serverSide.clientAddr());
                        session.setPostNatServer(serverSide.serverAddr());
                        session.setPostNatClientPort(serverSide.clientPort());
                        session.setPostNatServerPort(serverSide.serverPort());

                        /**
                         * Only have one priority per session
                         * Assume both client and server are the same
                         */
                        if (priority != 0)
                            session.setPriority(priority);
                        
                        foundUvmSession = true;
                        break;
                    }
                } catch (Exception e) {
                    /* sometimes argonHook or getPolicy return null */
                    /* if anything weird happens just assume it doesn't match */
                }
            }

            /**
             * If the session is not bypassed and is not in the UVM
             * Then it is likely some expired session or some local session (blockpages)
             * Remove it and dont show it to the user
             */
            if ( !session.getBypassed() && !foundUvmSession ) {
                logger.info("Removing: " + session);
                i.remove();
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

        return sessions;
    }
    
    /**
     * @param serializer
     *            the serializer to set
     */
    protected void setSerializer(JSONSerializer serializer)
    {
        this.serializer = serializer;
    }

    /**
     * @return the serializer
     */
    protected JSONSerializer getSerializer()
    {
        return serializer;
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

        for (SessionMonitorEntry session : sessions) {

            session.setClientKBps(Float.valueOf(0.0f));
            session.setServerKBps(Float.valueOf(0.0f));
            session.setTotalKBps(Float.valueOf(0.0f));

            for (SessionMonitorEntry jnettopSession : jnettopSessions) {

                boolean match = false;

                /**
                 * Depending on whether the internal and external is source interface jnettop can see either pre or post NAT
                 * and whether jnettop sees a client or server packet first the order it can reverse client and server
                 * We have to check for all combinations for matches
                 * (Bug #8290)
                 */
                if (match || _matches(jnettopSession.getProtocol(),
                                      jnettopSession.getPreNatClient(),jnettopSession.getPreNatServer(),
                                      jnettopSession.getPreNatClientPort(),jnettopSession.getPreNatServerPort(),
                                      session.getProtocol(),
                                      session.getPreNatClient(),session.getPreNatServer(),
                                      session.getPreNatClientPort(),session.getPreNatServerPort()))
                    match = true;
                if (match || _matches(jnettopSession.getProtocol(),
                                      jnettopSession.getPreNatClient(),jnettopSession.getPreNatServer(),
                                      jnettopSession.getPreNatClientPort(),jnettopSession.getPreNatServerPort(),
                                      session.getProtocol(),
                                      session.getPreNatServer(),session.getPreNatClient(),
                                      session.getPreNatServerPort(),session.getPreNatClientPort()))
                    match = true;
                if (match || _matches(jnettopSession.getProtocol(),
                                      jnettopSession.getPreNatClient(),jnettopSession.getPreNatServer(),
                                      jnettopSession.getPreNatClientPort(),jnettopSession.getPreNatServerPort(),
                                      session.getProtocol(),
                                      session.getPostNatClient(),session.getPostNatServer(),
                                      session.getPostNatClientPort(),session.getPostNatServerPort()))
                    match = true;
                if (match || _matches(jnettopSession.getProtocol(),
                                      jnettopSession.getPreNatClient(),jnettopSession.getPreNatServer(),
                                      jnettopSession.getPreNatClientPort(),jnettopSession.getPreNatServerPort(),
                                      session.getProtocol(),
                                      session.getPostNatServer(),session.getPostNatClient(),
                                      session.getPostNatServerPort(),session.getPostNatClientPort()))
                    match = true;

                if ( match ) {
                    session.setClientKBps(jnettopSession.getClientKBps());
                    session.setServerKBps(jnettopSession.getServerKBps());
                    session.setTotalKBps(jnettopSession.getTotalKBps());
                    break; /* break to outer loop */
                }
            }
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
            StringBuilder jsonString = new StringBuilder();
            Process p = uvmContext.exec(execStr);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                    jsonString.append(line);
            }
            
            List<SessionMonitorEntry> entryList = (List<SessionMonitorEntry>) serializer.fromJSON(jsonString.toString());
            return entryList;
            
        } catch (java.io.IOException exc) {
            logger.error("Unable to read jnettop - error reading input",exc);
            return null;
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
        String execStr = new String(System.getProperty("uvm.bin.dir") + "/" + "ut-conntrack");

        try {
            StringBuilder jsonString = new StringBuilder();
            Process p = uvmContext.exec(execStr);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                    jsonString.append(line);
            }
            
            List<SessionMonitorEntry> entryList = (List<SessionMonitorEntry>) serializer.fromJSON(jsonString.toString());
            return entryList;
            
        } catch (java.io.IOException exc) {
            logger.error("Unable to read conntrack - error reading input",exc);
            return null;
        } catch (org.jabsorb.serializer.UnmarshallException exc) {
            logger.error("Unable to read conntrack - invalid JSON",exc);
            return null;
        }
    }

    /**
     * Check if the entry matches the sessionDesc
     * This checks the 5-tuple (protocol, src, dst, src_port, dst_port)
     */
    private boolean _matches(com.untangle.uvm.node.IPSessionDesc sessionDesc, SessionMonitorEntry session)
    {
        switch (sessionDesc.protocol()) {
        case SessionEndpoints.PROTO_TCP:
            if (! "TCP".equals(session.getProtocol())) {
                return false;
            }
            break;
        case SessionEndpoints.PROTO_UDP:
            if (! "UDP".equals(session.getProtocol())) {
                return false;
            }
            break;
        }

        if (! sessionDesc.clientAddr().equals(session.getPreNatClient())) {
            return false;
        }
        if (! sessionDesc.serverAddr().equals(session.getPreNatServer())) {
            return false;
        }

        if (sessionDesc.clientPort() != session.getPreNatClientPort()) {
            return false;
        }
        if (sessionDesc.serverPort() != session.getPreNatServerPort()) {
            return false;
        }

        return true;
    }

    /**
     * This checks first 5-tuple (protocol, src, dst, src_port, dst_port)
     * against the second, returns true if match
     */
    private boolean _matches(String protocol1, InetAddress client1, InetAddress server1, int clientPort1, int serverPort1,
                             String protocol2, InetAddress client2, InetAddress server2, int clientPort2, int serverPort2)

    {
        if (! protocol1.equals(protocol2))
            return false;
        if (! client1.equals(client2))
            return false;
        if (! server1.equals(server2))
            return false;
        if (clientPort1 != clientPort2)
            return false;
        if (serverPort1 != serverPort2)
            return false;

        return true;
    }
                            
                            
}
