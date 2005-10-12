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
 * TODO bscott this will be serializable, and one of the magical
 *      hibernate classes
 */
public class QuarantineSettings {

  public static final long HOUR = 1000*60*60;
  public static final long DAY = HOUR*24;
  public static final long WEEK = DAY*7;

  private long m_maxMailIntern = 2*WEEK;
  private long m_maxIdleInbox = 4*WEEK;


  public long getMaxMailIntern() {
    return m_maxMailIntern;
  }
  public void setMaxMailIntern(long max) {
    m_maxMailIntern = max;
  }
  

  public long getMaxIdleInbox() {
    return m_maxMailIntern;
  }

  public void setMaxIdleInbox(long max) {
    m_maxMailIntern = max;
  }  
  
  
}