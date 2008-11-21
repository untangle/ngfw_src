/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/uvm-lib/api/com/untangle/uvm/BrandingBaseSettings.java $
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
package com.untangle.uvm;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

import com.untangle.uvm.util.I18nUtil;

/**
 * Base Settings for the Branding.
 *
 * @author <a href="mailto:cmatei@untangle.com">Catalin Matei</a>
 * @version 1.0
 */
@Embeddable
public class BrandingBaseSettings implements Serializable {
    private String companyName = "Untangle";
    private String companyUrl = "http://www.untangle.com";
    private String contactName = "your network administrator";
    private String contactEmail = null;
    private boolean defaultLogo = true;
    
	public BrandingBaseSettings() {
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
     * Get the vendor contact name.
     *
     * @return vendor contact name.
     */
    @Column(name="contact_name")
    public String getContactName()
    {
        return null == contactName ? I18nUtil.marktr("your network administrator") : contactName;
    }

    public void setContactName(String name)
    {
        this.contactName = name;
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

    @Transient
    public String getContactHtml()
    {
        if (null != contactEmail && !contactEmail.trim().equals("")) {
            return "<a href='mailto:" + contactEmail + "'>" + contactName
                + "</a>";
        } else {
            return contactName;
        }
    }
    
    @Transient
    public boolean isDefaultLogo() {
		return defaultLogo;
	}
	public void setDefaultLogo(boolean defaultLogo) {
		this.defaultLogo = defaultLogo;
	}


    public void copy(BrandingBaseSettings settings)
    {
        settings.setCompanyName(this.companyName);
        settings.setCompanyUrl(this.companyUrl);
        settings.setContactName(this.contactName);
        settings.setContactEmail(this.contactEmail);
        settings.setDefaultLogo(this.defaultLogo);
    }
}
