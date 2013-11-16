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

/* $Id$ */
#include <stdlib.h>
#include <stdio.h>
#include <libnetcap.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <mvutil/debug.h>

char* DEV_INSIDE="eth1";
char* DEV_OUTSIDE="eth0";


int main(int argc, char *argv[])
{
    int ret;
    netcap_pkt_t *prop;
    
    /**
     * init
     */
    printf("%s\n\n",netcap_version()); 
    netcap_init();
    debug_set_level(NETCAP_DEBUG_PKG,10);
    prop = netcap_pkt_create();
    if (!prop) {
	    exit(-1);
    }
#define DATA "hello, world"
    prop->dst_addr.s_addr = argc < 2 ? htonl(0x0a000020) : htonl(0x0a0000ff);
    prop->dst_port = 2048;
    if (argc < 2) {
        prop->outdev[0] = 0;
    } else {
        strcpy(prop->outdev, argv[1]);
    }
    ret = netcap_udp_send(DATA, strlen(DATA), prop);
    netcap_pkt_free(prop);
    return 0;
}







