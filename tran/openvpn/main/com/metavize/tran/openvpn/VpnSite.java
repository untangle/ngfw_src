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

import com.metavize.mvvm.tran.IPaddr;

import java.util.List;
import java.util.LinkedList;

import com.metavize.mvvm.tran.ValidateException;

/**
 *  A site network for a client.  Done this way so the client site networks and the server 
 *  site networks are in their own tables.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="tr_openvpn_site"
 */
public class  VpnSite extends VpnClient
{
    // XXX Fixme
    // private static final long serialVersionUID = -3950973798403822835L;

    /* XXX This should be in one place */
    private static final IPaddr EMPTY_ADDR = new IPaddr( null );

    // List of addresses at this site,
    // initially, may not be supported, just use one address.
    private List    exportedAddressList;

    private boolean isEdgeGuard = false;
    
    public VpnSite()
    {
        /* XXXXXXXXXXXXXXXXXXXXXXXXXXXXX This should have all of the stuff 
         * about exports in it, but for now just keep it in both places */
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
    public boolean getIsEdgeGuard()
    {
        return this.isEdgeGuard;
    }

    public void setIsEdgeGuard( boolean isEdgeGuard )
    {
        this.isEdgeGuard = isEdgeGuard;
    }

    /**
     * XXXX This is a convenience method that doesn't scale if there are
     * multiple networks and netmasks
     */
    
    public ClientSiteNetwork getSiteNetwork( IPaddr network, IPaddr netmask )
    {
        List list = getExportedAddressList();

        ClientSiteNetwork site;
        if ( list.size() < 1 ) {
            site = new ClientSiteNetwork();
            site.setNetwork( EMPTY_ADDR );
            site.setNetmask( EMPTY_ADDR );
            site.setLive( true );
            list.add( site );
        } else {
            site = (ClientSiteNetwork)list.get( 0 );
        }
        
        return site;
    }

    public void setSiteNetwork( IPaddr network, IPaddr netmask )
    {
        List list = getExportedAddressList();
        
        ClientSiteNetwork site;
        if ( list.size() < 1 ) {
            site = new ClientSiteNetwork();
            list.add( site );
        } else {
            site = (ClientSiteNetwork)list.get( 0 );
        }
        
        site.setLive( true );
        site.setNetwork( network );
        site.setNetmask( netmask );
    }

    public void validate() throws ValidateException
    {
        super.validate();

        if (( this.exportedAddressList == null ) || ( this.exportedAddressList.size()  == 0 )) {
            throw new ValidateException( "A site must have at least one exported address" );
        }
    }
}

    
