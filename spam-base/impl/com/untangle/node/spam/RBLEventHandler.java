/*
 * $HeadURL:$
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
