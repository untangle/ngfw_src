/**
 * $Id$
 */
package com.untangle.node.openvpn;

import java.util.List;

class AlpacaRulesWriter extends ScriptWriter
{
    private static final String RULES_HEADER =
        COMMENT + UNTANGLE_HEADER + "\n" +
        COMMENT + " VPN Access Packet Filter Rules\n\n";

    AlpacaRulesWriter()
    {
        super();
    }

    void appendExportedAddresses( List<SiteNetwork> exportedAddressList )
    {
        for ( SiteNetwork n : exportedAddressList ) {
            appendLine( "insert_vpn_export " + n.getNetwork() + " " + n.getNetmask());
        }
    }

    protected String header()
    {
        return RULES_HEADER;
    }    
}
