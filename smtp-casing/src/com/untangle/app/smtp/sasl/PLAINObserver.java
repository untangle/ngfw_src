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

    /**
     * Setup the PLAINObserver
     */
    PLAINObserver() {
        super(MECH_NAMES[0], DEF_MAX_MSG_SZ);
    }

    /**
     * Return if exchange of authentiation ID is found.
     * 
     * @return FeatureStatus of YES if m_id is specified.  UNKNOWN otherwise.
     */
    @Override
    public FeatureStatus exchangeAuthIDFound()
    {
        return m_id == null ? FeatureStatus.UNKNOWN : FeatureStatus.YES;
    }

    /**
     * Return the authentication id.
     * 
     * @return String of the authentication id.  null if not specified.
     */
    @Override
    public String getAuthID()
    {
        return m_id;
    }

    /**
     * Handle initial client data.
     *
     * @param  buf ByteBuffer of initial client data.
     * @return     true if initial data was able to be handled, false otherwise.
     */
    @Override
    public boolean initialClientData(ByteBuffer buf)
    {
        return clientMessage(buf);
    }

    /**
     * Handle client data.
     *
     * @param  buf ByteBuffer of client data.
     * @return     true if data was able to be handled, false otherwise.
     */
    @Override
    public boolean clientData(ByteBuffer buf)
    {
        return clientMessage(buf);
    }

    /**
     * Handle client message.
     * 
     * @param  buf ByteBuffer of message.
     * @return     true of successfully handled, false otherwise.
     */
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
