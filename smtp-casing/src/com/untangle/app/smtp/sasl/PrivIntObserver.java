/**
 * $Id$
 */
package com.untangle.app.smtp.sasl;

import java.nio.ByteBuffer;

/**
 * Base class for Observers of mechanisms which support privacy/integrity protection. <br>
 * <br>
 * By default, this class does not inspect the protocol yet advertizes that integrity and privacy mey result from the
 * exchange.
 */
abstract class PrivIntObserver extends SASLObserver
{

    /**
     * Setup PrivIntObserver
     *
     * @param mechName String of mechanism name.
     * @param maxMessageSz Integer of maximum message size.
     */
    PrivIntObserver(String mechName, int maxMessageSz) {
        super(mechName, true, true, maxMessageSz);
    }

    /**
     * Specifies whether exchange using privacy is supported.
     * 
     * @return FeatureStatus of UNKNOWN.
     */
    @Override
    public FeatureStatus exchangeUsingPrivacy()
    {
        return FeatureStatus.UNKNOWN;
    }

    /**
     * Specifies whether exchange using integrity is supported.
     * 
     * @return FeatureStatus of UNKNOWN.
     */
    @Override
    public FeatureStatus exchangeUsingIntegrity()
    {
        return FeatureStatus.UNKNOWN;
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
