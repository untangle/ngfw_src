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
package com.untangle.node.spyware;

import java.util.Iterator;
import java.util.Set;

import com.untangle.node.util.IPSet;
import com.untangle.node.util.IPSetTrie;
import com.untangle.uvm.node.IPMaddr;
import com.untangle.uvm.node.IPMaddrRule;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.MPipeException;
import com.untangle.uvm.vnet.Session;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.UDPNewSessionRequest;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.UDPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.UDPSessionEvent;
import org.apache.log4j.Logger;

public class SpywareEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());

    private final SpywareImpl node;

    private IPSet subnetSet  = null;

    public SpywareEventHandler(SpywareImpl node)
    {
        super(node);

        this.node = node;
    }

    public void subnetList(Set<IPMaddrRule> list)
    {
        if (null == list) {
            subnetSet = null;
        } else {
            IPSetTrie set = new IPSetTrie();

            for (Iterator<IPMaddrRule> i = list.iterator(); i.hasNext(); ) {
                IPMaddrRule se = i.next();
                IPMaddr ipm = se.getIpMaddr();
                set.add(ipm,(Object)se);
            }

            this.subnetSet = set;
        }
    }

    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event)
    {
        if (null != subnetSet) {
            detectSpyware(event.sessionRequest(), true);
        } else {
            logger.debug("spyware detection disabled");
        }
    }

    public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent event)
    {
        if (null != subnetSet) {
            detectSpyware(event.sessionRequest(), true);
        } else {
            logger.debug("spyware detection disabled");
        }
    }

    @Override
    public void handleTCPComplete(TCPSessionEvent event)
        throws MPipeException
    {
        Session s = event.session();
        SpywareAccessEvent spe = (SpywareAccessEvent)s.attachment();
        if (null != spe) {
            node.statisticManager.incrSubnetAccess(); // logged subnet access
            node.log(spe);
        } else {
            node.statisticManager.incrPass(); // pass subnet access
        }
    }

    @Override
    public void handleUDPComplete(UDPSessionEvent event)
        throws MPipeException
    {
        Session s = event.session();
        SpywareAccessEvent spe = (SpywareAccessEvent)s.attachment();
        if (null != spe) {
            node.statisticManager.incrSubnetAccess(); // logged subnet access
            node.log(spe);
        } else {
            node.statisticManager.incrPass(); // pass subnet access
        }
    }

    void detectSpyware(IPNewSessionRequest ipr, boolean release)
    {
        IPMaddr ipm = new IPMaddr(ipr.serverAddr().getHostAddress());

        IPMaddrRule ir = (IPMaddrRule)this.subnetSet.getMostSpecific(ipm);

        if (ir == null) {
            node.statisticManager.incrPass(); // pass subnet access
            if (logger.isDebugEnabled()) {
                logger.debug("Subnet scan: " + ipm.toString() + " -> clean.");
            }
            if (release) { ipr.release(); }
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Subnet scan: " + ipm.toString() + " -> DETECTED.");
        }

        node.incrementSubnetScan();

        if (logger.isInfoEnabled()) {
            logger.info("-------------------- Detected Spyware --------------------");
            logger.info("Spyware Name  : " + ir.getName());
            logger.info("Host          : " + ipr.clientAddr().getHostAddress() + ":" + ipr.clientPort());
            logger.info("Suspicious IP : " + ipr.serverAddr().getHostAddress() + ":" + ipr.serverPort());
            logger.info("Matches       : " + ir.getIpMaddr());
            if (ipr instanceof TCPNewSessionRequest)
                logger.info("Protocol      : TCP");
            if (ipr instanceof UDPNewSessionRequest)
                logger.info("Protocol      : UDP");
            logger.info("----------------------------------------------------------");
        }

        ipr.attach(new SpywareAccessEvent(ipr.pipelineEndpoints(), ir.getName(), ir.getIpMaddr(), ir.isLive()));

        if (ir.isLive()) {
            node.incrementSubnetBlock();
            if (ipr instanceof TCPNewSessionRequest) {
                ((TCPNewSessionRequest)ipr).rejectReturnRst(true);
            }
            if (ipr instanceof UDPNewSessionRequest) {
                ipr.rejectReturnUnreachable(IPNewSessionRequest.PROHIBITED,true);
            }
            return;
        }

        if (release) { ipr.release(true); }
    }
}
