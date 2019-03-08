/**
 * $Id$
 */
package com.untangle.app.smtp.sasl;

import java.nio.ByteBuffer;

/**
 * Base class for Object which observes a SASL exchange, based on a given SASL mechanism. <br>
 * <br>
 * The interesting thing about this interface is that it is the classic man-in-the-middle (or "woman-in-the-middle", as
 * "Eve" is the persona usualy given to this role in security texts). Since we are a benign company, instances of
 * SASLObserver do <b>not</b> alter a SASL exchange, and <b>never</b> steal passwords (some of the more lame mechanisms
 * only base64 encode passwords, which could be stolen). Instead, a SASLObserver's role is to attempt to determine the
 * public identity of the client in a SASL exchange (the {@link #getAuthID Authorization ID}), and to determine if the
 * enclosing protocol exchange will become {@link #exchangeUsingIntegrity integrity protected} of
 * {@link #exchangeUsingPrivacy encrypted} as a result of the SASL exchange. <br>
 * <br>
 * Note that the methods begining with "mechanism" reveal properties of the mechanism, not the given transaction (in
 * other words, just because a given mechanism {@link #mechanismSupportsPrivacy supports encryption} does not mean that
 * a given SASL exchange will result in an encrypted channel).
 */
public abstract class SASLObserver
{

    protected static final int DEF_MAX_MSG_SZ = 1024 * 4;

    /**
     * Enumeration of values for the status of a feature.
     */
    public static enum FeatureStatus
    {
        /**
         * The feature has been determined, and is positive
         */
        YES,
        /**
         * The feature has been determined, and is negative
         */
        NO,
        /**
         * The feature has yet to be determined
         */
        UNKNOWN
    };

    private final String m_mechName;
    private final boolean m_supportsPrivacy;
    private final boolean m_supportsIntegrity;
    private final int m_maxMessageSz;

    /**
     * Setup SASLObserver.
     *
     * @param mechName  Mechanism name.
     * @param supportsPrivacy true if supports privacy, false otherwise.
     * @param supportsIntegrity true if supprots integrity, false otherwise.
     * @param maxMessageSz maximium supported message size.
     */
    SASLObserver(String mechName, boolean supportsPrivacy, boolean supportsIntegrity, int maxMessageSz) {
        m_mechName = mechName;
        m_supportsPrivacy = supportsPrivacy;
        m_supportsIntegrity = supportsIntegrity;
        m_maxMessageSz = maxMessageSz;
    }

    /**
     * Query this Observer about the mechansim's support for privacy (encryption).
     * 
     * @return true if this mechanism supports privacy
     */
    public final boolean mechanismSupportsPrivacy()
    {
        return m_supportsPrivacy;
    }

    /**
     * Query this Observer about the mechansim's support for integrity checking. Integrity protection without
     * {@link #mechanismSupportsPrivacy privacy} means the protocol messages may be observed, but not altered.
     * 
     * @return true if this mechanism supports integrity protection
     */
    public boolean mechanismSupportsIntegrity()
    {
        return m_supportsIntegrity;
    }

    /**
     * Get the maximum reasonable message size for this mechanism. Note that this is without the enveloping protocol.
     * For example, if things are base64 encoded the number should be multiplied by 4 then divided by 3.
     * 
     * @return the max reasonable message size.
     */
    public int getMaxReasonableMessageSz()
    {
        return m_maxMessageSz;
    }

    /**
     * Get the SASL name of this mechanism
     * 
     * @return the name
     */
    public final String getMechanismName()
    {
        return m_mechName;
    }

    /**
     * Specifies whether exchange using privacy is supported.
     * 
     * @return FeatureStatus of UNKNOWN, YES, or NO.
     */
    public abstract FeatureStatus exchangeUsingPrivacy();

    /**
     * Specifies whether exchange using integrity is supported.
     * 
     * @return FeatureStatus of UNKNOWN, YES, or NO.
     */
    public abstract FeatureStatus exchangeUsingIntegrity();

    /**
     * Specifies whether exchange using authentication identifier is supported.
     * 
     * @return FeatureStatus of UNKNOWN, YES, or NO.
     */
    public abstract FeatureStatus exchangeAuthIDFound();

    /**
     * Get the AuthorizationID, if {@link #exchangeAuthIDFound it has been found}. Note that for some mechanisms, this
     * can never be found. For other mechanisms which separate the Authorization ID from the Authentication ID,
     * implementations should always choose the AuthorizationID.
     * 
     * @return the Authorization ID, or null if not (yet?) found.
     */
    public abstract String getAuthID();

    /**
     * This one is perhaps useless - I'm still not sure. SASL says that the profile (i.e. "SASL-in-SMTP" or
     * "SASL-in-IMAP") determines (a) when the exchange is complete and (b) the outcome. In other words, seeing
     * "XXXX OK" in IMAP means that the SASL login is done. However, in my reading is seems hard to parse those profiles
     * to know if we're observing (a) SASL stuff or (b) protocol stuff. <br>
     * <br>
     * This method then acts as a "hint" as to the disposition of the exchange, based on the SASL data.
     *
     * @return FeatureStatus of YES, NO, or UNKNOWN
     */
    public abstract FeatureStatus exchangeComplete();

    /**
     * Pass-in initial client data. This is an optional feature of SASL (see RFC 2222 Section 5.1). <br>
     * <br>
     * Note that only some SASL profiles support this feature (SMTP seems to, yet IMAP does not).
     * 
     * @param buf
     *            the buffer. Note that implementations <b>are permitted to modify the buffer</b> so a duplicate should
     *            be passed if this is a concern.
     * 
     * @return true if this data has changed any of the observed properties of this exchange.
     */
    public abstract boolean initialClientData(ByteBuffer buf);

    /**
     * Handle additional client data.
     * 
     * @param  buf ByteBuffer of the additional client data.
     * @return     Always false.
     */
    public abstract boolean clientData(ByteBuffer buf);

    /**
     * Handle server data.
     * 
     * @param  buf ByteBuffer of server data.
     * @return     Always false.
     */
    public abstract boolean serverData(ByteBuffer buf);

}
