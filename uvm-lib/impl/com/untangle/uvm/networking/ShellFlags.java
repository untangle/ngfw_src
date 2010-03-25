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

import static com.untangle.uvm.networking.NetworkManagerImpl.UVM_CONF;

class ShellFlags
{
    static final String FILE_RULE_CFG     = UVM_CONF + "/networking.sh";
    static final String FILE_PROPERTIES   = UVM_CONF + "/uvm.networking.properties";
    static final String FLAG_TCP_WIN      = "TCP_WINDOW_SCALING_EN";
    static final String FLAG_HTTP_IN      = "UVM_ALLOW_IN_HTTP";
    static final String FLAG_BLOCK_PAGE_PORT =  "UVM_BLOCK_PAGE_PORT";
    static final String FLAG_HTTPS_OUT    = "UVM_ALLOW_OUT_HTTPS";
    static final String FLAG_HTTPS_RES    = "UVM_ALLOW_OUT_RES";
    static final String FLAG_OUT_NET      = "UVM_ALLOW_OUT_NET";
    static final String FLAG_OUT_MASK     = "UVM_ALLOW_OUT_MASK";
    static final String FLAG_EXCEPTION    = "UVM_IS_EXCEPTION_REPORTING_EN";

    static final String FLAG_PUBLIC_URL = "UVM_PUBLIC_URL";

    static final String FLAG_EXTERNAL_HTTPS_PORT  = "HTTPS_EXTERNAL_PORT";

    static final String FLAG_EXTERNAL_ROUTER_ADDR = "HTTPS_PUBLIC_ADDR";
    static final String FLAG_EXTERNAL_ROUTER_PORT = "HTTPS_PUBLIC_PORT";
    static final String FLAG_EXTERNAL_ROUTER_EN   = "HTTPS_PUBLIC_REDIRECT_EN";

    static final String FLAG_POST_FUNC    = "UVM_POST_CONF";
    static final String POST_FUNC_NAME    = "postConfigurationScript";
    static final String DECL_POST_CONF    = POST_FUNC_NAME + "() {";


    static final String FLAG_CUSTOM_RULES = "UVM_CUSTOM_RULES";
    static final String CUSTOM_RULES_NAME = "customRulesScript";
    static final String DECL_CUSTOM_RULES = CUSTOM_RULES_NAME + "() {";

    static final String FLAG_IS_HOSTNAME_PUBLIC = "UVM_IS_HOSTNAME_EN";
    static final String FLAG_HOSTNAME          = "UVM_HOSTNAME";

    /* The following two flags are no longer used, they are here for legacy purposes */
    static final String FLAG_PUBLIC_ADDRESS_EN = "UVM_PUBLIC_ADDRESS_EN";
    static final String FLAG_PUBLIC_ADDRESS    = "UVM_PUBLIC_ADDRESS";

    static final String PROPERTY_HTTPS_PORT = "uvm.https.port";
}
