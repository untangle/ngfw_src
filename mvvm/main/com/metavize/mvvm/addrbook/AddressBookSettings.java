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

package com.metavize.mvvm.addrbook;


/**
 * Settings for the AddressBook
 *
 * TODO This will become a hibernate class
 */
public class AddressBookSettings
  implements java.io.Serializable {
  
  private static final long serialVersionUID = 1981170448212868734L;

  private Long id;
  private RepositorySettings m_aDSettings;
  private AddressBookConfiguration m_configuration;


  public AddressBookSettings() {
  }


  private Long getId() {
      return id;
  }

  private void setId(Long id) {
      this.id = id;
  }


  public RepositorySettings getADRepositorySettings() {
    return m_aDSettings;
  }
  
  public void setADRepositorySettings(RepositorySettings aDSettings) {
    m_aDSettings = aDSettings;
  }

  public AddressBookConfiguration getAddressBookConfiguration() {
    return m_configuration;
  }

  public void setAddressBookConfiguration(AddressBookConfiguration c) {
    m_configuration = c;
  }
}