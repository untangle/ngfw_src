/**
 * $Id$
 */
package com.untangle.app.smtp.sasl;

import static com.untangle.uvm.util.AsciiUtil.bbToString;

import java.nio.ByteBuffer;

/**
 * Observer for the PLAIN (RFC 2595) SASL Mechanism.
 */
class PLAINObserver extends ClearObserver
{

    static final String[] MECH_NAMES = new String[] { "PLAIN".toLowerCase() };

    private String m_id;

    PLAINObserver() {
        super(MECH_NAMES[0], DEF_MAX_MSG_SZ);
    }

    @Override
    public FeatureStatus exchangeAuthIDFound()
    {
        return m_id == null ? FeatureStatus.UNKNOWN : FeatureStatus.YES;
    }

    @Override
    public String getAuthID()
    {
        return m_id;
    }

    @Override
    public boolean initialClientData(ByteBuffer buf)
    {
        return clientMessage(buf);
    }

    @Override
    public boolean clientData(ByteBuffer buf)
    {
        return clientMessage(buf);
    }

    private boolean clientMessage(ByteBuffer buf)
    {

        if (!buf.hasRemaining()) {
            return false;
        }

        // I'm unclear from the spec if the authorization ID
        // is blank, if there is a leading null. If so,
        // just strip it off
        if (buf.get(buf.position()) == 0) {
            buf.get();
        }
        if (!buf.hasRemaining()) {
            return false;
        }

        // Now, there should be at least one and at-most
        // two NULL bytes (0) in this buffer.
        int nullPos = -1;
        for (int i = buf.position(); i < buf.limit(); i++) {
            if (buf.get(i) == 0) {
                nullPos = i;
                break;
            }
        }

        if (nullPos == -1) {
            return false;
        }
        buf.limit(nullPos);
        m_id = bbToString(buf);

        return m_id != null;
    }
}
