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

package com.untangle.node.spam;

import com.untangle.uvm.tapi.AbstractEventHandler;
import com.untangle.uvm.tapi.MPipeException;
import com.untangle.uvm.tapi.Session;
import com.untangle.uvm.tapi.TCPNewSessionRequest;

import com.untangle.uvm.tapi.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.tapi.event.TCPSessionEvent;

import org.apache.log4j.Logger;

class RBLEventHandler extends AbstractEventHandler
{
    private final Logger m_logger = Logger.getLogger(RBLEventHandler.class);

    private SpamImpl m_spamImpl;

    RBLEventHandler(SpamImpl spamImpl)
    {
        super(spamImpl);

        this.m_spamImpl = spamImpl;
    }

    @Override
    public void handleTCPComplete(TCPSessionEvent event)
        throws MPipeException
    {
        Session s = event.session();
        SpamSMTPRBLEvent rblEvent = (SpamSMTPRBLEvent)s.attachment();
        if (null != rblEvent) {
            m_spamImpl.logRBL(rblEvent);
        }
    }

    @Override
    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event)
        throws MPipeException
    {
        TCPNewSessionRequest tsr = event.sessionRequest();

        boolean inbound = tsr.isInbound();
        SpamSettings spamSettings = m_spamImpl.getSpamSettings();
        SpamSMTPConfig spamConfig = inbound ?
            spamSettings.getSMTPInbound() : spamSettings.getSMTPOutbound();
        
        boolean releaseSession = true;
        
        if (spamConfig.getThrottle()) {
            //m_logger.debug("Check DNSBL(s) for connection from: " + tsr.clientAddr());
            RBLChecker rblChecker = new RBLChecker(spamSettings.getSpamRBLList(),m_spamImpl);
            if (true == rblChecker.check(tsr, spamConfig.getThrottleSec())) {
                m_logger.debug("DNSBL hit confirmed, rejecting connection from: " + tsr.clientAddr());
                /* get finalization in order to log rejection */
                tsr.rejectReturnRst(true);
                /* don't reject and release */
                releaseSession = false;
            }
        }

        /* release, only needs finalization if the attachment is non-null */
        if (releaseSession) tsr.release(tsr.attachment() != null);
    }
}
