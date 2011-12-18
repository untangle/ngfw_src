/**
 * $Id$
 */
package com.untangle.node.spam;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.Session;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.TCPSessionEvent;

class RBLEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(RBLEventHandler.class);

    private SpamNodeImpl spamImpl;

    RBLEventHandler(SpamNodeImpl spamImpl)
    {
        super(spamImpl);

        this.spamImpl = spamImpl;
    }

    @Override
    public void handleTCPComplete(TCPSessionEvent event)
    {
        Session s = event.session();
        SpamSmtpRblEvent rblEvent = (SpamSmtpRblEvent)s.attachment();
        if (null != rblEvent) {
            spamImpl.logEvent(rblEvent);
        }
    }

    @Override
    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event)
    {
        TCPNewSessionRequest tsr = event.sessionRequest();
        SpamSettings spamSettings = spamImpl.getSettings();
        SpamSmtpConfig spamConfig = spamSettings.getSmtpConfig();

        boolean releaseSession = true;

        if (spamConfig.getTarpit()) {
            //logger.debug("Check DNSBL(s) for connection from: " + tsr.clientAddr());
            RBLChecker rblChecker = new RBLChecker(spamSettings.getSpamRBLList(),spamImpl);
            if (true == rblChecker.check(tsr, spamConfig.getTarpitTimeout())) {
                logger.debug("DNSBL hit confirmed, rejecting connection from: " + tsr.clientAddr());
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
