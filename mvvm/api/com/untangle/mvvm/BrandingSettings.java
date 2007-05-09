/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: ConnectivityTester.java 8515 2007-01-03 00:13:24Z amread $
 */

package com.untangle.mvvm;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="mvvm_branding_settings", schema="settings")
public class BrandingSettings implements Serializable
{
    private Long id;
    private String companyName = "Untangle";
    private byte[] logo = null;
    private String contactName = "Your System Administrator";
    private String contactEmail = null;

    public BrandingSettings() { }

    @Id
    @Column(name="settings_id")
    @GeneratedValue
    Long getId()
    {
        return id;
    }

    void setId(Long id)
    {
        this.id = id;
    }

    @Column(name="company_name")
    public String getCompanyName()
    {
        return companyName;
    }

    public void setCompanyName(String companyName)
    {
        this.companyName = companyName;
    }

    public byte[] getLogo()
    {
        return logo;
    }

    public void setLogo(byte[] logo)
    {
        this.logo = logo;
    }

    @Column(name="contact_name")
    public String getContactName()
    {
        return contactName;
    }

    public void setContactName(String name)
    {
        this.contactName = contactName;
    }

    @Column(name="contact_email")
    public String getContactEmail()
    {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail)
    {
        this.contactEmail = contactEmail;
    }
}
