/**
 * $Id$
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
