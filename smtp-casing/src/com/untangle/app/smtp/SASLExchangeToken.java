/**
 * $Id$
 */
package com.untangle.app.smtp;

import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.Token;

/**
 * Opaque chunk of data, used to pass SASL information between casings, being ignored by Nodes.
 */
public class SASLExchangeToken implements Token
{

    private final ByteBuffer m_buf;

    public SASLExchangeToken(ByteBuffer data) {
        m_buf = data;
    }

    /**
     * Returns a duplicate of the internal ByteBuffer, allowing the caller to modify the returned ByteBuffer without
     * concern for any downstream token handlers.
     */
    public ByteBuffer getBytes()
    {
        return m_buf.slice();
    }
}
