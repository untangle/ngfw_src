/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.node.mail.papi.smtp;

import java.nio.ByteBuffer;

import com.untangle.node.token.Token;

/**
 * Opaque chunk of data, used to pass SASL information
 * between casings, being ignored by Nodes.
 */
public class SASLExchangeToken
    implements Token {

    private final ByteBuffer m_buf;

    public SASLExchangeToken(ByteBuffer data) {
        m_buf = data;
    }

    /**
     * Returns a duplicate of the internal ByteBuffer, allowing
     * the caller to modify the returned ByteBuffer without concern
     * for any downstream token handlers.
     */
    public ByteBuffer getBytes() {
        return m_buf.slice();
    }

    public int getEstimatedSize()
    {
        return m_buf.remaining();
    }
}
