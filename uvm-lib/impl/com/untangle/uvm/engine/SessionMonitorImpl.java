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
import com.untangle.uvm.SessionMonitorEntry;
import com.untangle.uvm.security.NodeId;

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
    public List<com.untangle.uvm.vnet.IPSessionDesc> getNodeSessions(NodeId id)
    {
        NodeManager nodeManager = uvmContext.nodeManager();

        NodeContext nodeContext = nodeManager.nodeContext(id);

        return nodeContext.liveSessionDescs();
    }

    /**
     * This returns a list of descriptors for all sessions in the conntrack table
     */
    @SuppressWarnings("unchecked") //JSON
    public List<SessionMonitorEntry> getSessionMonitorEntrys()
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
            
            return (List<SessionMonitorEntry>) serializer.fromJSON(jsonString.toString());
            
        } catch (java.io.IOException exc) {
            logger.error("Unable to read conntrack - error reading input",exc);
            return null;
        } catch (org.jabsorb.serializer.UnmarshallException exc) {
            logger.error("Unable to read conntrack - invalid JSON",exc);
            return null;
        }
    }

    /**
     * This returns a list of descriptors for all sessions in the conntrack table
     * It also pulls the list of current "pipelines" from the foundry and adds the UVM informations
     * such as policy
     */
    @SuppressWarnings("unchecked") //JSON
    public List<SessionMonitorEntry> getMergedSessionMonitorEntrys()
    {
        List<PipelineImpl> pipelines = ((PipelineFoundryImpl) uvmContext.pipelineFoundry()).getCurrentPipelines();
        List<SessionMonitorEntry> sessions = this.getSessionMonitorEntrys();

        logger.warn("Checking Pipelines");
        
        for (SessionMonitorEntry session : sessions) {
            //assume bypassed until we find a match in the UVM
            session.setBypassed(Boolean.TRUE); 
            session.setPolicy("");             
            session.setClientIntf(Integer.valueOf(-1));
            session.setServerIntf(Integer.valueOf(-1));

            for (PipelineImpl pipeline : pipelines) {
                com.untangle.uvm.node.IPSessionDesc sessionDesc = pipeline.getSessionDesc();

                if (matches(sessionDesc,session)) {
                    session.setPolicy(pipeline.getPolicy().getName());
                    session.setBypassed(Boolean.FALSE);
                    session.setClientIntf(new Integer(sessionDesc.clientIntf()));
                    session.setServerIntf(new Integer(sessionDesc.serverIntf()));
                    break;
                }
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

    private boolean matches(com.untangle.uvm.node.IPSessionDesc sessionDesc, SessionMonitorEntry session)
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

}
