/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
#ifndef __NETCAP_RDR_H_
#define __NETCAP_RDR_H_

#include <sys/types.h>
#include <libnetcap.h>
#include <mvutil/list.h>

#include "netcap_globals.h"
#include "netcap_traffic.h"
#include "netcap_rdr.h"

/*! \brief the redirect rule
 *  this represent a redirect rule in the firewall \n
 *  this will be different from system to system \n
 */
typedef struct rdr {
    /**
     * An array of filedescriptors for diverts (only 1) and redirects(TCP)
     * This array is copied on initialization and freed and all filedescriptors
     * are closed in destroy.
     * This should be of size port_max-port_min + 1, or NULL if unused.
     */
    int* socks;

    /**
     * Local port_min for redirects.  For UDP diverts, this is the 
     * only port that is used.
     */
    int port_min;

    /**
     * Local port_max for redirects.  For UDP this is unused.
     */
    int port_max;
    
    /**
     * flags for the redirection (defined in libnetcap.h)
     */
    int flags;

    
    /*! \brief the iptables rm cmd
     *  this command is a system command to remove the redirect 
     */
    int   iptables_count;
    char* iptables_remove_cmd[MAX_CMD_IPTABLES_PER_RDR];
    char* iptables_insert_cmd[MAX_CMD_IPTABLES_PER_RDR];
} rdr_t;


rdr_t* rdr_malloc (void);
int    rdr_init (rdr_t* rdr, netcap_traffic_t* traffic, int flags, u_short port, int* socks, int sock_count );

rdr_t* rdr_create (netcap_traffic_t* traffic, int flags, u_short port, int* socks, int sock_count );

int    rdr_free (rdr_t* rdr);
int    rdr_destroy (rdr_t* rdr);
int    rdr_raze (rdr_t* rdr);

int    rdr_insert (rdr_t* rdr);
int    rdr_remove (rdr_t* rdr);

#endif
