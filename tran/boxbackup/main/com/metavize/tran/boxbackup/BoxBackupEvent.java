/*
 * Copyright (c) 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.boxbackup;

import java.util.Date;
import java.util.Iterator;
import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.logging.SyslogBuilder;



/**
 * ...name says it all...
 *
 * 
 * @hibernate.class
 * table="TR_BOXBACKUP_EVT"
 * mutable="false"
 */
public class BoxBackupEvent
  extends LogEvent {

  private static final long serialVersionUID = 5563835539346280962L;
  
  private boolean m_success;
  private String m_detail;

  public BoxBackupEvent() { }

  public BoxBackupEvent(boolean success,
    String detail) {
    m_success = success;
    m_detail = detail;
  }


  /**
   * Was the backup a success
   *
   * @hibernate.property
   * column="SUCCESS"
   */
  public boolean isSuccess() {
    return m_success;
  }
  public void setSuccess(boolean success) {
    m_success = success;
  }


  /**
   * Detail.  Only really interesting if
   * things fail.
   *
   * @hibernate.property
   * column="DESCRIPTION"
   */
  public String getDetail() {
    return m_detail;
  }
  public void setDetail(String detail) {
    m_detail = detail;
  }

  public void appendSyslog(SyslogBuilder sb) {
    sb.startSection("info");
    sb.addField("success", isSuccess());
    sb.addField("detail", getDetail());
  }
}
