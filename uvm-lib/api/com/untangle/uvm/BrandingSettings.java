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

package com.untangle.uvm;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embedded;
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
@Table(name="uvm_branding_settings", schema="settings")
public class BrandingSettings implements Serializable
{
    private Long id;
    private byte[] logo = null;
    private BrandingBaseSettings baseSettings = new BrandingBaseSettings();
    

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

    @Embedded
	public BrandingBaseSettings getBaseSettings() {
        if (null != baseSettings) {
            baseSettings.setDefaultLogo(null == logo);
        }

        return baseSettings;
	}

	public void setBaseSettings(BrandingBaseSettings baseSettings) {
		this.baseSettings = baseSettings;
	}
	
    public void copy(BrandingSettings settings)
    {
        settings.setLogo(this.logo);
        settings.getBaseSettings().setCompanyName(this.baseSettings.getCompanyName());
        settings.getBaseSettings().setCompanyUrl(this.baseSettings.getCompanyUrl());
        settings.getBaseSettings().setContactName(this.baseSettings.getContactName());
        settings.getBaseSettings().setContactEmail(this.baseSettings.getContactEmail());
    }

}
