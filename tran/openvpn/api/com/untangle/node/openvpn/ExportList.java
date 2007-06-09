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
package com.untangle.tran.openvpn;

import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;

import com.untangle.mvvm.tran.AddressValidator;
import com.untangle.mvvm.tran.AddressRange;
import com.untangle.mvvm.tran.Validatable;
import com.untangle.mvvm.tran.ValidateException;

public class ExportList implements Serializable, Validatable
{ 
    private static final long serialVersionUID = -6370773131855832786L;
    
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

    List<AddressRange> buildAddressRange()
    {
        List<AddressRange> checkList = new LinkedList<AddressRange>();

        for ( ServerSiteNetwork export : this.exportList ) {
            checkList.add( AddressRange.makeNetwork( export.getNetwork().getAddr(), 
                                                     export.getNetmask().getAddr()));
        }

        return checkList;
    }

   
    /** 
     * Validate the object, throw an exception if it is not valid */
    public void validate() throws ValidateException
    {
        for ( ServerSiteNetwork export : this.exportList ) export.validate();
        
        /* Determine if all of the addresses are unique */
        AddressValidator.getInstance().validate( buildAddressRange());
    }
}
