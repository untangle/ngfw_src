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

import org.apache.log4j.Logger;

import com.untangle.uvm.node.ScriptWriter;


class VpnScriptWriter extends ScriptWriter
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String OPENVPN_HEADER =
        COMMENT + UNTANGLE_HEADER + "\n" +
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
