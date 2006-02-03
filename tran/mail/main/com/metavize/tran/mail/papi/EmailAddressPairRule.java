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
 * Class used to associate two email addresses
 *
 * @hibernate.class
 * table="EMAIL_ADDR_PAIR_RULE"
 */
public class EmailAddressPairRule
  extends Rule
  implements java.io.Serializable {

  private static final long serialVersionUID = 4188555156332337464L;


  private String m_addr1;
  private String m_addr2;
  private Long m_id;

  public EmailAddressPairRule() {
    this(null, null);  
  }

  public EmailAddressPairRule(String addr1, String addr2) {
    m_addr1 = addr1;
    m_addr2 = addr2;
  }


  public void setAddress1(String addr1) {
    m_addr1 = addr1;
  }

  
  /**
   *
   * @hibernate.property
   * column="ADDRESS1"
   * not-null="true"
   */
  public String getAddress1() {
    return m_addr1;
  }

  public void setAddress2(String addr2) {
    m_addr2 = addr2;
  }


  /**
   *
   * @hibernate.property
   * column="ADDRESS2"
   * not-null="true"
   */   
  public String getAddress2() {
    return m_addr2;
  }

}
