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
  private String m_digestFrom;
  private int m_digestHOD;//Hour Of Day
  private int m_digestMOD;//Minute Of Day
  private long m_maxQuarantineSz;

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
    *
    * @return email address.
    * @hibernate.property
    * column="DIGEST_FROM"
    * not-null="true"
    */
  public String getDigestFrom() {
    return m_digestFrom;
  }

  /**
   * Set the name as it apears in the <b>mime</b> for the digest
   * sender.
   */
  public void setDigestFrom(String f) {
    m_digestFrom = f;
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
