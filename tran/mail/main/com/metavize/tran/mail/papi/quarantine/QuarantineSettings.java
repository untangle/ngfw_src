/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.papi.quarantine;

import com.metavize.tran.mail.papi.EmailAddressRule;
import com.metavize.tran.mail.papi.EmailAddressPairRule;

import java.util.List;

/**
 * Settings for the quarantine stuff
 *
 * @hibernate.class
 * table="TR_MAIL_QUARANTINE_SETTINGS" 
 */
public class QuarantineSettings
  implements java.io.Serializable {

  private static final long serialVersionUID = -4387806670497574999L;

  public static final long HOUR = 1000*60*60;
  public static final long DAY = HOUR*24;
  public static final long WEEK = DAY*7;

  private Long m_id;
  private long m_maxMailIntern = 2*WEEK;
  private long m_maxIdleInbox = 4*WEEK;
  private byte[] m_secretKey;
  private int m_digestHOD;//Hour Of Day
  private int m_digestMOD;//Minute Of Day
  private long m_maxQuarantineSz;
  private List m_addressRemaps;
  private List m_allowedAddressPatterns;
/*
  private EmailAddressPair workaround1 = new EmailAddressPair("x", "X");
  private EmailAddressRule workaround2 = new EmailAddressRule("x");


  public void workaround(EmailAddressRule w,
    EmailAddressPair p) {
    if(p != null) {
      p.getAddress1();
    }
  }
*/  
  
  /**
    * @hibernate.id
    * column="SETTINGS_ID"
    * generator-class="native"
    */
  private Long getId() {
    return m_id;
  }

  private void setId(Long id) {
    m_id = id;
  }

  /**
   * Get the list of {@link com.metavize.tran.mail.papi.EmailAddressRule EmailAddressRule}
   * objects, defining the address patterns for-which this server will quarantine emails.
   * The patterns are based on email address syntax ("local@domain").  However, a limited
   * glob syntax is also supported ("*@domain").  The glob matches any characters (0 or more).
   * This should not be confused with "real" regex which is not supported.  Only glob.
   * <br>
   * @return a List of com.metavize.tran.mail.papi.EmailAddressRule
   *         objects (can't use 1.5 template syntax for hibernate reasons).
   *
   * @hibernate.list
   * cascade="all-delete-orphan"
   * @hibernate.collection-key
   * column="SETTINGS_ID"
   * @hibernate.collection-index
   * column="POSITION"
   * @hibernate.collection-one-to-many
   * class="com.metavize.tran.mail.papi.EmailAddressRule"
   */  
  public List getAllowedAddressPatterns() {
    if(m_allowedAddressPatterns == null) {
      setAllowedAddressPatterns(null);
    }
    return m_allowedAddressPatterns;  
  }

  public void setAllowedAddressPatterns(List patterns) {
    if(patterns == null) {
      patterns = new java.util.ArrayList();
    }
    m_allowedAddressPatterns = patterns;
  }  

  /**
   * Set a List of {@link com.metavize.tran.mail.papi.EmailAddressPairRule EmailAddressPairRule}
   * objects, defining the "remappings" supported by this server.  Remappings
   * associate a pattern with an address.  For example, to cause all emails for
   * "sales@foo.com" to be quarantined in the inbox of "joe.salesguy@foo.com"
   * "sales@foo.com" is the pattern and "joe.salesguy@foo.com" is the mapped
   * address.  Since the "EmailAddressPairRule" class is generic (doesn't have
   * "pattern" and "address" members, the "address1" member is the pattern and
   * "address2" is the remap.
   * <br>
   * The pattern also suports limited wildcards, based on glob ("*") syntax.  The
   * glob matches any characters.  For example, to cause all mails for "foo.com"
   * to be quarantined within "fred@moo.com"'s inbox, the pattern would be
   * "*@foo.com" and the remapping would be "fred@moo.com".
   *
   *
   * @return a List of com.metavize.tran.mail.papi.EmailAddressPairRule
   *         objects (can't use 1.5 template syntax for hibernate reasons).
   *
   * @hibernate.list
   * cascade="all-delete-orphan"
   * @hibernate.collection-key
   * column="SETTINGS_ID"
   * @hibernate.collection-index
   * column="POSITION"
   * @hibernate.collection-one-to-many
   * class="com.metavize.tran.mail.papi.EmailAddressPairRule"
   */
  public List getAddressRemaps() {
    if(m_addressRemaps == null) {
      setAddressRemaps(null);
    }
    return m_addressRemaps;
  }

  /**
   * Set the list of addresses to be remapped.  The argument
   * is a list of {@link com.metavize.tran.mail.papi.EmailAddressPairRule EmailAddressPairRules}
   * (cannot use templating because of hibernate).  These represent
   * the <b>ordered</b> collection of pairs to be remapped.  Note also
   * that the <code>addr1</code> property of the pair is the
   * "map from" (i.e. <code>addr1</code> of "*@foo.com" and
   * <code>addr2</code> of "trash@foo.com").
   *
   * @param remaps the list of remapped addresses.
   */
  public void setAddressRemaps(List remaps) {
    if(remaps == null) {
      remaps = new java.util.ArrayList();
    }
    m_addressRemaps = remaps;
  }
  

  /**
   *
   * @hibernate.property
   * column="MAX_QUARANTINE_SZ"
   * not-null="true"   
   */   
  public long getMaxQuarantineTotalSz() {
    return m_maxQuarantineSz;
  }

  /**
   * Set the total size (in bytes) that the quarantine
   * is permitted to consume on disk.
   *
   * @param max the max size
   */
  public void setMaxQuarantineTotalSz(long max) {
    m_maxQuarantineSz = max;
  }
  

  /**
    *
    * @return the Hour of the day when digest emails should be sent.
    * @hibernate.property
    * column="HOUR_IN_DAY"
    */
  public int getDigestHourOfDay() {
    return m_digestHOD;
  }

  /**
   * Set the Hour of the day when digest emails should
   * be sent.  This should be a value between 0 and 23 (inclusive
   * of both ends).
   *
   * @param hod the hour of the day
   */
  public void setDigestHourOfDay(int hod) {
    m_digestHOD = hod;
  }

  /**
    *
    * @return the Minute of the day when digest emails should be sent.
    * @hibernate.property
    * column="MINUTE_IN_DAY"
    */
  public int getDigestMinuteOfDay() {
    return m_digestMOD;
  }

  /**
   * Set the Minute of the day when digest emails should
   * be sent.  This should be a value between 0 and 59 (inclusive
   * of both ends).
   *
   * @param mod the minute of the day
   */
  public void setDigestMinuteOfDay(int mod) {
    m_digestMOD = mod;
  }

  /**
    * Password, encrypted with password utils.
    *
    * @return encrypted password bytes.
    * @hibernate.property
    * type="binary"
    * length="32"
    * column="SECRET_KEY"
    * not-null="true"
    */
  public byte[] getSecretKey() {
    return m_secretKey;
  }

  /**
   * Set the key used to create authentication "tokens".  This should
   * really only ever be set once for a given deployment (or else
   * folks with older emails won't be able to use the links).
   */
  public void setSecretKey(byte[] key) {
    m_secretKey = key;
  }  

  /**
   *
   * @hibernate.property
   * column="MAX_INTERN_TIME"
   * not-null="true"   
   */  
  public long getMaxMailIntern() {
    return m_maxMailIntern;
  }

  /**
   * Get the longest period of time that a mail may be interned
   * before it is automagically purged.
   */
  public void setMaxMailIntern(long max) {
    m_maxMailIntern = max;
  }
  
  /**
   *
   * @hibernate.property
   * column="MAX_IDLE_INBOX_TIME"
   * not-null="true"   
   */
  public long getMaxIdleInbox() {
    return m_maxMailIntern;
  }

  /**
   * Set the maximum relative time (in milliseconds)
   * that inboxes can be idle before they are implicitly
   * cleaned-up.  This is a relative unit (ie "2 weeks")
   */
  public void setMaxIdleInbox(long max) {
    m_maxMailIntern = max;
  }  
  
  
}
