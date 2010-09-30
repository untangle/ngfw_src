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


public class Constants
{
    static final String UVM_BASE = System.getProperty( "uvm.home" );
    static final String UVM_CONF = System.getProperty( "uvm.conf.dir" );

    static final String SCRIPT_DIR     = UVM_BASE + "/openvpn";
    static final String DATA_DIR       = SCRIPT_DIR;

    /* Base configuration directory, all of the files should go into
     * one of the sub directories */
    static final String CONF_DIR = UVM_CONF + "/openvpn";

    /* The directory for all of the client packages, needed by the servlet */
    public static final String PACKAGES_DIR = CONF_DIR + "/client-packages";

    /* This is the variable used to define which client to download */
    public static final String ADMIN_DOWNLOAD_CLIENT_PARAM = "client";
    public static final String ADMIN_DOWNLOAD_CLIENT_KEY = "adminKey";

    public static final String ADMIN_UPLOAD_CLIENT_PARAM = "siteConfiguration";

    /* The PKI infrastructure */
    static final String PKI_DIR = CONF_DIR + "/pki";

    /* Miscellaneous configuration files */
    static final String MISC_DIR = CONF_DIR + "/misc";

    /* Error codes from scripts */

    /* Unable to start the openvpn server, may just be the other end is
     * not started */
    static final int START_ERROR         = 251;

    /**
     * The loaded file is not a valid OpenVPN client configuration
     */
    static final int INVALID_FILE_ERROR  = 247;
}
