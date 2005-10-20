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

package com.metavize.tran.mail.impl.quarantine;

import org.apache.log4j.Logger;
import java.util.GregorianCalendar;
import java.util.Calendar;



/**
 *
 */
class CronThread implements Runnable {

  private final Logger m_logger =
    Logger.getLogger(CronThread.class);  
  private Quarantine m_quarantine;
  private boolean m_done = false;
  private int m_hourInDay = 6;
  private long m_nextWakeup = System.currentTimeMillis() +
    1000*60*60*24;

  CronThread(Quarantine q) {
    m_quarantine = q;
  }

  synchronized void setHourInDay(int hid) {
    m_hourInDay = hid;
    notify();
  }

  /**
   * Stops the thread
   */
  synchronized void done() {
    m_done = true;
    notify();
  }

  public void run() {
    while(!m_done) {
      if(!doWait()) {
        return;
      }
      m_quarantine.cronCallback();
    }
  }

  /**
   * Return of false means we should exit
   */
  private synchronized boolean doWait() {
    try {
      wait(getMillisUntilNext(m_hourInDay));
      return !m_done;
    }
    catch(Exception ex) {
      m_logger.warn("Exception waiting", ex);
      return false;
    }
  }


  /**
   * Gets the number of milliseconds until the
   * next <code>hourOfDay</code> hour.
   */
  private long getMillisUntilNext(int hourOfDay) {
    GregorianCalendar calendar = new GregorianCalendar();
    while(calendar.get(Calendar.HOUR_OF_DAY) != hourOfDay) {
      calendar.add(Calendar.HOUR_OF_DAY, 1);
    }
    long ret = calendar.getTimeInMillis() - System.currentTimeMillis();
    return ret>0?
      ret:
      0;
  }  

  

}