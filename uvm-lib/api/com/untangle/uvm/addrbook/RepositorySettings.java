/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.addrbook;

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
@Table(name="u_ab_repository_settings")
public class RepositorySettings implements Serializable
{

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
