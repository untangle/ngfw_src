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

#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>

#include <stdlib.h>
#include <string.h>

#include <mvutil/errlog.h>
#include <mvutil/debug.h>

#include <libnetcap.h>

int  netcap_endpoints_copy          ( netcap_endpoints_t* dst, netcap_endpoints_t* src )
{
    if ( src == NULL || dst == NULL ) return errlogargs();

    memcpy ( dst, src, sizeof( netcap_endpoints_t));
    return 0;
}

int  netcap_endpoints_bzero         ( netcap_endpoints_t* endpoints )
{
    if ( endpoints == NULL ) return errlogargs();

    bzero ( endpoints, sizeof( netcap_endpoints_t ));
    return 0;
}
