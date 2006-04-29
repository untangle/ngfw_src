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
    private String m_superuser;
    private String m_superuserPass;
    private String m_domain;
    private String m_ldapHost;
    private String m_ouFilter;
    private int m_ldapPort;

    public RepositorySettings() {
    }

    public RepositorySettings(String superuser,
                              String superuserPass,
                              String domain,
                              String ldapHost,
                              int ldapPort) {
      
        m_superuser = superuser;
        m_superuserPass = superuserPass;
        m_domain = domain;
        m_ldapHost = ldapHost;
        m_ldapPort = ldapPort;
        m_ouFilter = "";
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
     * column="SUPERUSER"
     */
    public String getSuperuser() {
        return m_superuser;
    }
  
    public void setSuperuser(String dn) {
        m_superuser = dn;
    }

    public String getSuperuserDN() {
        return "cn=" + m_superuser;
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
     * @hibernate.property
     * column="DOMAIN"
     */    
    public String getDomain() {
        return m_domain;
    }


    /**
     * Get the AD domain
     */
    public void setDomain(String domain) {
        m_domain = domain;
    }

    public String getSearchBase() {
        return "cn=users," + domainComponents(m_domain);
    }

    private String domainComponents(String dom)
    {
        while (dom.endsWith("."))
            dom = dom.substring(0, dom.length() - 1);
        return "DC=" + dom.replace(".", ",DC=");
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

    /**
     * @hibernate.property
     * column="OU_FILTER"
     */  
    public String getOUFilter() {
        return m_ouFilter;
    }

    public void setOUFilter(String ouFilter) {
        m_ouFilter = ouFilter;
    }
}
