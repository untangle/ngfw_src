/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: EndMarker.java,v 1.4 2005/01/27 09:53:35 amread Exp $
 */

package com.metavize.tran.token;

import java.nio.ByteBuffer;

/**
 * Marks the end of a set of {@link Chunk}s.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class EndMarker implements Token
{
    public static final EndMarker MARKER = new EndMarker();

    private static final ByteBuffer ZERO = ByteBuffer.allocate(0);

    private EndMarker() { }

    // Token methods ----------------------------------------------------------

    public ByteBuffer getBytes()
    {
        return ZERO;
    }
}
