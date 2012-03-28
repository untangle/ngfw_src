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

import java.util.LinkedList;
import java.util.List;
import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.ValidateException;

/**
 * A site network for a client.  Done this way so the client site
 * networks and the server site networks are in their own tables.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class VpnSite extends VpnClient
{
    private static final IPAddress EMPTY_ADDR = new IPAddress( null );

    // List of addresses at this site,
    // initially, may not be supported, just use one address.
    private List<SiteNetwork>     exportedAddressList;

    public VpnSite()
    {
        /* XXXXXXXXXXXXXXXXXXXXXXXXXXXXX This should have all of the stuff
         * about exports in it, but for now just keep it in both places */
    }

    /**
     * The list of exported networks for this site.
     * Should rename the column from client_id to site_id.
     *
     * @return the list of exported networks for this site.
     */
    public List<SiteNetwork> getExportedAddressList()
    {
        if ( this.exportedAddressList == null ) this.exportedAddressList = new LinkedList<SiteNetwork>();

        if (this.exportedAddressList != null) this.exportedAddressList.removeAll(java.util.Collections.singleton(null));
        return this.exportedAddressList;
    }

    public void setExportedAddressList( List<SiteNetwork> exportedAddressList )
    {
        this.exportedAddressList = exportedAddressList;
    }

    /**
     * XXXX This is a convenience method that doesn't scale if there are
     * multiple networks and netmasks
     */
    public SiteNetwork trans_getSiteNetwork()
    {
        List<SiteNetwork> list = getExportedAddressList();

        SiteNetwork site;
        if ( list.size() < 1 ) {
            site = new SiteNetwork();
            site.setNetwork( EMPTY_ADDR );
            site.setNetmask( EMPTY_ADDR );
            site.setLive( true );
            list.add( site );
        } else {
            site = list.get( 0 );
        }

        return site;
    }

    public void trans_setSiteNetwork( IPAddress network, IPAddress netmask )
    {
    	List<SiteNetwork>  list = getExportedAddressList();

        SiteNetwork site;
        if ( list.size() < 1 ) {
            site = new SiteNetwork();
            list.add( site );
        } else {
            site = list.get( 0 );
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
