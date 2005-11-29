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

public class GroupList implements Serializable, Validatable
{
    // XXX SERIALVER private static final long serialVersionUID = 1032713361795879615L;
    
    List<VpnGroup> groupList;

    public GroupList()
    {
        this( new LinkedList<VpnGroup>());
    }

    public GroupList( List<VpnGroup> groupList )
    {
        this.groupList = groupList;
    }

    public List<VpnGroup> getGroupList()
    {
        return this.groupList;
    }
    
    public void setGroupList( List<VpnGroup> groupList )
    {
        this.groupList = groupList;
    }
   
    /** 
     * Validate the object, throw an exception if it is not valid */
    public void validate() throws ValidateException
    {
        /* XXXXXXXX */
        for ( VpnGroup group : this.groupList ) group.validate();

        /* XXX Check for overlap, and check for conflicts with the network settings */
    }
}
