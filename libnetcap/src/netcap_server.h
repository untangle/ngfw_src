/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: netcap_server.h,v 1.1 2004/11/09 19:40:00 dmorris Exp $
 */
#ifndef __NETCAP_SERVER_H
#define __NETCAP_SERVER_H

#include <sys/types.h>

typedef enum _netcap_mesg {
    NETCAP_MSG_REFRESH,
    NETCAP_MSG_ADD_SUB,
    NETCAP_MSG_REM_SUB,
    NETCAP_MSG_SHUTDOWN,
    NETCAP_MSG_NULL /* not used for anything */
} netcap_mesg_t;


int  netcap_server_init (void);
int  netcap_server_shutdown (void);
int  netcap_server (void);
int  netcap_server_sndmsg (netcap_mesg_t mesg, void* arg);


    
#endif
