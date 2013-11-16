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
#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include "netcap_server.h"

/*! \brief donates a thread to the pool
 *  you must donate atleast one thread for netcap to start \n
 *  the format is meant to be used with pthread_create    \n
 *  or you can just call it from a thread or process      \n 
 *  arg is meaningless and is ignored                     \n
 * 
 *  \param arg is ignored 
 *  \return int thread_id can be used to stop or reclaim thread
 */
void* netcap_thread_donate (void* arg)
{
    debug(4,"NETCAP: Donating Thread\n");
    netcap_server();
    return NULL;
}

/*! \brief removes a thread from the pool
 *  \warning not implemented
 *  \return 0 upon success, -1 otherwise
 */
int netcap_thread_undonate (int thread_id)
{
    return errlog(ERR_CRITICAL,"Unimplemented\n");
}
