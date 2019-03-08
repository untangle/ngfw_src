/**
 * $Id$
 */
package com.untangle.app.smtp.sasl;

import static com.untangle.uvm.util.AsciiUtil.bbToString;

import java.nio.ByteBuffer;

/**
 * Abstract base class for SASL mechanisms which send a clear UID as the first client message (ANONYMOUS and SKEY that I
 * know of).
 */
abstract class InitialIDObserver extends ClearObserver
{

    private String m_id;
    private boolean m_seenInitialClientData = false;

    /**
     * Setup InitialIDObserver.
     *
     * @param mechName Mechanism name.
     * @param maxMsgSize Maximium supported message size.
     */
    InitialIDObserver(String mechName, int maxMsgSize) {
        super(mechName, maxMsgSize);
    }

    /**
     * Return if exchange of authentiation ID is found.
     * 
     * @return FeatureStatus of YES if m_id is specified.  NO if null.  UNKNOWN if initial client data has not been seen.
     */
    @Override
    public FeatureStatus exchangeAuthIDFound()
    {
        return m_seenInitialClientData ? (m_id == null ? FeatureStatus.NO : FeatureStatus.YES) : FeatureStatus.UNKNOWN;
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
     * Handle clent message.
     * 
     * @param  buf ByteBuffer of client data.
     * @return     true if data was able to be handled, false otherwise.
     */
    private boolean clientMessage(ByteBuffer buf)
    {

        if (m_seenInitialClientData) {
            return false;
        }

        if (!buf.hasRemaining()) {
            return false;
        }

        m_seenInitialClientData = true;
        m_id = bbToString(buf);
        return true;
    }
}
