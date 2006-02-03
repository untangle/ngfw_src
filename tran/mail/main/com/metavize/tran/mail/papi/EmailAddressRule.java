/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.papi;

import com.metavize.mvvm.tran.Rule;


/**
 * Class used to hold an email address (needed for 
 * hibernate stuff).
 *
 * @hibernate.class
 * table="EMAIL_ADDR_RULE"
 */
public class EmailAddressRule
  extends Rule
  implements java.io.Serializable {

  private static final long serialVersionUID = 7226453350424547957L;

  private String m_addr;

  public EmailAddressRule() {
    this(null);  
  }

  public EmailAddressRule(String addr) {
    m_addr = addr;
  }

  public void setAddress(String addr) {
    m_addr = addr;
  }

  
  /**
   *
   * @hibernate.property
   * column="ADDRESS"
   */
  public String getAddress() {
    return m_addr;
  }
}
