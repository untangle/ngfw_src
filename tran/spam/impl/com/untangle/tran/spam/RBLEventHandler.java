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

package com.untangle.tran.spam;

import com.untangle.mvvm.tapi.AbstractEventHandler;
import com.untangle.mvvm.tapi.MPipeException;
import com.untangle.mvvm.tapi.TCPNewSessionRequest;

import com.untangle.mvvm.tapi.event.TCPNewSessionRequestEvent;
import com.untangle.mvvm.tapi.event.TCPSessionEvent;

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
            //m_logger.debug("Check RBL(s) for connection from: " + tsr.clientAddr());
            RBLChecker rblChecker = new RBLChecker(spamSettings.getSpamRBLList());
            if (true == rblChecker.check(tsr, spamConfig.getThrottleSec())) {
                m_logger.debug("RBL hit confirmed, rejecting connection from: " + tsr.clientAddr());
                tsr.rejectReturnRst();
                /* don't reject and release */
                releaseSession = false;
            }
        }

        /* release, doesn't need finalization */
        if (releaseSession) tsr.release(false);
    }

    @Override
    public void handleTCPComplete(TCPSessionEvent event)
        throws MPipeException
    {
        /* Load the session and check if there is an event to log */
        Object a = event.session().attachment();
        if ((a != null) && (a instanceof SpamSMTPRBLEvent)) {
            SpamSMTPRBLEvent fe = (SpamSMTPRBLEvent)a;
            m_spamImpl.logRBL(fe);
        }
    }
}
