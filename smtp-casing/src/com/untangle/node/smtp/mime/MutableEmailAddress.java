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

package com.untangle.node.smtp.mime;

import java.io.UnsupportedEncodingException;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.log4j.Logger;

/**
 * Subclass of {@link EmailAddress EmailAddress} which can
 * be modified.
 */
public class MutableEmailAddress
    extends EmailAddress {


    /**
     * Constructor which wraps a JavaMail address.
     */
    protected MutableEmailAddress(InternetAddress addr) {
        super(addr);
    }

    /**
     * Blank constructor.
     */
    public MutableEmailAddress() {
        super((InternetAddress) null);
    }

    /**
     * Construct a new EmailAddress from the "local@domain"
     * formatted String.
     *
     * @param addr the "local@domain" formatted String.
     *
     * @exception BadEmailAddressFormatException if the addr
     *            String could not be parsed into a valid address.
     */
    public MutableEmailAddress(String addr)
        throws BadEmailAddressFormatException {
        super(addr);
    }

    /**
     * Construct a new EmailAddress from the "local@domain"
     * formatted String and personal
     *
     * @param addr the "local@domain" formatted String.
     * @param personal the personal, which may currently be
     *        encoded as per RFC 2047.
     *
     * @exception BadEmailAddressFormatException if the addr
     *            String could not be parsed into a valid address.
     *
     * @exception UnsupportedEncodingException if the personal
     *            is currently encoded (as per the RFC 2047 "?="
     *            stuff) but this platform cannot decode.
     */
    public MutableEmailAddress(String addr, String personal)
        throws BadEmailAddressFormatException,
               UnsupportedEncodingException {
        super(addr, personal);
    }

    /**
     * Copy constructor
     */
    public MutableEmailAddress(EmailAddress copyFrom) {
        super(copyFrom.isNullAddress()?
              (InternetAddress) null:
              copyFromEmailAddress(copyFrom));
    }

    /**
     * This method supresses the encoding exception,
     * as it should not be thrown (it would have already
     * been caught by the copy target).
     */
    private static InternetAddress copyFromEmailAddress(EmailAddress copyFrom) {
        try {
            InternetAddress ret = new InternetAddress(copyFrom.getAddress(), false);
            if(copyFrom.getPersonal() != null) {
                ret.setPersonal(copyFrom.getPersonal());
            }
            return ret;
        }
        catch(java.io.UnsupportedEncodingException shouldNotHappen) {
            Logger.getLogger(EmailAddress.class).error(shouldNotHappen);
            try {
                return new InternetAddress(copyFrom.getAddress());
            }
            catch(AddressException ex) {
                Logger.getLogger(EmailAddress.class).error(ex);
                return null;//XXXXXXX bscott Correct behavior?
            }
        }
        catch(AddressException ex) {
            Logger.getLogger(EmailAddress.class).error(ex);
            return null;//XXXXXXX bscott Correct behavior?
        }
    }

    /**
     * Set this to be the null address.  There is no
     * "UnsetNullAddress".  This is done by calling
     * {@link #setAddress setAddress}.
     *
     */
    public void setNullAddress() {
        m_jmAddress = null;
    }


    /**
     * Set the personal for this address.
     * <p>
     * <b>WARNING - To make my life easy I've avoided a corner
     * case.  If you call this method while this
     * is {@link #isNullAddress a null address} then
     * this call is ignored.</b>
     *
     * @param personal the personal portion of the address.
     *
     *
     * @exception UnsupportedEncodingException if the personal
     *            is currently encoded (as per the RFC 2047 "?="
     *            stuff) but this platform cannot decode.
     */
    public void setPersonal(String personal)
        throws UnsupportedEncodingException {
        if(!isNullAddress()) {
            m_jmAddress.setPersonal(personal);
        }
    }

    /**
     * Set the address (local@domain).
     *
     * @exception BadEmailAddressFormatException if the address
     *            is in an invalid format
     */
    public void setAddress(String addr)
        throws BadEmailAddressFormatException {
        try {
            if(isNullAddress()) {
                m_jmAddress = new InternetAddress(addr, false);
            }
            else {
                String oldPersonal = m_jmAddress.getPersonal();
                m_jmAddress = new InternetAddress(addr, false);
                if(oldPersonal != null) {
                    m_jmAddress.setPersonal(oldPersonal);
                }
            }
        }
        catch(UnsupportedEncodingException shouldNotHappen) {
            Logger.getLogger(EmailAddress.class).error(shouldNotHappen);
            m_jmAddress.setAddress(addr);
        }
        catch(AddressException ex) {
            throw new BadEmailAddressFormatException(ex);
        }
    }


    /**
     * Helper method for creating a (Untangle) MutableEmailAddress from
     * a JavaMail address.
     *
     * @param addr the JavaMail address
     *
     * @return the EmailAddress
     */
    public static EmailAddress fromJavaMail(InternetAddress addr) {
        return new MutableEmailAddress(addr);
    }

}
