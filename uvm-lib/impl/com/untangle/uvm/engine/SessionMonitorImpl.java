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
import com.untangle.uvm.vnet.IPSessionDesc;
import com.untangle.uvm.ConntrackSession;
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

    public List<IPSessionDesc> getNodeSessions(NodeId id)
    {
        NodeManager nodeManager = uvmContext.nodeManager();

        NodeContext nodeContext = nodeManager.nodeContext(id);

        return nodeContext.liveSessionDescs();
    }

    @SuppressWarnings("unchecked") //JSON
    public List<ConntrackSession> getConntrackSessions()
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
            
            return (List<ConntrackSession>) serializer.fromJSON(jsonString.toString());
            
        } catch (java.io.IOException exc) {
            logger.error("Unable to read conntrack - error reading input",exc);
            return null;
        } catch (org.jabsorb.serializer.UnmarshallException exc) {
            logger.error("Unable to read conntrack - invalid JSON",exc);
            return null;
        }
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
    

}
