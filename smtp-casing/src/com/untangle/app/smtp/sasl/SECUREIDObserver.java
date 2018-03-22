/**
 * $Id$
 */
package com.untangle.app.smtp.sasl;

import static com.untangle.uvm.util.AsciiUtil.bbToString;

import java.nio.ByteBuffer;

/**
 * Observer for SECUREID mechanism (RFC 2808).
 */
class SECUREIDObserver extends ClearObserver
{

    static final String[] MECH_NAMES = new String[] { "SECUREID".toLowerCase() };

    private String m_id;
    private boolean m_seenInitialClientMessage = false;

    /**
     * Setup SECUREIDOBserver
     */
    SECUREIDObserver() {
        super(MECH_NAMES[0], DEF_MAX_MSG_SZ);
    }

    /**
     * Specifies whether exchange using authentication identifier is supported.
     * 
     * @return FeatureStatus of UNKNOWN, YES, or NO.
     */
    @Override
    public FeatureStatus exchangeAuthIDFound()
    {
        return m_seenInitialClientMessage ? (m_id == null ? FeatureStatus.NO : FeatureStatus.YES)
                : FeatureStatus.UNKNOWN;
    }

    /**
     * Get the AuthorizationID, if {@link #exchangeAuthIDFound it has been found}. Note that for some mechanisms, this
     * can never be found. For other mechanisms which separate the Authorization ID from the Authentication ID,
     * implementations should always choose the AuthorizationID.
     * 
     * @return the Authorization ID, or null if not (yet?) found.
     */
    @Override
    public String getAuthID()
    {
        return m_id;
    }

    /**
     * Handle initial client data.
     * 
     * @param  buf ByteBuffer of the initial client data.
     * @return     Always false.
     */
    @Override
    public boolean initialClientData(ByteBuffer buf)
    {
        return clientMessage(buf);
    }

    /**
     * Handle additional client data.
     * 
     * @param  buf ByteBuffer of the additional client data.
     * @return     Always false.
     */
    @Override
    public boolean clientData(ByteBuffer buf)
    {
        return clientMessage(buf);
    }

    /**
     * Handle server data.
     * 
     * @param  buf ByteBuffer of server data.
     * @return     Always false.
     */
    private boolean clientMessage(ByteBuffer buf)
    {

        if (m_seenInitialClientMessage) {
            return false;
        }

        if (!buf.hasRemaining()) {
            return false;
        }
        m_seenInitialClientMessage = true;

        // I'm unclear from the spec if the authorization ID
        // is blank, if there is a leading null. If so,
        // just strip it off
        if (buf.get(buf.position()) == 0) {
            buf.get();
        }
        if (!buf.hasRemaining()) {
            return false;
        }

        // If we see yet-another leading null, then
        // give-up.
        if (buf.get(buf.position()) == 0) {
            return true;
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

        return true;
    }
}
