/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.openvpn;

import java.io.Serializable;

import com.untangle.uvm.node.Validatable;
import com.untangle.uvm.node.ValidateException;

public class CertificateParameters implements Serializable, Validatable
{
    
    private String organization;
    private String domain;
    private String country;
    private String state;
    private String locality;
    private boolean storeCaUsb;

    public CertificateParameters()
    {
        this( "", "", "", "", "", false );
    }

    public CertificateParameters( String organization, String domain, String country, String state, 
                                  String locality, boolean storeCaUsb )
    {
        this.organization = organization;
        this.domain  = domain;
        this.country = country;
        this.state = state;
        this.locality = locality;
        this.storeCaUsb = storeCaUsb;
    }

    public String getOrganization()
    {
        return this.organization;
    }

    public void setOrganization( String organization )
    {
        this.organization = organization;
    }

    public String getDomain()
    {
        return this.domain;
    }

    public void setDomain( String domain )
    {
        this.domain = domain;
    }


    public String getCountry()
    {
        return this.country;
    }

    public void setCountry( String country )
    {
        this.country = country;
    }

    public String getState()
    {
        return this.state;
    }

    public void setState( String state )
    {
        this.state = state;
    }

    public String getLocality()
    {
        return this.locality;
    }

    public void setLocality( String locality )
    {
        this.locality = locality;
    }

    public boolean getStoreCaUsb()
    {
        return this.storeCaUsb;
    }

    public void setStoreCaUsb( boolean storeCaUsb )
    {
        this.storeCaUsb = storeCaUsb;
    }
    
    /** 
     * Validate the object, throw an exception if it is not valid
     */
    public void validate() throws ValidateException
    {
        if ( this.country.length() != 2  && this.country.length() != 0 ) {
            throw new ValidateException( "Country is two characters. " + this.country );
        }
    }
}
