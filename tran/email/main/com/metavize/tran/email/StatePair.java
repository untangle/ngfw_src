/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: StatePair.java,v 1.1 2005/01/22 05:34:24 jdi Exp $
 */
package com.metavize.tran.email;

import java.util.*;
import java.util.regex.*;

import com.metavize.tran.util.*;

public class StatePair
{
    /* constants */

    /* class variables */

    /* instance variables */
    /* for this command, we expect this "ok" reply */
    private Integer zCmd;
    private Integer zReply;
    private boolean bRenewable;

    /* constructors */
    public StatePair()
    {
        zCmd = null;
        zReply = null;
        bRenewable = true;
    }

    public StatePair(Integer zCmd, Integer zReply)
    {
        this.zCmd = zCmd;
        this.zReply = zReply;
        bRenewable = true;
    }

    public StatePair(Integer zCmd, Integer zReply, boolean bRenewable)
    {
        this.zCmd = zCmd;
        this.zReply = zReply;
        this.bRenewable = bRenewable;
    }

    /* public methods */
    public Integer getCmd()
    {
        return zCmd;
    }

    public Integer getReply()
    {
        return zReply;
    }

    public boolean isRenewable()
    {
        return bRenewable;
    }

    public void renew(Integer zCmd, Integer zReply)
    {
        if (true == bRenewable)
        {
            this.zCmd = zCmd;
            this.zReply = zReply;
        }

        return;
    }

    /* private methods */
}
