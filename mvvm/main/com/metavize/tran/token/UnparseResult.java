/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: UnparseResult.java,v 1.2 2005/01/10 23:15:02 amread Exp $
 */

package com.metavize.tran.token;

import java.nio.ByteBuffer;

public class UnparseResult
{
    // XXX make List<ByteBuffer> when no XDoclet
    private ByteBuffer[] result;

    // XXX make List<ByteBuffer> when no XDoclet
    public UnparseResult(ByteBuffer[] result)
    {
        this.result = result;
    }

    public ByteBuffer[] result()
    {
        return result;
    }
}
