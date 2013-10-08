/**
 * $Id: TarpitEventHandler.java 34295 2013-03-17 20:24:07Z dmorris $
 */
package com.untangle.node.spam;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.NodeSession;
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
    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event)
    {
        TCPNewSessionRequest tsr = event.sessionRequest();
        SpamSettings spamSettings = spamImpl.getSettings();
        SpamSmtpConfig spamConfig = spamSettings.getSmtpConfig();

        if (spamConfig.getTarpit()) {

            if (this.dnsblChecker == null) {
                synchronized(this) {
                    if (this.dnsblChecker == null) {
                        this.dnsblChecker  = new DnsblChecker( spamImpl.getSettings().getSpamDnsblList(), spamImpl );
                    }
                }
            }
            
            logger.debug("Check DNSBL(s) for connection from: " + tsr.getClientAddr());

            if ( dnsblChecker.check(tsr, spamConfig.getTarpitTimeout()) == true ) {
                logger.debug("DNSBL hit confirmed, rejecting connection from: " + tsr.getClientAddr());
                tsr.rejectReturnRst();
                return;
            }
        }

        tsr.release();
    }
}
