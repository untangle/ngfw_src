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
package com.untangle.mvvm.networking;

import static com.untangle.mvvm.networking.NetworkManagerImpl.BUNNICULA_CONF;

class ShellFlags
{
    static final String FILE_RULE_CFG     = BUNNICULA_CONF + "/networking.sh";
    static final String FILE_PROPERTIES   = BUNNICULA_CONF + "/mvvm.networking.properties";
    static final String FLAG_TCP_WIN      = "TCP_WINDOW_SCALING_EN";
    static final String FLAG_HTTP_IN      = "MVVM_ALLOW_IN_HTTP";
    static final String FLAG_HTTPS_OUT    = "MVVM_ALLOW_OUT_HTTPS";
    static final String FLAG_HTTPS_RES    = "MVVM_ALLOW_OUT_RES";
    static final String FLAG_OUT_NET      = "MVVM_ALLOW_OUT_NET";
    static final String FLAG_OUT_MASK     = "MVVM_ALLOW_OUT_MASK";
    static final String FLAG_EXCEPTION    = "MVVM_IS_EXCEPTION_REPORTING_EN";


    static final String FLAG_EXTERNAL_ROUTER_ADDR = "HTTPS_PUBLIC_ADDR";
    static final String FLAG_EXTERNAL_ROUTER_PORT = "HTTPS_PUBLIC_PORT";
    static final String FLAG_EXTERNAL_ROUTER_EN   = "HTTPS_PUBLIC_REDIRECT_EN";
    
    static final String FLAG_POST_FUNC    = "MVVM_POST_CONF";
    static final String POST_FUNC_NAME    = "postConfigurationScript";
    static final String DECL_POST_CONF    = "function " + POST_FUNC_NAME + "() {";

    
    static final String FLAG_CUSTOM_RULES = "MVVM_CUSTOM_RULES";
    static final String CUSTOM_RULES_NAME = "customRulesScript";
    static final String DECL_CUSTOM_RULES = "function " + CUSTOM_RULES_NAME + "() {";

    static final String FLAG_IS_HOSTNAME_PUBLIC = "MVVM_IS_HOSTNAME_EN";
    static final String FLAG_HOSTNAME          = "MVVM_HOSTNAME";
    static final String FLAG_PUBLIC_ADDRESS_EN = "MVVM_PUBLIC_ADDRESS_EN";
    static final String FLAG_PUBLIC_ADDRESS    = "MVVM_PUBLIC_ADDRESS";

    static final String PROPERTY_HTTPS_PORT = "mvvm.https.port";
}