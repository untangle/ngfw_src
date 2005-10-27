/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.openvpn;

class Constants
{
    static final String BUNNICULA_BASE  = System.getProperty( "bunnicula.home" );
    static final String BUNNICULA_CONF  = System.getProperty( "bunnicula.conf.dir" );
    
    static final String VPN_SCRIPT_BASE = BUNNICULA_BASE + "/openvpn";
    static final String VPN_CONF_BASE   = BUNNICULA_CONF;
}
