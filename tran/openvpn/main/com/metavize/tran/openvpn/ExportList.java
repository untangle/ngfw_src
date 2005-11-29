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

public class ExportList implements Serializable, Validatable
{ 
    // XXX SERIALVER private static final long serialVersionUID = 1032713361795879615L;
    
    List<ServerSiteNetwork> exportList;

    public ExportList()
    {
        this( new LinkedList<ServerSiteNetwork>());
    }

    public ExportList( List<ServerSiteNetwork> exportList )
    {
        this.exportList = exportList;
    }

    public List<ServerSiteNetwork> getExportList()
    {
        return this.exportList;
    }
    
    public void setExportList( List<ServerSiteNetwork> exportList )
    {
        this.exportList = exportList;
    }
   
    /** 
     * Validate the object, throw an exception if it is not valid */
    public void validate() throws ValidateException
    {
        for ( ServerSiteNetwork export : this.exportList ) export.validate();

        /* XXX Check for overlap, and check for conflicts with the network settings */
    }
}
