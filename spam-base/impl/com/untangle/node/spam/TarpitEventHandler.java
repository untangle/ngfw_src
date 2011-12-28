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

class TarpitEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());

    private SpamNodeImpl spamImpl;
    private DnsblChecker dnsblChecker;
    
    TarpitEventHandler(SpamNodeImpl spamImpl)
    {
        super(spamImpl);

        this.spamImpl = spamImpl;
    }

    @Override
    public void handleTCPComplete(TCPSessionEvent event)
    {
        Session s = event.session();
        SpamSmtpTarpitEvent tarpitEvent = (SpamSmtpTarpitEvent)s.attachment();
        if (null != tarpitEvent) {
            spamImpl.logEvent(tarpitEvent);
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

            if (this.dnsblChecker == null) {
                synchronized(this) {
                    if (this.dnsblChecker == null) {
                        this.dnsblChecker  = new DnsblChecker( spamImpl.getSettings().getSpamDnsblList(), spamImpl );
                    }
                }
            }
            
            logger.debug("Check DNSBL(s) for connection from: " + tsr.clientAddr());

            if ( dnsblChecker.check(tsr, spamConfig.getTarpitTimeout()) == true ) {
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
