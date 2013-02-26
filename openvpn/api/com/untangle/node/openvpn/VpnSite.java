/**
 * $Id$
 */
package com.untangle.node.openvpn;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

/**
 * A site network for a client.  Done this way so the client site
 * networks and the server site networks are in their own tables.
 */
@SuppressWarnings("serial")
public class VpnSite extends VpnClient
{
    private List<SiteNetwork>     exportedAddressList;

    public VpnSite() {}

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
            site.setNetwork( null );
            site.setNetmask( null );
            site.setLive( true );
            list.add( site );
        } else {
            site = list.get( 0 );
        }

        return site;
    }

    public void trans_setSiteNetwork( InetAddress network, InetAddress netmask )
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

    public void validate() throws Exception
    {
        super.validate();

        if (( this.exportedAddressList == null ) || ( this.exportedAddressList.size()  == 0 )) {
            throw new Exception( "A site must have at least one exported address" );
        }
    }
}
