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

package com.untangle.uvm.networking;

import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.script.ScriptWriter;

class ResolvScriptWriter extends ScriptWriter
{
    private static final IPaddr LOCALHOST;

    private final Logger logger = Logger.getLogger(getClass());

    static final String NS_PARAM = "nameserver";

    private static final String RESOLV_HEADER =
        COMMENT + UNTANGLE_HEADER +
        COMMENT + " name resolution settings.\n";

    private final NetworkSpacesInternalSettings settings;

    ResolvScriptWriter( NetworkSpacesInternalSettings settings )
    {
        super();
        this.settings = settings;
    }

    void addNetworkSettings()
    {
        // ???? Just always write the file, just in case
        // if ( this.settings.isDhcpEnabled()) return;
        
        /* insert localhost */
        addDns( LOCALHOST );
        addDns( settings.getDns1());
        addDns( settings.getDns2());
    }

    private void addDns( IPaddr dns )
    {
        if ( dns == null || dns.isEmpty()) return;

        appendVariable( NS_PARAM, dns.toString());
    }

    /**
     * In the /etc/resolv.conf file a variable is just separated by a space
     */
    @Override
    public void appendVariable( String variable, String value )
    {
        if ( variable == null || value == null ) {
            logger.warn( "Variable["  + variable + "] or value["+ value + "] is null" );
            return;
        }

        variable = variable.trim();
        value    = value.trim();

        appendLine( variable + " " + value );
    }

    @Override
    protected String header()
    {
        return RESOLV_HEADER;
    }

    static 
    {
        IPaddr addr = null;
        try {
            addr = IPaddr.parse( "127.0.0.1" );
        } catch ( ParseException e ) {
            System.out.println( "unable to parse localhost, not binding to local dns server" );
            addr = null;
        } catch ( UnknownHostException e ) {
            System.out.println( "unable to parse localhost, not binding to local dns server" );
            addr = null;
        }

        LOCALHOST = addr;

    }
}
