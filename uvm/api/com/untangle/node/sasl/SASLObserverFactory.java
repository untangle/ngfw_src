/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */
package com.untangle.node.sasl;

/**
 * Class which acts as a Factory,
 * {@link #createObserverForMechanism creating SASLObservers}
 * for named SASL mechanisms.
 */
public class SASLObserverFactory {

    /*

    Unsupported (as they may be encrypted)

    "GSSAPI" May be encrypted, RFC 2222
    "DIGEST-MD5" May be encrypted, RFC 2831
    "KERBEROS_V4" May be encrypted, RFC 2222
    "SRP" May be encrypted
    "SCRAM-MD5" Don't know, assume encrypted
    "NTLM" Undocumented (http://davenport.sourceforge.net/ntlm.html), assume encrypted
    "EXTERNAL" Assume can be encrypted, RFC 2222


    */

    /**
     * Factory method to create a SASLObserver for
     * the named SASL mechanism.
     *
     * @param mechanismName the name of the mechanism (see RFC 2222 Section 3).
     *
     * @return the Observer, or null if none could be created for the
     *         given mechanism.
     */
    public static SASLObserver createObserverForMechanism(String mechanismName) {

        if(mechanismName == null) {
            return null;
        }
        mechanismName = mechanismName.trim().toLowerCase();

        for(String s : LOGINObserver.MECH_NAMES) {
            if(s.equals(mechanismName)) {
                return new LOGINObserver();
            }
        }

        for(String s : PLAINObserver.MECH_NAMES) {
            if(s.equals(mechanismName)) {
                return new PLAINObserver();
            }
        }

        for(String s : SKEYObserver.MECH_NAMES) {
            if(s.equals(mechanismName)) {
                return new SKEYObserver();
            }
        }

        for(String s : ANONYMOUSObserver.MECH_NAMES) {
            if(s.equals(mechanismName)) {
                return new ANONYMOUSObserver();
            }
        }

        for(String s : SECUREIDObserver.MECH_NAMES) {
            if(s.equals(mechanismName)) {
                return new SECUREIDObserver();
            }
        }

        for(String s : CRAM_MD5Observer.MECH_NAMES) {
            if(s.equals(mechanismName)) {
                return new CRAM_MD5Observer();
            }
        }

        return null;
    }

}
