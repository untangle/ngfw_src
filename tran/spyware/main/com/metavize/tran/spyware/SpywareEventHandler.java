/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpywareEventHandler.java,v 1.8 2005/03/15 02:11:52 amread Exp $
 */
package com.metavize.tran.spyware;


import java.util.Iterator;
import java.util.List;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.AbstractEventHandler;
import com.metavize.mvvm.tapi.IPNewSessionRequest;
import com.metavize.mvvm.tapi.TCPNewSessionRequest;
import com.metavize.mvvm.tapi.UDPNewSessionRequest;
import com.metavize.mvvm.tapi.event.TCPNewSessionRequestEvent;
import com.metavize.mvvm.tapi.event.UDPNewSessionRequestEvent;
import com.metavize.mvvm.tran.IPMaddr;
import com.metavize.mvvm.tran.IPMaddrRule;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.util.IPSet;
import com.metavize.tran.util.IPSetTrie;
import org.apache.log4j.Logger;

public class SpywareEventHandler extends AbstractEventHandler
{
    private static final Logger logger = Logger
        .getLogger(SpywareEventHandler.class.getName());

    private final Logger eventLogger = MvvmContextFactory
        .context().eventLogger();

    private final SpywareImpl transform;

    private IPSet subnetSet  = null;

    public SpywareEventHandler(SpywareImpl transform)
    {
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

    void detectSpyware(IPNewSessionRequest ipr, boolean release)
    {
        IPMaddr ipm = new IPMaddr(ipr.serverAddr().getHostAddress());

        IPMaddrRule ir = (IPMaddrRule)this.subnetSet.getMostSpecific(ipm);

        transform.incrementCount(Transform.GENERIC_0_COUNTER); // SCAN COUNTER

        if (ir == null) {
            logger.debug("Subnet scan: " + ipm.toString() + " -> clean.");
            if (release) { ipr.release(); }
            return;
        }

        logger.debug("Subnet scan: " + ipm.toString() + " -> DETECTED.");
        transform.incrementCount(Transform.GENERIC_1_COUNTER); // DETECT COUNTER

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

        eventLogger.info(new SpywareAccessEvent(ipr.id(), ir.getName(), ir.getIpMaddr(), ir.isLive()));

        if (ir.isLive()) {
            transform.incrementCount(Transform.GENERIC_2_COUNTER); // BLOCK COUNTER
            if (ipr instanceof TCPNewSessionRequest)
                ((TCPNewSessionRequest)ipr).rejectReturnRst();
            if (ipr instanceof UDPNewSessionRequest)
                ipr.rejectReturnUnreachable(IPNewSessionRequest.PROHIBITED);
            return;
        }


        if (ir.getAlert()) {
            /* XXX alerts */
        }

        if (release) { ipr.release(); }
    }
}
