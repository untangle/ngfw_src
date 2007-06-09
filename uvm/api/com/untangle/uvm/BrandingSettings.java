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

package com.untangle.uvm;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Contains properties that a vendor may use to rebrand the product.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="mvvm_branding_settings", schema="settings")
public class BrandingSettings implements Serializable
{
    private Long id;
    private String companyName = "Untangle";
    private String companyUrl = "http://www.untangle.com";
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

    /**
     * Get the vendor name.
     *
     * @return vendor name.
     */
    @Column(name="company_name")
    public String getCompanyName()
    {
        return null == companyName ? "Untangle" : companyName;
    }

    public void setCompanyName(String companyName)
    {
        this.companyName = companyName;
    }

    /**
     * Get the vendor URL.
     *
     * @return vendor url
     */
    @Column(name="company_url")
    public String getCompanyUrl()
    {
        return null == companyUrl ? "http://www.untangle.com" : companyUrl;
    }

    public void setCompanyUrl(String companyUrl)
    {
        this.companyUrl = companyUrl;
    }

    /**
     * The vendor logo to use, null means use the default Untangle
     * logo.
     *
     * @return GIF image bytes, null if Untangle logo.
     */
    public byte[] getLogo()
    {
        return logo;
    }

    public void setLogo(byte[] logo)
    {
        this.logo = logo;
    }

    /**
     * Get the vendor contact name.
     *
     * @return vendor contact name.
     */
    @Column(name="contact_name")
    public String getContactName()
    {
        return null == contactName ? "Your System Administrator" : contactName;
    }

    public void setContactName(String name)
    {
        this.contactName = contactName;
    }

    /**
     * Get the vendor contact email.
     *
     * @return vendor contact email.
     */
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
