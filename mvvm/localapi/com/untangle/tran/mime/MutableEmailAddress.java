/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.mime;

import java.io.UnsupportedEncodingException;
import javax.mail.*;
import javax.mail.internet.*;

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