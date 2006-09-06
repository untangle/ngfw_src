/*
 * Copyright (c) 2003, 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.networking;

import com.metavize.mvvm.networking.internal.NetworkSpacesInternalSettings;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.script.ScriptWriter;
import org.apache.log4j.Logger;

import static com.metavize.mvvm.tran.script.ScriptWriter.COMMENT;
import static com.metavize.mvvm.tran.script.ScriptWriter.METAVIZE_HEADER;

class ResolvScriptWriter extends ScriptWriter
{
    private final Logger logger = Logger.getLogger(getClass());

    static final String NS_PARAM = "nameserver";

    private static final String RESOLV_HEADER =
        COMMENT + METAVIZE_HEADER +
        COMMENT + " name resolution settings\n";

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
}
