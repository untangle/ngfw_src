 /*
  * Copyright (c) 2005 Metavize Inc.
  * All rights reserved.
  *
  * This software is the confidential and proprietary information of
  * Metavize Inc. ("Confidential Information").  You shall
  * not disclose such Confidential Information.
  *
  * $Id:$
  */
package com.metavize.tran.mime;

import javax.mail.internet.*;
import javax.mail.*;
import org.apache.log4j.Logger;
import java.io.UnsupportedEncodingException;

//---------------------------------------------------------
// Implementation Note:  Currently, we wrap a
// JavaMail InternetAddress.  Experience has shown 
// JavaMail to sometimes be flaky, so this class adds
// abstraction should we ever need to roll our own.
//
// -wrs 6/05
//---------------------------------------------------------

/**
 * Class to represent an EmailAddress.
 * <p>
 * Because of its use in HeaderFields, EmailAddress is immutable.  However,
 * there is a MutableEmailAddress subclass if you need to create your own.
 * <p>
 * Two terms have been invented for use with this class: "SMTPString" and
 * "MIMEString".  The MIMEString may have personal (proper name in quotes
 * seen often in MIME headers) whereas the SMTPString version will not.
 * There are accessors for each.
 * <p>
 * To support SMTP, there is a special case of a "blank" address.  This is an 
 * address without any data.  This is useful when the SMTP command "MAIL FROM:"
 * is formatted with "<>", as with notifications.  A null address can be tested-for
 * via the {@link #isNullAddress isNullAddress} accessor.  Note that Null address
 * have no Personal or Address, but print "<>" in their toXXXString methods.
 */
public class EmailAddress {

  private static final String BLANK_STR = "<>";

  protected InternetAddress m_jmAddress;

  /**
   * Email Address used to represent a blank mailbox.
   */
  public static final EmailAddress NULL_ADDRESS = 
    new EmailAddress();
  
  /**
   * Construct a new EmailAddress from the "local@domain"
   * formatted String.
   *
   * @param addr the "local@domain" formatted String.
   */
  public EmailAddress(String addr) 
    throws BadEmailAddressFormatException {
    try {
      m_jmAddress = new InternetAddress(addr, false);
    }
    catch(AddressException ex) {
      throw new BadEmailAddressFormatException(ex);
    }    
  }
  
  /**
   * Construct a new EmailAddress from the "local@domain"
   * formatted String and personal
   *
   * @param addr the "local@domain" formatted String.
   * @param personal the personal, which may currently be 
   *        encoded using the "=?" stuff from RFC 2047 
   */  
  public EmailAddress(String addr, String personal) 
    throws BadEmailAddressFormatException,
      java.io.UnsupportedEncodingException {
    try {
      m_jmAddress = new InternetAddress(addr, false);
      if(personal != null) {
        m_jmAddress.setPersonal(personal);
      }  
    }
    catch(AddressException ex) {
      throw new BadEmailAddressFormatException(ex);
    }    
  }  
    
    
  /**
   * Constructor which wraps a JavaMail address.
   */
  protected EmailAddress(InternetAddress addr) {
    m_jmAddress = addr;
  }
  
  /*
   * Constructor for the null address.
   */
  private EmailAddress() {
  }
  
  
  /**
   * Test if this is a null address.
   *
   * @return true if this is the null (blank) address.
   */
  public boolean isNullAddress() {
    return m_jmAddress == null;
  }
    
  
  /**
   * Access the "personal" field, which is a comment associated
   * with the address (usualy the name of the user who owns the address).
   * This may be null.
   * <p>
   * Any RFC 2047 encoding has been removed from 
   * what is returned.
   */
  public String getPersonal() {
    return isNullAddress()?
      null:m_jmAddress.getPersonal();
  }
  
  /**
   * Get the email address (w/o personal).
   *
   * @return the address.
   */
  public String getAddress() {
    return isNullAddress()?
      null:m_jmAddress.getAddress();
  }
  
  /**
   * Convert to a String suitable for SMTP transport.  This removes
   * any of the "personal" stuff.
   *
   * XXXXXX bscott Figure out if you really cannot put the encoded personal stuff on SMTP?
   */
  public String toSMTPString() {
    if(isNullAddress()) {
      return BLANK_STR;
    }
    try {
      String oldPersonal = m_jmAddress.getPersonal();
      if(oldPersonal != null) {
        m_jmAddress.setPersonal(null);
        String ret = m_jmAddress.toString();
        m_jmAddress.setPersonal(oldPersonal);
        return ret;
      }
    }
    catch(UnsupportedEncodingException shouldNotHappen) {
      Logger.getLogger(EmailAddress.class).error(shouldNotHappen);
    }
    return m_jmAddress.toString();  
  }
  
  /**
   * Convert to a MIME String.  Note that this may have personal name,
   * and may be encoded as-per the encoding of the original personal
   */
  public String toMIMEString() {
    return isNullAddress()?
      BLANK_STR:m_jmAddress.toString();
  }
  
  /**
   * Email addresses test for equivilancy based on the case-insensitive
   * comparison of the {@link #getAddress address} property.  Twp
   * {@link #isNullAddress null addresses} test true for equality.
   */
  public boolean equals(Object obj) {
    if(obj instanceof EmailAddress) {
      EmailAddress other = (EmailAddress) obj;
      return 
        other.isNullAddress()?
          isNullAddress()://Other guy null.  True if we are also null address
          isNullAddress()?//Other guy is not null.  Test if we are
            false://We're null, they are not
            getAddress().equalsIgnoreCase(other.getAddress());//Both not null.  Test address.
    }
    return false;
  }
  
  @Override
  public int hashCode() {
    return isNullAddress()?
      BLANK_STR.hashCode():m_jmAddress.hashCode();
  }

  
  /**
   * Helper method for creating a (Metavize) EmailAddress from 
   * a JavaMail address.  Note that a blank (null) address
   * can be created by passing null.
   *
   * @param addr the JavaMail address
   *
   * @return the EmailAddress
   */
  public static EmailAddress fromJavaMail(InternetAddress addr) {
    return new EmailAddress(addr);
  }  

}