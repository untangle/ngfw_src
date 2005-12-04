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
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;

import com.metavize.mvvm.tran.Validatable;
import com.metavize.mvvm.tran.ValidateException;

public class ClientList implements Serializable, Validatable
{ 
    // XXX SERIALVER private static final long serialVersionUID = 1032713361795879615L;

    List<VpnClient> clientList;

    public ClientList()
    {
        this( new LinkedList<VpnClient>());
    }

    public ClientList( List<VpnClient> clientList )
    {
        this.clientList = clientList;
    }

    public List<VpnClient> getClientList()
    {
        return this.clientList;
    }
    
    public void setClientList( List<VpnClient> clientList )
    {
        this.clientList = clientList;
    }
   
    /** 
     * Validate the object, throw an exception if it is not valid */
    public void validate() throws ValidateException
    {
        Set<String> nameSet = new HashSet<String>();
        for ( VpnClient client : this.clientList ) {
            client.validate();
            String name = client.getInternalName();
            if ( !nameSet.add( name )) {
                throw new ValidateException( "Client and site names must all be unique: '" + name + "'" );
            }
        }
    }
}
