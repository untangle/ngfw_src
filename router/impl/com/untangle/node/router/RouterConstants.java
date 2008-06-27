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

package com.untangle.node.router;


class RouterConstants
{
    /* TCP Port range for nat */
    static final int TCP_NAT_PORT_START = 10000;
    static final int TCP_NAT_PORT_END   = 60000;

    /* UDP Port range for nat */
    static final int UDP_NAT_PORT_START = 10000;
    static final int UDP_NAT_PORT_END   = 60000;

    /* ICMP PID range for nat */
    static final int ICMP_PID_START     = 1;
    static final int ICMP_PID_END       = 60000;

    /* Port the server receives data on, probably not the best place for this constant */
    static final int FTP_SERVER_PORT    = 21;
}
