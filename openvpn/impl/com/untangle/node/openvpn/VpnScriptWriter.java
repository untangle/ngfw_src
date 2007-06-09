/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.node.openvpn;

import com.untangle.uvm.node.script.ScriptWriter;
import org.apache.log4j.Logger;

import static com.untangle.uvm.node.script.ScriptWriter.COMMENT;
import static com.untangle.uvm.node.script.ScriptWriter.METAVIZE_HEADER;


class VpnScriptWriter extends ScriptWriter
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String OPENVPN_HEADER =
        COMMENT + METAVIZE_HEADER + "\n" +
        COMMENT + " OpenVPN(v2.0) configuration script\n\n";

    VpnScriptWriter()
    {
        super();
    }

    @Override
    public void appendVariable( String variable, String value )
    {
        appendVariable( variable, value, false );
    }

    /*
     * Designed to write a variable name pair (there are no equal signs in open vpn)
     * isGlobal is not used
     * @Overrides
     */
    @Override
    public void appendVariable( String variable, String value, boolean isGlobal )
    {
        if (( variable == null ) || ( value == null )) {
            logger.warn( "NULL variable[" + variable +"] or value[" + variable + "], ignoring" );
            return;
        }

        variable = variable.trim();
        value    = value.trim();

        if ( variable.length() == 0 ) {
            /* This is a jenky way to get a stack trace */
            logger.warn( "Empty variable name, ignoring", new Exception());
            return;
        }

        appendLine( variable + " " + value );
    }

    protected String header()
    {
        return OPENVPN_HEADER;
    }
}
