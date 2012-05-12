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

package com.untangle.node.mail;

import com.untangle.node.mail.impl.pop.PopClientParser;
import com.untangle.node.mail.impl.pop.PopServerParser;
import com.untangle.node.mail.impl.pop.PopUnparser;
import com.untangle.node.token.Casing;
import com.untangle.node.token.Parser;
import com.untangle.node.token.Unparser;
import com.untangle.uvm.vnet.NodeTCPSession;

public class PopCasing implements Casing
{
    private final Parser parser;
    private final PopUnparser unparser;

    // these elements are only used on server-side casings
    private String zUser;
    // incoming msg is expected from server
    // (client sent RETR <#> command
    //  so unless there is an error,
    //  server will send msg immediately after +OK reply)
    private boolean bIncomingMsg;
    // incoming msg hdr is expected from server
    // (client sent TOP <#> <#> command
    //  so unless there is an error,
    //  server will send msg hdr immediately after +OK reply)
    private boolean bIncomingMsgHdr;

    // constructors -----------------------------------------------------------

    PopCasing(NodeTCPSession session, boolean clientSide)
    {
        parser = true == clientSide ? new PopClientParser(session, this) : new PopServerParser(session, this);
        unparser = new PopUnparser(session, clientSide, this);

        zUser = null;
        bIncomingMsg = false;
        bIncomingMsgHdr = false;
    }

    // Casing methods ---------------------------------------------------------

    public Parser parser()
    {
        return parser;
    }

    public Unparser unparser()
    {
        return unparser;
    }

    public void setUser(String zUser)
    {
        this.zUser = zUser;
        return;
    }

    public String getUser()
    {
        return zUser;
    }

    // client unparser will set flag to true when RETR command is found
    // server parser will set flag to false as soon as reply is received
    // (msg is assumed to immediately follow only if reply is +OK)
    public void setIncomingMsg(boolean bIncomingMsg)
    {
        this.bIncomingMsg = bIncomingMsg;
        return;
    }

    public boolean getIncomingMsg()
    {
        return bIncomingMsg;
    }

    // client unparser will set flag to true when TOP command is found
    // server parser will set flag to false as soon as reply is received
    // (msg hdr is assumed to immediately follow only if reply is +OK)
    public void setIncomingMsgHdr(boolean bIncomingMsgHdr)
    {
        this.bIncomingMsgHdr = bIncomingMsgHdr;
        return;
    }

    public boolean getIncomingMsgHdr()
    {
        return bIncomingMsgHdr;
    }
}
