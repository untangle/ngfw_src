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

package com.untangle.node.spam;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.MPipeException;
import com.untangle.uvm.vnet.Session;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.TCPSessionEvent;

class RBLEventHandler extends AbstractEventHandler
{
    private final Logger m_logger = Logger.getLogger(RBLEventHandler.class);

    private SpamNodeImpl m_spamImpl;

    RBLEventHandler(SpamNodeImpl spamImpl)
    {
        super(spamImpl);

        this.m_spamImpl = spamImpl;
    }

    @Override
    public void handleTCPComplete(TCPSessionEvent event)
        throws MPipeException
    {
        Session s = event.session();
        SpamSmtpRblEvent rblEvent = (SpamSmtpRblEvent)s.attachment();
        if (null != rblEvent) {
            m_spamImpl.logRBL(rblEvent);
        }
    }

    @Override
    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event)
        throws MPipeException
    {
        TCPNewSessionRequest tsr = event.sessionRequest();
        SpamSettings spamSettings = m_spamImpl.getSpamSettings();
        SpamSmtpConfig spamConfig = spamSettings.getBaseSettings().getSmtpConfig();

        boolean releaseSession = true;

        if (spamConfig.getTarpit()) {
            //m_logger.debug("Check DNSBL(s) for connection from: " + tsr.clientAddr());
            RBLChecker rblChecker = new RBLChecker(spamSettings.getSpamRBLList(),m_spamImpl);
            if (true == rblChecker.check(tsr, spamConfig.getTarpitTimeout())) {
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
