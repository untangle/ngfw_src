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


/**
 * Class used to associate two email addresses
 *
 * @hibernate.class
 * table="EMAIL_ADDRESS_PAIR"
 */
public class EmailAddressPair {


  private String m_addr1;
  private String m_addr2;
  private Long m_id;

  public EmailAddressPair() {
    this(null, null);  
  }

  public EmailAddressPair(String addr1, String addr2) {
    m_addr1 = addr1;
    m_addr2 = addr2;
  }

  /**
    * @hibernate.id
    * column="PAIR_ID"
    * generator-class="native"
    */
  private Long getId() {
    return m_id;
  }

  private void setId(Long id) {
    m_id = id;
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
