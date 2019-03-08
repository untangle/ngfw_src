/**
 * $Id$
 */
package com.untangle.app.smtp;

import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.Token;

/**
 * Opaque chunk of data, used to pass SASL information between casings, being ignored by Apps.
 */
public class SASLExchangeToken implements Token
{

    private final ByteBuffer m_buf;

    /**
     * Initialize instance of SASLExchangeToken.
     *
     * @param  data ByteBuffer to initialize with
     * @return      Instance of SASLExchangeToken.
     */
    public SASLExchangeToken(ByteBuffer data) {
        m_buf = data;
    }

    /**
     * Returns a duplicate of the internal ByteBuffer, allowing the caller to modify the returned ByteBuffer without
     * concern for any downstream token handlers.
     * @return Bytes in token.
     */
    public ByteBuffer getBytes()
    {
        return m_buf.slice();
    }
}
