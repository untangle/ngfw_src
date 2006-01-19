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
 * Settings for the AddressBook repository (really,
 * a bunch of LDAP settings).
 *
 * @hibernate.class
 * table="AB_REPOSITORY_SETTINGS"
 */
public class RepositorySettings
  implements java.io.Serializable {
  
  private static final long serialVersionUID = 1856246303246961114L;

  private Long id;
  private String m_superuserDN;
  private String m_superuserPass;
  private String m_searchBase;
  private String m_ldapHost;
  private int m_ldapPort;

  public RepositorySettings() {
  }

  public RepositorySettings(String superuserDN,
    String superuserPass,
    String searchBase,
    String ldapHost,
    int ldapPort) {
      
    m_superuserDN = superuserDN;
    m_superuserPass = superuserPass;
    m_searchBase = searchBase;
    m_ldapHost = ldapHost;
    m_ldapPort = ldapPort;

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
   * @hibernate.property
   * column="SUPERUSER_DN"
   */
  public String getSuperuserDN() {
    return m_superuserDN;
  }
  
  public void setSuperuserDN(String dn) {
    m_superuserDN = dn;
  }



  /**
   * @hibernate.property
   * column="SUPERUSER_PASS"
   */  
  public String getSuperuserPass() {
    return m_superuserPass;
  }

  public void setSuperuserPass(String pass) {
    m_superuserPass = pass;
  }




  /**
   * Get the base for searches.
   */
  public void setSearchBase(String sb) {
    m_searchBase = sb;
  }

  /**
   * @hibernate.property
   * column="SEARCH_BASE"
   */    
  public String getSearchBase() {
    return m_searchBase;
  }



  public void setLDAPHost(String ldapHost) {
    m_ldapHost = ldapHost;
  }

  /**
   * @hibernate.property
   * column="LDAP_HOST"
   */     
  public String getLDAPHost() {
    return m_ldapHost;
  }



  public void setLDAPPort(int port) {
    m_ldapPort = port;
  }

  /**
   * @hibernate.property
   * column="PORT"
   */
  public int getLDAPPort() {
    return m_ldapPort;
  }
  
  

  


}