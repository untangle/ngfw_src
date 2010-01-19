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
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

/**
 * Settings for the AddressBook
 */
@Entity
@Table(name="u_ab_settings", schema="settings")
public class AddressBookSettings implements Serializable {
    private static final long serialVersionUID = 1981170448212868734L;

    private Long id;
    private RepositorySettings m_aDSettings;
    private AddressBookConfiguration m_configuration;
    private RadiusServerSettings radiusServerSettings;
    private boolean radiusEnabled;

    public AddressBookSettings() { }

    @Id
    @Column(name="settings_id")
    @GeneratedValue
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="ad_repo_settings", nullable=false)
    public RepositorySettings getADRepositorySettings() {
        return m_aDSettings;
    }
    public void setADRepositorySettings(RepositorySettings aDSettings) {
        m_aDSettings = aDSettings;
    }

    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="radius_server_settings", nullable=false)
    public RadiusServerSettings getRadiusServerSettings() {
        return radiusServerSettings;
    }

    public void setRadiusServerSettings(RadiusServerSettings rss) {
        radiusServerSettings = rss;
    }

    @Column(name="ab_configuration")
    @Type(type="com.untangle.uvm.addrbook.AddressBookConfigurationUserType")
    public AddressBookConfiguration getAddressBookConfiguration() {
        return m_configuration;
    }

    public void setAddressBookConfiguration(AddressBookConfiguration c) {
        m_configuration = c;
    }

    @Column(name="radius_enabled")
    public boolean getRadiusEnabled() {
        return radiusEnabled;
    }

    public void setRadiusEnabled(boolean b) {
        radiusEnabled = b;
    }

}
