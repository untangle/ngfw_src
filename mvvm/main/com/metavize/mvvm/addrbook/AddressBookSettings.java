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
 * @hibernate.class
 * table="AB_SETTINGS"
 */
public class AddressBookSettings
  implements java.io.Serializable {
  
  private static final long serialVersionUID = 1981170448212868734L;

  private Long id;
  private RepositorySettings m_aDSettings;
  private AddressBookConfiguration m_configuration;


  public AddressBookSettings() {
  }


  /**
   * @hibernate.id
   * column="SETTINGS_ID"
   * generator-class="native"
   * not-null="true"
   */  
  private Long getId() {
      return id;
  }

  private void setId(Long id) {
      this.id = id;
  }

  /**
    * @hibernate.many-to-one
    * column="AD_REPO_SETTINGS"
    * cascade="all"
    * not-null="true"
    */
  public RepositorySettings getADRepositorySettings() {
    return m_aDSettings;
  }
  
  public void setADRepositorySettings(RepositorySettings aDSettings) {
    m_aDSettings = aDSettings;
  }

  /**
    * @hibernate.property
    * column="AB_CONFIGURATION"
    * type="com.metavize.mvvm.addrbook.AddressBookConfigurationUserType"
    * not-null="true"
    */
  public AddressBookConfiguration getAddressBookConfiguration() {
    return m_configuration;
  }

  public void setAddressBookConfiguration(AddressBookConfiguration c) {
    m_configuration = c;
  }
}