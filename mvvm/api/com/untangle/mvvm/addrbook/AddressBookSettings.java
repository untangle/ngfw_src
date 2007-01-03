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
@Table(name="ab_settings", schema="settings")
public class AddressBookSettings implements Serializable {
    private static final long serialVersionUID = 1981170448212868734L;

    private Long id;
    private RepositorySettings m_aDSettings;
    private AddressBookConfiguration m_configuration;


    public AddressBookSettings() { }

    @Id
    @Column(name="settings_id")
    @GeneratedValue
    private Long getId() {
        return id;
    }

    private void setId(Long id) {
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

    @Column(name="ab_configuration")
    @Type(type="com.untangle.mvvm.addrbook.AddressBookConfigurationUserType")
    public AddressBookConfiguration getAddressBookConfiguration() {
        return m_configuration;
    }

    public void setAddressBookConfiguration(AddressBookConfiguration c) {
        m_configuration = c;
    }
}
