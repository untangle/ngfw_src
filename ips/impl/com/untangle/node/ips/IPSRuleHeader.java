/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
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

package com.untangle.node.ips;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;

import com.untangle.uvm.node.PortRange;
import com.untangle.uvm.node.SessionEndpoints;
import com.untangle.uvm.node.firewall.ip.IPMatcher;
import com.untangle.uvm.node.firewall.ip.IPMatcherFactory;
import com.untangle.uvm.vnet.Protocol;
import org.apache.log4j.Logger;

public class IPSRuleHeader
{
    public static final boolean IS_BIDIRECTIONAL = true;
    public static final boolean IS_SERVER = true;

    private final int action;
    private final Protocol protocol;

    private final List<IPMatcher> clientIPList;
    private final PortRange clientPortRange;

    private final boolean bidirectional;

    private final List<IPMatcher> serverIPList;
    private final PortRange serverPortRange;

    private final Logger logger = Logger.getLogger(getClass());

    private final boolean clientIPFlag;
    private final boolean clientPortFlag;
    private final boolean serverIPFlag;
    private final boolean serverPortFlag;

    // constructors ------------------------------------------------------------

    private IPSRuleHeader(int action, boolean bidirectional, Protocol protocol,
                          List<IPMatcher> clientIPList,
                          PortRange clientPortRange,
                          List<IPMatcher> serverIPList,
                          PortRange serverPortRange,
                          boolean clientIPFlag, boolean clientPortFlag,
                          boolean serverIPFlag, boolean serverPortFlag)
    {

        this.action = action;
        this.bidirectional = bidirectional;
        this.protocol = protocol;
        this.clientIPList = clientIPList;
        this.serverIPList = serverIPList;

        this.clientPortRange = clientPortRange;
        this.serverPortRange = serverPortRange;


        this.clientIPFlag = clientIPFlag;
        this.clientPortFlag = clientPortFlag;
        this.serverIPFlag = serverIPFlag;
        this.serverPortFlag = serverPortFlag;
    }

    // static methods ----------------------------------------------------------

    public static IPSRuleHeader getHeader(int action, boolean bidirectional,
                                          Protocol protocol,
                                          List<IPMatcher> clientIPList,
                                          PortRange clientPortRange,
                                          List<IPMatcher> serverIPList,
                                          PortRange serverPortRange,
                                          boolean clientIPFlag,
                                          boolean clientPortFlag,
                                          boolean serverIPFlag,
                                          boolean serverPortFlag)

    {
        return new IPSRuleHeader(action, bidirectional, protocol, clientIPList,
                                 clientPortRange, serverIPList,serverPortRange,
                                 clientIPFlag, clientPortFlag,
                                 serverIPFlag, serverPortFlag);
    }

    // public methods ----------------------------------------------------------

    public boolean portMatches(int port, boolean toServer)
    {
        if(toServer)
            return serverPortFlag ^ serverPortRange.contains(port);
        else
            return clientPortFlag ^ clientPortRange.contains(port);
    }

    public boolean matches(SessionEndpoints sess, boolean sessInbound, boolean forward)
    {
        return matches(sess, sessInbound, forward, false);
    }

    private boolean matches(SessionEndpoints sess, boolean sessInbound, boolean forward, boolean swapFlag)
    {
        if(this.protocol != Protocol.getInstance(sess.protocol()))
            return false;

        // logger.debug("protocol match succeeded");

        /**Check Port Match*/
        boolean clientPortMatch = clientPortRange.contains(forward ? sess.clientPort() : sess.serverPort());
        boolean serverPortMatch = serverPortRange.contains(forward ? sess.serverPort() : sess.clientPort());

        boolean portMatch = (clientPortMatch ^ clientPortFlag) && (serverPortMatch ^ serverPortFlag);

        /*  if(!portMatch && !bidirectional)
            {
            System.out.println();
            System.out.println("Header: " + this);
            System.out.println("ClientPort: " + clientPort);
            System.out.println("ServerPort: " + serverPort);
            System.out.println();
            }*/

        if(!portMatch && !bidirectional)
            return false;

        // logger.debug("port match succeeded");

        boolean isInbound = forward ? sessInbound : !sessInbound;

        /**Check IP Match*/
        InetAddress cAddr = forward ? sess.clientAddr() : sess.serverAddr();
        boolean clientIPMatch = false;
        Iterator<IPMatcher> clientIt = clientIPList.iterator();

        IPMatcherFactory ipmf = IPMatcherFactory.getInstance();
        IPMatcher internalMatcher = ipmf.getInternalMatcher();
        IPMatcher externalMatcher = ipmf.getExternalMatcher();

        while(clientIt.hasNext() && !clientIPMatch) {
            IPMatcher matcher = clientIt.next();
            if (matcher == externalMatcher)
                clientIPMatch = isInbound;
            else if (matcher == internalMatcher)
                clientIPMatch = !isInbound;
            else
                clientIPMatch = matcher.isMatch(cAddr);
            // logger.debug("client matcher: " + matcher + " sez: " + clientIPMatch);
        }

        InetAddress sAddr = forward ? sess.serverAddr() : sess.clientAddr();
        boolean serverIPMatch = false;
        Iterator<IPMatcher> serverIt = serverIPList.iterator();
        while(serverIt.hasNext() && !serverIPMatch)
            {
                IPMatcher matcher = serverIt.next();
                if (matcher == externalMatcher)
                    serverIPMatch = !isInbound;
                else if (matcher == internalMatcher)
                    serverIPMatch = isInbound;
                else
                    serverIPMatch = matcher.isMatch(sAddr);
                // logger.debug("server matcher: " + matcher + " sez: " + serverIPMatch);
            }
        boolean ipMatch = (clientIPMatch ^ clientIPFlag) && (serverIPMatch ^ serverIPFlag);

        // logger.debug("ip match: " + ipMatch);

        /**Check Directional flag*/
        if(!(ipMatch && portMatch) && bidirectional && !swapFlag)
            {
                return matches(sess, sessInbound, !forward, true);
            }

        /*  if(!(ipMatch && portMatch))
            {
            System.out.println();
            System.out.println("Header: " + this);
            System.out.println("ClientIP: " + clientAddr);
            System.out.println("ServerIP: " + serverAddr);
            System.out.println();
            }*/

        return ipMatch && portMatch;
    }

    public int getAction()
    {
        return action;
    }

    // Rule manager uses this to decide if the rule is already
    // known.
    public boolean matches(IPSRuleHeader other)
    {
        boolean action = (this.action == other.action);
        boolean protocol = (this.protocol == other.protocol); // ?
        boolean clientPorts = (this.clientPortRange.equals(other.clientPortRange));
        boolean serverPorts = (this.serverPortRange.equals(other.serverPortRange));
        boolean serverIP = (this.serverIPList.equals(other.serverIPList));
        boolean clientIP = (this.serverIPList.equals(other.serverIPList));
        boolean direction = (this.bidirectional == other.bidirectional);

        return action && protocol && clientPorts && serverPorts && serverIP && clientIP && direction;
    }

    // Object methods ----------------------------------------------------------

    public String toString()
    {
        String str = "alert "+protocol+" ";
        if(clientIPFlag)
            str += "!";
        str += clientIPList + " ";
        if(clientPortFlag)
            str += "!";
        str += clientPortRange;
        if(bidirectional)
            str += " <> ";
        else
            str += " -> ";
        if(serverIPFlag)
            str += "!";
        str += serverIPList +" ";
        if(serverPortFlag)
            str += "!";
        str += serverPortRange;
        return str;
    }
}
