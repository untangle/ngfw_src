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

import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.node.mail.papi.WrappedMessageGenerator;
import com.untangle.node.mail.papi.safelist.SafelistNodeView;
import com.untangle.node.spam.SpamImapHandler;
import com.untangle.node.spam.SpamImapConfig;

class PhishImapHandler extends SpamImapHandler
{
    private static final String MOD_SUB_TEMPLATE =
        "[PHISH] $MIMEMessage:SUBJECT$";
    private static final String MOD_BODY_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$\r\n" +
        "was determined by the Phish Blocker to be PHISH (a\r\n" +
        "fraudulent email intended to steal information).  The kind of PHISH that was\r\n" +
        "found was $SPAMReport:FULL$";
    private static WrappedMessageGenerator msgGenerator = new WrappedMessageGenerator(MOD_SUB_TEMPLATE,MOD_BODY_TEMPLATE);

    PhishImapHandler(NodeTCPSession session,
                     long maxClientWait,
                     long maxSvrWait,
                     PhishNode impl,
                     SpamImapConfig config,
                     SafelistNodeView safelist) {
        super(session, maxClientWait, maxSvrWait, impl, config, safelist);
        PhishImapHandler.msgGenerator = new WrappedMessageGenerator(MOD_SUB_TEMPLATE,MOD_BODY_TEMPLATE);
    }

    @Override
    protected WrappedMessageGenerator getMsgGenerator() {
        return PhishImapHandler.msgGenerator;
    }
}
