/**
 * $Id$
 */
package com.untangle.app.smtp.sasl;

/**
 * Observer for ANONYMOUS (RFC 2245) mechanism.
 */
class ANONYMOUSObserver extends InitialIDObserver
{

    static final String[] MECH_NAMES = new String[] { "ANONYMOUS".toLowerCase() };

    /**
     * Setup for ANONYMOUS authentication.
     */
    ANONYMOUSObserver() {
        super(MECH_NAMES[0], DEF_MAX_MSG_SZ);
    }
}
