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

import java.nio.ByteBuffer;

public class UntokenizerResult
{
    // XXX make List<ByteBuffer> when no XDoclet
    private ByteBuffer[] result;

    // XXX make List<ByteBuffer> when no XDoclet
    public UntokenizerResult(ByteBuffer[] result)
    {
        this.result = result;
    }

    public ByteBuffer[] result()
    {
        return result;
    }
}
