/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.node.spyware;

import java.util.Iterator;
import java.util.List;

import com.untangle.uvm.tapi.AbstractEventHandler;
import com.untangle.uvm.tapi.IPNewSessionRequest;
import com.untangle.uvm.tapi.MPipeException;
import com.untangle.uvm.tapi.Session;
import com.untangle.uvm.tapi.TCPNewSessionRequest;
import com.untangle.uvm.tapi.UDPNewSessionRequest;
import com.untangle.uvm.tapi.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.tapi.event.TCPSessionEvent;
import com.untangle.uvm.tapi.event.UDPNewSessionRequestEvent;
import com.untangle.uvm.tapi.event.UDPSessionEvent;
import com.untangle.uvm.node.IPMaddr;
import com.untangle.uvm.node.IPMaddrRule;
import com.untangle.node.util.IPSet;
import com.untangle.node.util.IPSetTrie;
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

    public void subnetList(List list)
    {
        if (null == list) {
            subnetSet = null;
        } else {
            IPSetTrie set = new IPSetTrie();

            for (Iterator i = list.iterator(); i.hasNext(); ) {
                IPMaddrRule se = (IPMaddrRule)i.next();
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

        node.incrementCount(Spyware.SCAN);

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
            node.incrementCount(Spyware.BLOCK); //XXX logged but not blocked (count as blocked anyway)???
            if (ipr instanceof TCPNewSessionRequest)
                ((TCPNewSessionRequest)ipr).rejectReturnRst(true);
            if (ipr instanceof UDPNewSessionRequest)
                ipr.rejectReturnUnreachable(IPNewSessionRequest.PROHIBITED,true);
            return;
        }

        if (ir.getAlert()) {
            /* XXX alerts */
        }

        if (release) { ipr.release(true); }
    }
}
