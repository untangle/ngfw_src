/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Reply.java,v 1.1 2005/01/22 05:34:24 jdi Exp $
 */
package com.metavize.tran.email;

import java.util.regex.*;

import com.metavize.tran.util.*;

public class Reply
{
    /* constants */

    /* class variables */

    /* instance variables */
    Pattern zPattern; /* expected reply */
    CBufferWrapper zCLine; /* pseudo-reply */

    /* constructors */
    public Reply()
    {
        this.zPattern = null;
        this.zCLine = null;
    }

    public Reply(Pattern zPattern, CBufferWrapper zCLine)
    {
        this.zPattern = zPattern;
        this.zCLine = zCLine;
    }

    /* public methods */
    public Pattern getP()
    {
        return zPattern;
    }

    public CBufferWrapper getC()
    {
        return zCLine;
    }

    public void renew(Pattern zPattern, CBufferWrapper zCLine)
    {
        this.zPattern = zPattern;
        this.zCLine = zCLine;

        return;
    }

    /* private methods */
}
