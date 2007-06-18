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

package com.untangle.node.phish;

import com.untangle.uvm.tapi.TCPSession;
import com.untangle.node.mail.papi.quarantine.QuarantineNodeView;
import com.untangle.node.mail.papi.safelist.SafelistNodeView;
import com.untangle.node.spam.SpamReport;
import com.untangle.node.spam.SpamSMTPConfig;

/**
 * Protocol Handler which is called-back as scanable messages
 * are encountered.
 */
public class PhishSmtpHandler extends com.untangle.node.spam.SmtpSessionHandler {

    PhishSmtpHandler(TCPSession session,
                     long maxClientWait,
                     long maxSvrWait,
                     PhishNode impl,
                     SpamSMTPConfig config,
                     QuarantineNodeView quarantine,
                     SafelistNodeView safelist) {
        super(session, maxClientWait, maxSvrWait, impl, config, quarantine, safelist);
    }

    @Override
    protected String getQuarantineCategory() {
        return "FRAUD";
    }

    @Override
    protected String getQuarantineDetail(SpamReport report) {
        //TODO bscott Do something real here
        return "Identity Theft";
    }
}
