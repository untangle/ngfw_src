/**
 * $Id$
 */
package com.untangle.app.smtp.sasl;

import java.nio.ByteBuffer;

/**
 * Base class for Observers of mechanisms which do not support privacy/integrity protection. <br>
 * <br>
 * By default, this class does not inspect the protocol yet advertizes that integrity and privacy cannot result from the
 * exchange.
 */
abstract class ClearObserver extends SASLObserver
{

    /**
     * Setup the observer with the specified mechanism name
     *
     * @param mechName  Mechanism name (e.g.,PLAIN, ANONYMOUS, CRAM-MD5)
     * @param maxMessageSz  Maximum messaqge size.
     */
    ClearObserver(String mechName, int maxMessageSz) {
        super(mechName, false, false, maxMessageSz);
    }

    /**
     * Specifies whether exchange using privacy is supported.
     * 
     * @return FeatureStatus of NO.
     */
    @Override
    public FeatureStatus exchangeUsingPrivacy()
    {
        return FeatureStatus.NO;
    }

    /**
     * Specifies whether exchange using integrity is supported.
     * 
     * @return FeatureStatus of NO.
     */
    @Override
    public FeatureStatus exchangeUsingIntegrity()
    {
        return FeatureStatus.NO;
    }

    /**
     * Specifies whether exchange using autentication identity is supported.
     * 
     * @return FeatureStatus of UNKNOWN.
     */
    @Override
    public FeatureStatus exchangeAuthIDFound()
    {
        return FeatureStatus.UNKNOWN;
    }

    /**
     * Return the authentication identifier.
     *
     * @return Null string.
     * 
     */
    @Override
    public String getAuthID()
    {
        return null;
    }

    /**
     * Return whether exchange is complete.
     *
     * @return FeatureStatus of UNKNOWN.
     * 
     */
    @Override
    public FeatureStatus exchangeComplete()
    {
        return FeatureStatus.UNKNOWN;
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
        return false;
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
        return false;
    }

    /**
     * Handle server data.
     * 
     * @param  buf ByteBuffer of server data.
     * @return     Always false.
     */
    @Override
    public boolean serverData(ByteBuffer buf)
    {
        return false;
    }

}
