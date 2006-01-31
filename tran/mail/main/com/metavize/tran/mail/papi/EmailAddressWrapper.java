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
 * Class used to hold an email address (needed for 
 * hibernate stuff).
 *
 * @hibernate.class
 * table="EMAIL_ADDRESS_WRAPPER"
 */
public class EmailAddressWrapper implements java.io.Serializable {

  private static final long serialVersionUID = 7226453350424547957L;


  private String m_addr;
  private Long m_id;

  public EmailAddressWrapper() {
    this(null);  
  }

  public EmailAddressWrapper(String addr) {
    m_addr = addr;
  }

  /**
    * @hibernate.id
    * column="ADDR_ID"
    * generator-class="native"
    */
  private Long getId() {
    return m_id;
  }

  private void setId(Long id) {
    m_id = id;
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
