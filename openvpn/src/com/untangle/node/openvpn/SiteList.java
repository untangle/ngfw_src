/**
 * $Id$
 */
package com.untangle.node.openvpn;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@SuppressWarnings("serial")
public class SiteList implements Serializable
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

    List<AddressRange> buildAddressRange()
    {
        List<AddressRange> checkList = new LinkedList<AddressRange>();

        for ( VpnSite site : this.siteList ) {
            for ( SiteNetwork siteNetwork : site.getExportedAddressList()) {
                checkList.add( AddressRange.makeNetwork( siteNetwork.getNetwork(), siteNetwork.getNetmask()) );
            }
        }

        return checkList;
    }
}
