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

package com.untangle.tran.mail.impl.quarantine.store;

import java.util.HashSet;


/**
 * Global account lock.
 *
 * Assumes all addresses have been lower-cased
 */
class AddressLock {

  private HashSet<String> m_set;
  private int m_waitCount = 0;
  
  AddressLock() {
    m_set = new HashSet<String>();
  }

  /**
   * Attempt to lock the given address
   *
   * @param address the <b>lower caseed</b> address
   * @param maxWait the max time to wait
   *
   * @return true if locked, false if it could not be locked
   */
  synchronized boolean tryLock(String address, long maxWait) {
    //I'll assume getting clock time isn't too expensive,
    //so it can be done in a sync block
    long giveup = System.currentTimeMillis() + maxWait;
    while(m_set.contains(address)) {
      long remainder = giveup - System.currentTimeMillis();
      if(remainder < 2) {
        return false;
      }
      m_waitCount++;
      if(!waitImpl(remainder)) {
        m_waitCount--;
        return false;
      }
      m_waitCount--;
    }
    m_set.add(address);
    return true;
  }  

  /**
   * Lock the given address
   *
   * @param address the <b>lower caseed</b> address
   *
   */
  synchronized void lock(String address) {
    while(m_set.contains(address)) {
      m_waitCount++;
      if(!waitImpl()) {
        m_waitCount--;
        return;//Assume shutdown
      }
      m_waitCount--;
    }
    m_set.add(address);
  }

  /**
   * Attempt to lock the given address
   *
   * @param address the <b>lower caseed</b> address
   *
   * @return true if the address was locked
   *
   */  
  synchronized boolean tryLock(String address) {
    return tryLock(address, -1);
  }

  /**
   * Unlock a previously locked address.
   *
   * @param address the <b>lower caseed</b> address   
   */
  synchronized void unlock(String address) {
    m_set.remove(address);
    if(m_waitCount > 0) {
      notifyAll();
    }
  }

  private boolean waitImpl(long time) {
    try {
      wait(time);
      return true;
    }
    catch(Exception ex) {
      return false;
    }
  }  

  private boolean waitImpl() {
    try {
      wait();
      return true;
    }
    catch(Exception ex) {
      return false;
    }
  }

}