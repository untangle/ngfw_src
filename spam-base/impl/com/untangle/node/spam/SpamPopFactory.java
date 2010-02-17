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

import com.untangle.node.mail.papi.MailExport;
import com.untangle.node.mail.papi.MailExportFactory;
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.TCPSession;

public class SpamPopFactory implements TokenHandlerFactory
{
    private final SpamNodeImpl node;
    private final MailExport zMExport;

    // constructors -----------------------------------------------------------

    SpamPopFactory(SpamNodeImpl node)
    {
        this.node = node;
        Policy p = node.getTid().getPolicy();
        zMExport = MailExportFactory.factory().getExport();
    }

    // TokenHandlerFactory methods --------------------------------------------

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new SpamPopHandler(session, node, zMExport);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
