/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: UnparseEvent.java,v 1.3 2005/01/14 00:41:20 amread Exp $
 */

package com.metavize.tran.token;

public class UnparseEvent
{
    private Token node;
    private UnparseSession us;

    UnparseEvent(UnparseSession us, Token node)
    {
        this.node = node;
        this.us = us;
    }

    public UnparseSession unparseSession()
    {
        return us;
    }

    public Token node()
    {
        return node;
    }
}
