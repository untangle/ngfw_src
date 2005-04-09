/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.token;

public class UnparseEvent
{
    private Token node;

    UnparseEvent(Token node)
    {
        this.node = node;
    }

    public Token node()
    {
        return node;
    }
}
