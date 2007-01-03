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

package com.untangle.mvvm.addrbook;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Settings for the AddressBook repository (really,
 * a bunch of LDAP settings).
 */
@Entity
@Table(name="ab_repository_settings")
public class RepositorySettings implements Serializable
{
    private static final long serialVersionUID = 1856246303246961114L;

    private Long id;
    private String m_superuser;
    private String m_superuserPass;
    private String m_domain;
    private String m_ldapHost;
    private String m_ouFilter;
    private int m_ldapPort;

    public RepositorySettings() { }

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

    @Id
    @Column(name="settings_id")
    @GeneratedValue
    private Long getId() {
        return id;
    }

    private void setId(Long id) {
        this.id = id;
    }

    public String getSuperuser() {
        return m_superuser;
    }

    public void setSuperuser(String dn) {
        m_superuser = dn;
    }

    @Column(name="superuser_pass")
    public String getSuperuserPass() {
        return m_superuserPass;
    }

    public void setSuperuserPass(String pass) {
        m_superuserPass = pass;
    }

    /**
     * Get the AD domain
     */
    public String getDomain() {
        return m_domain;
    }

    public void setDomain(String domain) {
        m_domain = domain;
    }

    @Column(name="ldap_host")
    public String getLDAPHost() {
        return m_ldapHost;
    }

    public void setLDAPHost(String ldapHost) {
        m_ldapHost = ldapHost;
    }

    @Column(name="port", nullable=false)
    public int getLDAPPort() {
        return m_ldapPort;
    }

    public void setLDAPPort(int port) {
        m_ldapPort = port;
    }

    @Column(name="ou_filter")
    public String getOUFilter() {
        return m_ouFilter;
    }

    public void setOUFilter(String ouFilter) {
        m_ouFilter = ouFilter;
    }
}
