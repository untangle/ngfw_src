/*
 * $HeadURL: svn://chef/work/src/smtp-casing/src/com/untangle/node/smtp/SmtpCasing.java $
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

package com.untangle.node.smtp;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.untangle.node.smtp.sasl.SASLObserver;
import com.untangle.node.smtp.sasl.SASLObserverFactory;
import com.untangle.node.token.Casing;
import com.untangle.node.token.Parser;
import com.untangle.node.token.Unparser;
import com.untangle.uvm.vnet.NodeTCPSession;

public class SmtpCasing implements Casing
{
    private final SmtpParser parser;
    private final SmtpUnparser unparser;

    private final Logger logger = Logger.getLogger(getClass());

    // private CasingSessionTracker m_tracker;
    private SmtpSASLObserver saslObserver;
    private CasingSessionTracker casingSessionTracker;

    // constructors -----------------------------------------------------------

    public SmtpCasing(NodeTCPSession session, boolean clientSide) {
        if (logger.isEnabledFor(Level.DEBUG)) {
            logger.debug("Creating " + (clientSide ? "client" : "server") + " SMTP Casing.  Client: "
                    + session.getClientAddr() + "(" + Integer.toString(session.getClientIntf()) + "), " + "Server: "
                    + session.getServerAddr() + "(" + Integer.toString(session.getServerIntf()) + ")");
        }

        casingSessionTracker = new CasingSessionTracker();
        parser = (clientSide ? new SmtpClientParser(session, this, casingSessionTracker) : new SmtpServerParser(
                session, this, casingSessionTracker));
        unparser = (clientSide ? new SmtpClientUnparser(session, this, casingSessionTracker) : new SmtpServerUnparser(
                session, this, casingSessionTracker));
    }

    // Casing methods ---------------------------------------------------------

    public Unparser unparser()
    {
        return unparser;
    }

    public Parser parser()
    {
        return parser;
    }

    // package private methods ------------------------------------------------

    /**
     * Callback from either parser or unparser, indicating that we are entering passthru mode. This is required as the
     * passthru token may only flow in one direction. The casing will ensure that both parser and unparser enter
     * passthru.
     */
    public final void passthru()
    {
        parser.passthru();
        unparser.passthru();
    }

    /**
     * Test if this session is engaged in a SASL exchange
     * 
     * @return true if in SASL login
     */
    boolean isInSASLLogin()
    {
        return saslObserver != null;
    }

    /**
     * Open a SASL exchange observer, based on the given mechanism name. If null is returned, a suitable SASLObserver
     * could not be found for the named mechanism (and we should punt on this session).
     */
    boolean openSASLExchange(String mechanismName)
    {
        SASLObserver observer = SASLObserverFactory.createObserverForMechanism(mechanismName);
        if (observer == null) {
            logger.debug("Could not find SASLObserver for mechanism \"" + mechanismName + "\"");
            return false;
        }
        saslObserver = new SmtpSASLObserver(observer);
        return true;
    }

    /**
     * Get the current SASLObserver. If this returns null yet the caller thinks there is an open SASL exchange, this is
     * an error
     * 
     * @return the SmtpSASLObserver
     */
    SmtpSASLObserver getSASLObserver()
    {
        return saslObserver;
    }

    /**
     * Close the current SASLExchange
     */
    void closeSASLExchange()
    {
        saslObserver = null;
    }
}
