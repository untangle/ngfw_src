/**
 * $Id$
 */
package com.untangle.app.smtp.sasl;

/**
 * Observer for SKEY (RFC 2222) mechanism.
 */
class SKEYObserver extends InitialIDObserver
{

    static final String[] MECH_NAMES = new String[] { "SKEY".toLowerCase() };

    /**
     * Setup SKEYObserver
     */
    SKEYObserver() {
        super(MECH_NAMES[0], DEF_MAX_MSG_SZ);
    }
}
