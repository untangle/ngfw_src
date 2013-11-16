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

#include <stdlib.h>

#include <mvutil/errlog.h>
#include <mvutil/debug.h>

#include "libnetcap.h"
#include "netcap_icmp_msg.h"


netcap_icmp_msg_t* netcap_icmp_msg_malloc ( int data_len )
{
    netcap_icmp_msg_t* msg;
    
    if ( data_len < 0 ) 
        return errlogargs_null();

    if (( msg = malloc( data_len + NETCAP_ICMP_MSG_BASE_SIZE )) == NULL ) {
        return errlogmalloc_null();
    }
    
    return msg;
}

int                netcap_icmp_msg_init   ( netcap_icmp_msg_t* msg, int msg_size, u_char* data, int data_len )
{
    if ( msg == NULL || data == NULL || data_len < 0 )
        return errlogargs();
    
    if ( msg_size < ( data_len + sizeof( int )))
        return errlog( ERR_CRITICAL, "Message must be large enough to hold data and 4 size\n" );
    
    msg->type = NETCAP_ICMP_MSG_TYPE;
    
    msg->data_len = data_len;
    memcpy( &msg->data, data, data_len );
    
    return 0;
}

netcap_icmp_msg_t* netcap_icmp_msg_create ( u_char* data, u_int data_len )
{
    netcap_icmp_msg_t* msg;

    if ( data == NULL || data_len < 0 )
        return errlogargs_null();
    
    if (( msg = netcap_icmp_msg_malloc( data_len )) == NULL ) {
        return errlog_null( ERR_CRITICAL, "netcap_icmp_msg_malloc\n" );
    }
    
    if ( netcap_icmp_msg_init( msg, data_len + sizeof( int ), data, data_len ) < 0 ) {
        netcap_icmp_msg_raze( msg );
        return errlog_null( ERR_CRITICAL, "netcap_icmp_msg_init\n" );
    }
    
    return msg;
}

int                netcap_icmp_msg_free    ( netcap_icmp_msg_t* msg )
{
    if ( msg == NULL )
        return errlogargs();

    free( msg );
    
    return 0;
}

int                netcap_icmp_msg_destroy ( netcap_icmp_msg_t* msg )
{
    if ( msg == NULL )
        return errlogargs();

    /* Make sure that it is the proper type */
    if ( msg->type != NETCAP_ICMP_MSG_TYPE )
        return errlog( ERR_CRITICAL, "Invalid message type\n" );

    return 0;
}

int                netcap_icmp_msg_raze    ( netcap_icmp_msg_t* msg )
{
    if ( msg == NULL )
        return errlogargs();
    
    if ( netcap_icmp_msg_destroy( msg ) < 0 )
        errlog( ERR_CRITICAL, "netcap_icmp_msg_destroy\n" );
    
    if ( netcap_icmp_msg_free( msg ) < 0 )
        errlog( ERR_CRITICAL, "netcap_icmp_msg_free\n" );
    
    return 0;
}
