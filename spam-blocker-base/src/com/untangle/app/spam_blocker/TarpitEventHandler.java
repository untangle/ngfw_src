/**
 * $Id$
 */

package com.untangle.app.spam_blocker;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.TCPNewSessionRequest;

/**
 * Tarpit event handler
 */
class TarpitEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());

    private SpamBlockerBaseApp spamImpl;
    private DnsblChecker dnsblChecker;

    /**
     * Constructor
     * 
     * @param spamImpl
     *        The base application
     */
    TarpitEventHandler(SpamBlockerBaseApp spamImpl)
    {
        super(spamImpl);

        this.spamImpl = spamImpl;
    }

    /**
     * Handle new session requests
     * 
     * @param sessionRequest
     *        The new session request
     */
    @Override
    public void handleTCPNewSessionRequest(TCPNewSessionRequest sessionRequest)
    {
        SpamSettings spamSettings = spamImpl.getSettings();
        SpamSmtpConfig spamConfig = spamSettings.getSmtpConfig();

        if (spamConfig.getTarpit()) {

            if (this.dnsblChecker == null) {
                synchronized (this) {
                    if (this.dnsblChecker == null) {
                        this.dnsblChecker = new DnsblChecker(spamImpl.getSettings().getSpamDnsblList(), spamImpl);
                    }
                }
            }

            logger.debug("Check DNSBL(s) for connection from: " + sessionRequest.getOrigClientAddr());

            if (dnsblChecker.check(sessionRequest, spamConfig.getTarpitTimeout()) == true) {
                logger.debug("DNSBL hit confirmed, rejecting connection from: " + sessionRequest.getOrigClientAddr());
                sessionRequest.rejectReturnRst();
                return;
            }
        }

        sessionRequest.release();
    }
}
