/**
 * $Id$
 */
package com.untangle.app.smtp.sasl;

/**
 * Observer for CRAM-MD5 (RFC 2195) mechanism. Does not find the user's credentials, but serves as a placeholder so we
 * know that this mechanism <b>cannot</b> result in an encrypted channel.
 */
class CRAM_MD5Observer extends ClearObserver
{

    static final String[] MECH_NAMES = new String[] { "CRAM-MD5".toLowerCase() };

    /**
     * Setup for CRAM-MD5 authenciation
     */
    CRAM_MD5Observer() {
        super(MECH_NAMES[0], DEF_MAX_MSG_SZ);
    }
}
