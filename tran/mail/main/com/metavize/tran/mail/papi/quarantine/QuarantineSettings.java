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
//BILL - see com.metavize.mvvm.security.User for example of byte[]

/**
 * Settings for the quarantine stuff
 */
public class QuarantineSettings
  implements java.io.Serializable {

  public static final long HOUR = 1000*60*60;
  public static final long DAY = HOUR*24;
  public static final long WEEK = DAY*7;

  private long m_maxMailIntern = 2*WEEK;
  private long m_maxIdleInbox = 4*WEEK;
  private byte[] m_secretKey;
  private String m_digestFrom;
  private int m_digestHOD;//Hour Of Day
  private long m_maxQuarantineSz;

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