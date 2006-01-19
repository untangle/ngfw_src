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
 * TODO This will become a hibernate class
 */
public class RepositorySettings
  implements java.io.Serializable {
  
  private static final long serialVersionUID = 1856246303246961114L;

  private Long id;
  private String m_superuserDN;
  private String m_superuserPass;
  private String m_userClass;
  private String m_mailAttributeAN;
  private String m_fullNameAN;
  private String m_uidAN;
  private String m_searchBase;
  private String m_ldapHost;
  private int m_ldapPort;

  public RepositorySettings() {
  }

  public RepositorySettings(String superuserDN,
    String superuserPass,
    String userClass,
    String mailAttributeAN,
    String fullNameAN,
    String uidAN,
    String searchBase,
    String ldapHost,
    int ldapPort) {
      
    m_superuserDN = superuserDN;
    m_superuserPass = superuserPass;
    m_userClass = userClass;
    m_mailAttributeAN = mailAttributeAN;
    m_fullNameAN = fullNameAN;
    m_uidAN = uidAN;
    m_searchBase = searchBase;
    m_ldapHost = ldapHost;
    m_ldapPort = ldapPort;

  }

  private Long getId() {
      return id;
  }

  private void setId(Long id) {
      this.id = id;
  }


  public String getSuperuserDN() {
    return m_superuserDN;
  }
  
  public void setSuperuserDN(String dn) {
    m_superuserDN = dn;
  }



  
  public String getSuperuserPass() {
    return m_superuserPass;
  }

  public void setSuperuserPass(String pass) {
    m_superuserPass = pass;
  }


  
  public String getUserClass() {
    return m_userClass;
  }

  /**
   * Set the name of the user class (i.e. "user" or "inetOrgPerson").
   */
  public void setUserClass(String userClass) {
    m_userClass = userClass;
  }

  

  /**
   * Get the name used for the mail attribute (i.e. "mail" or "email").
   */
  public String getMailAttributeAN() {
    return m_mailAttributeAN;
  }  

  public void setMailAttributeAN(String mailName) {
    m_mailAttributeAN = mailName;
  }


  /**
   * Get the name used for the full name attribute (i.e. "cn").
   */
  public String getFullNameAN() {
    return m_fullNameAN;
  }  

  public void setFullNameAN(String name) {
    m_fullNameAN = name;
  }



  /**
   * Get the name used for the uid attribute (i.e. "uid" or "sAMAccountName").
   */
  public String getUIDAN() {
    return m_uidAN;
  }  

  public void setUIDAN(String name) {
    m_uidAN = name;
  }    



  /**
   * Get the base for searches.
   */
  public void setSearchBase(String sb) {
    m_searchBase = sb;
  }

  public String getSearchBase() {
    return m_searchBase;
  }



  public void setLDAPHost(String ldapHost) {
    m_ldapHost = ldapHost;
  }

  public String getLDAPHost() {
    return m_ldapHost;
  }



  public void setLDAPPort(int port) {
    m_ldapPort = port;
  }
  public int getLDAPPort() {
    return m_ldapPort;
  }
  
  

  


}