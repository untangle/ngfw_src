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
    
    // XXX SERIALVER private static final long serialVersionUID = 4143567998376955882L;
    
    private IPaddr address;            // may be null.
    
    // The address group to pull this client address
    private VpnGroup group;

    private String certificateStatus = "Unknown";

    /* A 96-bit random string to be emailed out.  When the user goes
     * to the page and uses this key, they are allowed to download the
     * configuration for one session */
    private String distributionKey;
    
    /* Most likely unused for early versions but a nice possiblity for
     * the future */
    private String distributionPassword;

    /* Not stored to the database hibernate */

    /* Set to true to tell the transform to distribute the configuration files
     * for this client */
    private boolean distributeClient = false;

    /* Email addresss where to send the client */
    private String distributionEmail = null;
    
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
     * cascade="all"
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

    /**
     * Static address for this client, this cannot be set, it is assigned.
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
     * @return the key required to download the client.
     * @hibernate.property
     * column="dist_key"
     */
    public String getDistributionKey()
    {
        return this.distributionKey;
    }

    public void setDistributionKey( String distributionKey )
    {
        this.distributionKey = distributionKey;
    }

    /**
     * @return the key password to download the client.(unused)
     * @hibernate.property
     * column="dist_passwd"
     */
    public String getDistributionPassword()
    {
        return this.distributionPassword;
    }

    public void setDistributionPassword( String distributionPassword )
    {
        this.distributionPassword = distributionPassword;
    }

    /* Indicates whether or not the server should distribute a config for this client */
    public boolean getDistributeClient()
    {
        return this.distributeClient;
    }

    public void setDistributeClient( boolean distributeClient )
    {
        this.distributeClient = distributeClient;
    }
    
    public boolean getDistributeUsb()
    {
        return ( this.distributionEmail == null );
    }
    
    public void setDistributeUsb( boolean distributeUsb )
    {
        /* In order to distribute over usb, you have to null out the email address  *
         * in order to distribute over email, you have to just set the distribution *
         * email. */
        if ( distributeUsb ) this.distributionEmail = null;
    }
    
    public String getDistributionEmail()
    {
        return this.distributionEmail;
    }

    public void setDistributionEmail( String email )
    {
        this.distributionEmail = email;
    }

    /* This is the name that is used as the common name in the certificate */
    public String getInternalName()
    {
        return getName().trim().toLowerCase().replace( " ", "_" );
    }

    public void validate() throws ValidateException
    {
        /* XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX */
        String internalName = getInternalName();
        String name = getName();

        if ( internalName.length() == 0 ) {
            throw new ValidateException( "A client cannot have an empty name" );
        }
        
        if ( !NAME_PATTERN.matcher( name ).matches()) {
            throw new ValidateException( "A client name should only contains numbers, letters, " +
                                         "dashes and periods.  Spaces are not allowed. " + name );
        }

        if ( this.group == null ) {
            throw new ValidateException( "Group cannot be null" );
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

    public boolean getIsEdgeGuard()
    {
        /* A single client can never be an edgeguard */
        return false;
    }

    /* If the client is alive and the group it is in is enabled */
    public boolean isEnabled()
    {
        return isLive() && ( this.group != null ) && ( this.group.isLive());
    }

    static
    {
        Pattern p;

        try {
            /* Limited to prevent funny shell hacks(the name goes to the shell) */
            p = Pattern.compile( "^[A-Za-z0-9]([-_.0-9A-Za-z]*[0-9A-Za-z])?$" );
        } catch ( PatternSyntaxException e ) {
            System.err.println( "Unable to intialize the host label pattern" );
            p = null;
        }
        
        NAME_PATTERN = p;        
    }
}
