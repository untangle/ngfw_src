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

package com.untangle.node.mail.impl.smtp;

import com.untangle.uvm.tapi.TCPSession;
import com.untangle.node.mail.impl.AbstractMailCasing;
import com.untangle.node.sasl.SASLObserver;
import com.untangle.node.sasl.SASLObserverFactory;
import com.untangle.node.token.Parser;
import com.untangle.node.token.Unparser;
import org.apache.log4j.Logger;


public class SmtpCasing
    extends AbstractMailCasing {

    private final Logger m_logger =
        Logger.getLogger(SmtpCasing.class);

    private static final boolean TRACE = false;

    private final SmtpParser m_parser;
    private final SmtpUnparser m_unparser;

    private CasingSessionTracker m_tracker;
    private SmtpSASLObserver m_saslObserver;

    // constructors -----------------------------------------------------------

    public SmtpCasing(TCPSession session,
                      boolean clientSide) {

        super(session, clientSide, "smtp", TRACE);

        m_tracker = new CasingSessionTracker();

        if(clientSide) {
            m_parser = new SmtpClientParser(session, this, m_tracker);
            m_unparser = new SmtpClientUnparser(session, this, m_tracker);
        }
        else {
            m_parser = new SmtpServerParser(session, this, m_tracker);
            m_unparser = new SmtpServerUnparser(session, this, m_tracker);
        }
    }

    /**
     * Test if this session is engaged in a SASL exchange
     *
     * @return true if in SASL login
     */
    boolean isInSASLLogin() {
        return m_saslObserver != null;
    }

    /**
     * Open a SASL exchange observer, based on the given
     * mechanism name.  If null is returned, a suitable
     * SASLObserver could not be found for the named mechanism
     * (and we should punt on this session).
     */
    boolean openSASLExchange(String mechanismName) {
        SASLObserver observer =
            SASLObserverFactory.createObserverForMechanism(mechanismName);
        if(observer == null) {
            m_logger.debug("Could not find SASLObserver for mechanism \"" +
                           mechanismName + "\"");
            return false;
        }
        m_saslObserver = new SmtpSASLObserver(observer);
        return true;
    }

    /**
     * Get the current SASLObserver.  If this returns
     * null yet the caller thinks there is an open
     * SASL exchange, this is an error
     *
     * @return the SmtpSASLObserver
     */
    SmtpSASLObserver getSASLObserver() {
        return m_saslObserver;
    }

    /**
     * Close the current SASLExchange
     */
    void closeSASLExchange() {
        m_saslObserver = null;
    }

    public Parser parser() {
        return m_parser;
    }

    public Unparser unparser() {
        return m_unparser;
    }
}
