/*
 * Copyright (c) 2004, 2005 Metavize Inc.
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

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

import java.util.Map;
import java.util.HashMap;

import com.metavize.mvvm.tran.Rule;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.Validatable;

import com.metavize.mvvm.tran.ValidateException;

/**
 * the configuration for a vpn client.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="tr_openvpn_client"
 */
public class VpnClient extends Rule implements Validatable
{
    private static final Pattern NAME_PATTERN;
    
    // XXX update the serial version id
    // private static final long serialVersionUID = 4143567998376955882L;
    
    private IPaddr address;            // may be null.
    
    // The address group to pull this client address
    private VpnGroup group;

    // List of addresses at this site,
    // initially, may not be supported, just use one address.
    private List    exportedAddressList;

    private boolean isEdgeguard = false;

    private String certificateStatus = "Unknown";
    
    // constructors -----------------------------------------------------------
    
    /**
     * Hibernate constructor.
     */
    public VpnClient()
    {
    }

    // accessors --------------------------------------------------------------

    /**
     * @return The address group that this client belongs to.
     * @hibernate.many-to-one
     * class="com.metavize.tran.openvpn.VpnGroup"
     * column="group_id"
     */
    public VpnGroup getGroup()
    {
        return this.group;
    }

    public void setGroup( VpnGroup group )
    {
        this.group = group;
    }

    /* have to somehow convey that each user actually uses two address */

    /**
     * Static address of this client, this can only be set if address group is null.
     *   not available in bridge mode.
     *
     * @return static address of the machine.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="address"
     * sql-type="inet"
     */
    public IPaddr getAddress()
    {
        return this.address;
    }

    public void setAddress( IPaddr address )
    {
        this.address = address;
    }

    /**
     * The list of exported networks for this site.
     *
     * @return the list of exported networks for this site.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="client_id"
     * @hibernate.collection-index
     * column="position"
     * @hibernate.collection-one-to-many
     * class="com.metavize.tran.openvpn.ClientSiteNetwork"
     */
    public List getExportedAddressList()
    {
        if ( this.exportedAddressList == null ) this.exportedAddressList = new LinkedList();

        return this.exportedAddressList;
    }
    public void setExportedAddressList( List exportedAddressList )
    {
        this.exportedAddressList = exportedAddressList;
    }
    
    /**
     * @return whether the other side is an edgeguard.
     * @hibernate.property
     * column="is_edgeguard"
     */
    public boolean getIsEdgeguard()
    {
        return this.isEdgeguard;
    }

    public void setIsEdgeguard( boolean isEdgeguard )
    {
        this.isEdgeguard = isEdgeguard;
    }

    /* This is the name that is used as the common name in the certificate */
    public String getInternalName()
    {
        return getName().trim().toLowerCase().replace( " ", "_" );
    }

    public void validate() throws Exception
    {
        /* XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX */
        String name = getInternalName();
        if ( name.length() == 0 ) {
            throw new ValidateException( "A client cannot have an empty name" );
        }
        
        if ( !NAME_PATTERN.matcher( name ).matches()) {
            throw new ValidateException( "A client name should only contains numbers, letters, spaces and dashes and periods: " + name );
        }
    }

    public String getCertificateStatus()
    {
        return this.certificateStatus;
    }

    public void setCertificateStatusUnknown( )
    {
        this.certificateStatus = "unknown";
    }


    public void setCertificateStatusValid( )
    {
        this.certificateStatus = "valid";
    }

    public void setCertificateStatusRevoked( )
    {
        this.certificateStatus = "revoked";
    }

    static
    {
        Pattern p;

        try {
            /* Limited to prevent funny shell hacks(the name goes to the shell) */
            p = Pattern.compile( "^[A-Za-z]([-_ .0-9A-Za-z]*[0-9A-Za-z])?$" );
        } catch ( PatternSyntaxException e ) {
            System.err.println( "Unable to intialize the host label pattern" );
            p = null;
        }
        
        NAME_PATTERN = p;        
    }
}
