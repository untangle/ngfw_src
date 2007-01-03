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
package com.untangle.tran.spyware;

import java.util.Iterator;
import java.util.List;

import com.untangle.mvvm.tapi.AbstractEventHandler;
import com.untangle.mvvm.tapi.IPNewSessionRequest;
import com.untangle.mvvm.tapi.MPipeException;
import com.untangle.mvvm.tapi.Session;
import com.untangle.mvvm.tapi.TCPNewSessionRequest;
import com.untangle.mvvm.tapi.UDPNewSessionRequest;
import com.untangle.mvvm.tapi.event.TCPNewSessionRequestEvent;
import com.untangle.mvvm.tapi.event.TCPSessionEvent;
import com.untangle.mvvm.tapi.event.UDPNewSessionRequestEvent;
import com.untangle.mvvm.tapi.event.UDPSessionEvent;
import com.untangle.mvvm.tran.IPMaddr;
import com.untangle.mvvm.tran.IPMaddrRule;
import com.untangle.tran.util.IPSet;
import com.untangle.tran.util.IPSetTrie;
import org.apache.log4j.Logger;

public class SpywareEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());

    private final SpywareImpl transform;

    private IPSet subnetSet  = null;

    public SpywareEventHandler(SpywareImpl transform)
    {
        super(transform);

        this.transform = transform;
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
            transform.statisticManager.incrSubnetAccess(); // logged subnet access
            transform.log(spe);
        } else {
            transform.statisticManager.incrPass(); // pass subnet access
        }
    }

    @Override
    public void handleUDPComplete(UDPSessionEvent event)
        throws MPipeException
    {
        Session s = event.session();
        SpywareAccessEvent spe = (SpywareAccessEvent)s.attachment();
        if (null != spe) {
            transform.statisticManager.incrSubnetAccess(); // logged subnet access
            transform.log(spe);
        } else {
            transform.statisticManager.incrPass(); // pass subnet access
        }
    }

    void detectSpyware(IPNewSessionRequest ipr, boolean release)
    {
        IPMaddr ipm = new IPMaddr(ipr.serverAddr().getHostAddress());

        IPMaddrRule ir = (IPMaddrRule)this.subnetSet.getMostSpecific(ipm);

        if (ir == null) {
            transform.statisticManager.incrPass(); // pass subnet access
            if (logger.isDebugEnabled()) {
                logger.debug("Subnet scan: " + ipm.toString() + " -> clean.");
            }
            if (release) { ipr.release(); }
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Subnet scan: " + ipm.toString() + " -> DETECTED.");
        }

        transform.incrementCount(Spyware.SCAN);

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

        if (release)
            ipr.attach(new SpywareAccessEvent(ipr.pipelineEndpoints(), ir.getName(), ir.getIpMaddr(), ir.isLive()));
        else {
            transform.statisticManager.incrSubnetAccess(); // logged subnet access
            transform.log(new SpywareAccessEvent(ipr.pipelineEndpoints(), ir.getName(), ir.getIpMaddr(), ir.isLive()));
        }

        if (ir.isLive()) {
            transform.incrementCount(Spyware.BLOCK); //XXX logged but not blocked (count as blocked anyway)???
            if (ipr instanceof TCPNewSessionRequest)
                ((TCPNewSessionRequest)ipr).rejectReturnRst();
            if (ipr instanceof UDPNewSessionRequest)
                ipr.rejectReturnUnreachable(IPNewSessionRequest.PROHIBITED);
            return;
        }

        if (ir.getAlert()) {
            /* XXX alerts */
        }

        if (release) { ipr.release(true); }
    }
}
