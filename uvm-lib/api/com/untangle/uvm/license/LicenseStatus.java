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

package com.untangle.uvm.license;

import java.io.Serializable;
import java.util.Date;

import org.json.JSONBean;

/**
 * Class used to describe the licensing status of an application,
 * presently really only used for the GUI.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@JSONBean.Marker
public class LicenseStatus implements Serializable
{
    /* True if this product ever had a license, false otherwise, used
    * to distinguish products that have gone through a 30-day trial
    * that has expired and products that were never licensed. */
    private final boolean hasLicense;

    /** identifier for this product */
    private final String identifier;
    
    /* name of the type of license, unused if <code>hasLicense</code> is false */
    private final String type;

    /* Date that the license will expire, unused if <code>hasLicense</code> is false */
    private final Date expirationDate;
    
    /* Whether or not the license was expired when this object was created. */
    private final boolean isExpired;

    /* The amount of time remaining as a string */
    private final String timeRemaining;

    /* true if this is a trial */
    private final boolean isTrial;

    /* This is the mackage name for the license */
    private final String mackageName;
    
    public LicenseStatus( boolean hasLicense, String identifier, String mackageName, String type, 
                          Date expirationDate, String timeRemaining, boolean isTrial )
    {
        this.hasLicense = hasLicense;
        this.identifier = identifier;
        this.mackageName = mackageName;
        this.type = type;
        this.expirationDate = expirationDate;
        /* it is unstable if it doesn't have a license or the current time is after the expiration time */
        this.isExpired = !hasLicense || ( System.currentTimeMillis() > expirationDate.getTime());
        this.timeRemaining = timeRemaining;
        this.isTrial = isTrial;
    }

    @JSONBean.Getter
    public boolean hasLicense()
    {
        return this.hasLicense;
    }

    /** This is the identifier for this product */
    public String getIdentifier()
    {
        return this.identifier;
    }

    public String getType()
    {
        return type;
    }

    public Date getExpirationDate()
    {
        return this.expirationDate;
    }
    
    public String getTimeRemaining()
    {
        return this.timeRemaining;
    }

    public String getMackageName()
    {
        return this.mackageName;
    }
    
    @JSONBean.Getter
    public boolean isTrial()
    {
        return this.isTrial;
    }

    /* Done this way so that timezone issues between the GUI and the UVM */
    @JSONBean.Getter
    public boolean isExpired()
    {
        return this.isExpired;
    }

}

