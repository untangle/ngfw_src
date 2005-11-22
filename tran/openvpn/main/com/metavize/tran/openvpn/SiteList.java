/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.openvpn;

import java.io.Serializable;

import java.util.List;
import java.util.LinkedList;

import com.metavize.mvvm.tran.Validatable;
import com.metavize.mvvm.tran.ValidateException;

public class SiteList implements Serializable, Validatable
{ 
    List<VpnSite> siteList;

    public SiteList()
    {
        this( new LinkedList<VpnSite>());
    }

    public SiteList( List<VpnSite> siteList )
    {
        this.siteList = siteList;
    }

    public List<VpnSite> getSiteList()
    {
        return this.siteList;
    }
    
    public void setSiteList( List<VpnSite> siteList )
    {
        this.siteList = siteList;
    }
   
    /** 
     * Validate the object, throw an exception if it is not valid */
    public void validate() throws ValidateException
    {
        for ( VpnSite site : this.siteList ) site.validate();

        /* XXX Check for overlap, and check for conflicts with the network settings */
    }
}
