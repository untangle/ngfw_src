/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.mail.impl.pop.PopClientParser;
import com.metavize.tran.mail.impl.pop.PopServerParser;
import com.metavize.tran.mail.impl.pop.PopUnparser;
import com.metavize.tran.token.AbstractCasing;
import com.metavize.tran.token.Parser;
import com.metavize.tran.token.Unparser;

public class PopCasing extends AbstractCasing
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

    PopCasing(TCPSession session, boolean clientSide)
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
