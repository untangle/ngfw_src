/**
 * $Id$
 */
package com.untangle.app.smtp.sasl;

/**
 * Class which acts as a Factory, {@link #createObserverForMechanism creating SASLObservers} for named SASL mechanisms.
 */
public class SASLObserverFactory
{

    /*
     * 
     * Unsupported (as they may be encrypted)
     * 
     * "GSSAPI" May be encrypted, RFC 2222 "DIGEST-MD5" May be encrypted, RFC 2831 "KERBEROS_V4" May be encrypted, RFC
     * 2222 "SRP" May be encrypted "SCRAM-MD5" Don't know, assume encrypted "NTLM" Undocumented
     * (http://davenport.sourceforge.net/ntlm.html), assume encrypted "EXTERNAL" Assume can be encrypted, RFC 2222
     */

    /**
     * Factory method to create a SASLObserver for the named SASL mechanism.
     * 
     * @param mechanismName
     *            the name of the mechanism (see RFC 2222 Section 3).
     * 
     * @return the Observer, or null if none could be created for the given mechanism.
     */
    public static SASLObserver createObserverForMechanism(String mechanismName)
    {

        if (mechanismName == null) {
            return null;
        }
        mechanismName = mechanismName.trim().toLowerCase();

        for (String s : LOGINObserver.MECH_NAMES) {
            if (s.equals(mechanismName)) {
                return new LOGINObserver();
            }
        }

        for (String s : PLAINObserver.MECH_NAMES) {
            if (s.equals(mechanismName)) {
                return new PLAINObserver();
            }
        }

        for (String s : SKEYObserver.MECH_NAMES) {
            if (s.equals(mechanismName)) {
                return new SKEYObserver();
            }
        }

        for (String s : ANONYMOUSObserver.MECH_NAMES) {
            if (s.equals(mechanismName)) {
                return new ANONYMOUSObserver();
            }
        }

        for (String s : SECUREIDObserver.MECH_NAMES) {
            if (s.equals(mechanismName)) {
                return new SECUREIDObserver();
            }
        }

        for (String s : CRAM_MD5Observer.MECH_NAMES) {
            if (s.equals(mechanismName)) {
                return new CRAM_MD5Observer();
            }
        }

        return null;
    }

}
